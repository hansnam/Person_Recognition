package com.example.coreservice.service;

import com.example.coreservice.client.MlServiceClient;
import com.example.coreservice.dto.*;
import com.example.coreservice.entity.LogPhatHien;
import com.example.coreservice.entity.NguoiMatTich;
import com.example.coreservice.repository.LogPhatHienRepository;
import com.example.coreservice.repository.NguoiMatTichRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class DetectionService {

    private final MlServiceClient mlServiceClient;
    private final NguoiMatTichRepository nguoiMatTichRepository;
    private final LogPhatHienRepository logPhatHienRepository;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;

    @Value("${ml-service.similarity-threshold:0.45}")
    private double defaultThreshold;

    public DetectionService(MlServiceClient mlServiceClient,
                            NguoiMatTichRepository nguoiMatTichRepository,
                            LogPhatHienRepository logPhatHienRepository,
                            FileStorageService fileStorageService,
                            EmailService emailService) {
        this.mlServiceClient = mlServiceClient;
        this.nguoiMatTichRepository = nguoiMatTichRepository;
        this.logPhatHienRepository = logPhatHienRepository;
        this.fileStorageService = fileStorageService;
        this.emailService = emailService;
    }

    /**
     * Nhận diện và so khớp khuôn mặt từ ảnh/khung hình camera:
     * 1. Lưu ảnh chụp thời điểm phát hiện
     * 2. Gọi ML Service phát hiện & tìm kiếm FAISS
     * 3. Nếu trùng khớp -> Tra cứu hồ sơ MySQL -> Lưu bản ghi vào log_phat_hien
     */
    @Transactional
    public DetectionResponse detectAndMatch(MultipartFile file, Double threshold) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Ảnh đầu vào không được để trống.");
        }

        double activeThreshold = threshold != null ? threshold : defaultThreshold;

        // 1. Lưu ảnh chụp từ camera/upload
        String capturedImageUrl = fileStorageService.storeFile(file);

        // 2. Gọi ML Service so khớp
        MlMatchResponse mlResult = mlServiceClient.detectAndMatch(file, activeThreshold);

        DetectionResponse response = new DetectionResponse();
        response.setMatched(false);
        response.setSimilarity(mlResult.getSimilarity());
        response.setBbox(mlResult.getBbox());
        response.setAnhChupUrl(capturedImageUrl);

        // Chuyển đổi danh sách chi tiết các khuôn mặt phát hiện được
        List<FaceDetectionDto> detections = new ArrayList<>();
        if (mlResult.getAllDetections() != null && !mlResult.getAllDetections().isEmpty()) {
            for (MlDetectionItem item : mlResult.getAllDetections()) {
                FaceDetectionDto dto = new FaceDetectionDto();
                dto.setBbox(item.getBbox());
                dto.setMatched(item.getMatched());
                dto.setSimilarity(item.getSimilarity());

                if (Boolean.TRUE.equals(item.getMatched()) && item.getVectorId() != null) {
                    Optional<NguoiMatTich> matchOpt = nguoiMatTichRepository.findByVectorIdFaiss(item.getVectorId());
                    matchOpt.ifPresent(nguoiMatTich -> dto.setHoSo(NguoiMatTichResponse.fromEntity(nguoiMatTich)));
                }
                detections.add(dto);
            }
        }
        response.setDetections(detections);

        if (Boolean.TRUE.equals(mlResult.getMatched()) && mlResult.getVectorId() != null) {
            // 3. Tra cứu ngược thông tin hồ sơ từ vector_id_faiss
            Optional<NguoiMatTich> nguoiOpt = nguoiMatTichRepository.findByVectorIdFaiss(mlResult.getVectorId());
            if (nguoiOpt.isPresent()) {
                NguoiMatTich nguoi = nguoiOpt.get();

                // 4. Ghi log phát hiện vào CSDL MySQL
                LogPhatHien log = new LogPhatHien();
                log.setNguoiMatTich(nguoi);
                log.setThoiGian(LocalDateTime.now());
                log.setDoTinCay(mlResult.getSimilarity());
                log.setAnhChupUrl(capturedImageUrl);

                LogPhatHien savedLog = logPhatHienRepository.save(log);

                // 5. Gửi email cảnh báo khẩn cấp tới người thân (chạy bất đồng bộ)
                emailService.sendMissingPersonAlert(
                        nguoi.getLienHeNguoiThan(),
                        nguoi.getHoTen(),
                        mlResult.getSimilarity(),
                        savedLog.getThoiGian(),
                        capturedImageUrl
                );

                response.setMatched(true);
                response.setHoSo(NguoiMatTichResponse.fromEntity(nguoi));
                response.setLogId(savedLog.getId());
                response.setMessage("PHÁT HIỆN TRÙNG KHỚP: " + nguoi.getHoTen() +
                        " (Độ tin cậy: " + String.format("%.2f%%", mlResult.getSimilarity() * 100) + ")");
                return response;
            } else {
                response.setMessage("Tìm thấy vector trong FAISS nhưng không khớp với hồ sơ MySQL nào (id=" + mlResult.getVectorId() + ").");
                return response;
            }
        }

        // Trường hợp không khớp (open-set)
        if (mlResult.getTotalFacesDetected() != null && mlResult.getTotalFacesDetected() > 0) {
            response.setMessage("Phát hiện " + mlResult.getTotalFacesDetected() +
                    " khuôn mặt nhưng không khớp với danh sách người mất tích nào (Độ tương đồng cao nhất: " +
                    String.format("%.2f%%", mlResult.getSimilarity() * 100) + " < " +
                    String.format("%.2f%%", activeThreshold * 100) + ").");
        } else {
            response.setMessage(mlResult.getMessage() != null ? mlResult.getMessage() : "Không phát hiện khuôn mặt nào trong ảnh.");
        }

        return response;
    }

    /**
     * Nhận diện và so khớp khuôn mặt từ file video tải lên:
     * 1. Lưu video vào ./uploads/ để hỗ trợ phát trực tiếp trên web
     * 2. Gọi ML Service phân tích video theo từng giây/khung hình
     * 3. Với mỗi người mất tích phát hiện được:
     *    - Lưu ảnh snapshot khung hình có độ tương đồng cao nhất
     *    - Ghi log vào log_phat_hien (1 log/người để tránh spam)
     *    - Gửi email cảnh báo khẩn cấp cho gia đình (1 email/người)
     * 4. Trả về kết quả videoUrl, timeline mốc thời gian và tóm tắt
     */
    @Transactional
    public VideoDetectionResponse detectAndMatchVideo(MultipartFile file, Double threshold, Double frameInterval) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Tệp video không được để trống.");
        }

        double activeThreshold = threshold != null ? threshold : defaultThreshold;
        double activeInterval = frameInterval != null ? frameInterval : 1.0;

        // 1. Lưu video vật lý vào uploads
        String storedVideoUrl = fileStorageService.storeFile(file);

        // 2. Gửi sang ML Service phân tích video
        MlVideoMatchResponse mlResult = mlServiceClient.detectAndMatchVideo(file, activeThreshold, activeInterval);

        VideoDetectionResponse response = new VideoDetectionResponse();
        response.setVideoUrl(storedVideoUrl);
        response.setDurationSeconds(mlResult.getDurationSeconds());
        response.setTotalFrames(mlResult.getTotalFrames());
        response.setProcessedFrames(mlResult.getProcessedFrames());
        response.setTotalFacesDetected(mlResult.getTotalFacesDetected());
        response.setMatched(Boolean.TRUE.equals(mlResult.getMatched()));
        response.setTotalMatches(mlResult.getTotalMatches());

        // Cache tra cứu người mất tích theo vector_id để tối ưu truy vấn MySQL
        Map<Long, NguoiMatTich> personCache = new HashMap<>();

        // 3. Xử lý danh sách tóm tắt người mất tích (unique_persons)
        List<VideoPersonSummaryDto> summaryList = new ArrayList<>();
        if (mlResult.getUniquePersons() != null) {
            for (MlVideoPersonSummary pSum : mlResult.getUniquePersons()) {
                VideoPersonSummaryDto sDto = new VideoPersonSummaryDto();
                sDto.setMaxSimilarity(pSum.getMaxSimilarity());
                sDto.setOccurrencesCount(pSum.getOccurrencesCount());
                sDto.setFirstSeen(pSum.getFirstSeen());
                sDto.setFirstSeenSeconds(pSum.getFirstSeenSeconds());
                sDto.setLastSeen(pSum.getLastSeen());
                sDto.setLastSeenSeconds(pSum.getLastSeenSeconds());
                sDto.setTimestamps(pSum.getTimestamps());

                // Lưu snapshot khung hình tốt nhất
                String bestSnapshotUrl = fileStorageService.storeBase64Image(pSum.getBestSnapshotBase64(), "vid_best_");
                sDto.setBestSnapshotUrl(bestSnapshotUrl);

                // Tra cứu MySQL
                Optional<NguoiMatTich> nmtOpt = nguoiMatTichRepository.findByVectorIdFaiss(pSum.getVectorId());
                if (nmtOpt.isPresent()) {
                    NguoiMatTich nmt = nmtOpt.get();
                    personCache.put(pSum.getVectorId(), nmt);
                    sDto.setPerson(NguoiMatTichResponse.fromEntity(nmt));

                    // Lưu log_phat_hien cho người này
                    LogPhatHien log = new LogPhatHien();
                    log.setNguoiMatTich(nmt);
                    log.setThoiGian(LocalDateTime.now());
                    log.setDoTinCay(pSum.getMaxSimilarity());
                    log.setAnhChupUrl(bestSnapshotUrl);
                    LogPhatHien savedLog = logPhatHienRepository.save(log);
                    sDto.setLogId(savedLog.getId());

                    // Gửi email cảnh báo cho gia đình (Async)
                    emailService.sendMissingPersonAlert(
                            nmt.getLienHeNguoiThan(),
                            nmt.getHoTen(),
                            pSum.getMaxSimilarity(),
                            savedLog.getThoiGian(),
                            bestSnapshotUrl
                    );
                }
                summaryList.add(sDto);
            }
        }
        response.setUniquePersons(summaryList);

        // 4. Xử lý danh sách timeline chi tiết từng mốc phát hiện
        List<VideoTimelineItemDto> timelineList = new ArrayList<>();
        if (mlResult.getTimeline() != null) {
            for (MlVideoTimelineItem item : mlResult.getTimeline()) {
                VideoTimelineItemDto tDto = new VideoTimelineItemDto();
                tDto.setTimestamp(item.getTimestamp());
                tDto.setTimestampFormatted(item.getTimestampFormatted());
                tDto.setFrameIndex(item.getFrameIndex());
                tDto.setSimilarity(item.getSimilarity());
                tDto.setBbox(item.getBbox());

                // Lưu ảnh snapshot cho mốc sự kiện
                String snapUrl = fileStorageService.storeBase64Image(item.getSnapshotBase64(), "vid_event_");
                tDto.setSnapshotUrl(snapUrl);

                NguoiMatTich nmt = personCache.computeIfAbsent(item.getVectorId(),
                        vid -> nguoiMatTichRepository.findByVectorIdFaiss(vid).orElse(null));
                if (nmt != null) {
                    tDto.setPerson(NguoiMatTichResponse.fromEntity(nmt));
                }

                timelineList.add(tDto);
            }
        }
        response.setTimeline(timelineList);

        // 5. Thông điệp phản hồi
        if (Boolean.TRUE.equals(response.getMatched())) {
            response.setMessage("PHÁT HIỆN " + summaryList.size() + " NGƯỜI THÂN TRONG VIDEO (Tổng " +
                    response.getTotalMatches() + " lượt xuất hiện trên " + response.getProcessedFrames() + " khung hình được quét).");
        } else if (response.getTotalFacesDetected() != null && response.getTotalFacesDetected() > 0) {
            response.setMessage("Đã quét video (" + response.getProcessedFrames() + " khung hình), phát hiện " +
                    response.getTotalFacesDetected() + " khuôn mặt nhưng không khớp với danh sách người mất tích.");
        } else {
            response.setMessage("Đã quét xong video nhưng không phát hiện khuôn mặt nào trong các khung hình.");
        }

        return response;
    }

    /**
     * Nhận diện và so khớp Fusion (Face + Body Re-ID) từ ảnh/khung hình camera:
     * 1. Lưu ảnh chụp thời điểm phát hiện
     * 2. Gọi ML Service detect-and-match-fusion
     * 3. Với mỗi detection item:
     *    - Map face, body, fusion status
     *    - Tra cứu hồ sơ NguoiMatTich theo fusion.personId (hoặc face/body)
     *    - Gắn hoSo vào detection
     *    - Nếu trạng thái là CONFIRMED, FACE_MATCH_BODY_MISMATCH hoặc FACE_CANDIDATE:
     *      -> Lưu log_phat_hien với face_similarity, body_similarity, fusion_status, body_warning
     *      -> Nếu CONFIRMED hoặc FACE_MATCH_BODY_MISMATCH -> gửi email cảnh báo (Async)
     * 4. Trả về FusionDetectionResponse
     */
    @Transactional
    public FusionDetectionResponse detectAndMatchFusion(MultipartFile file, Double faceThreshold, Double bodyThreshold) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Ảnh đầu vào không được để trống.");
        }

        double activeFaceThresh = faceThreshold != null ? faceThreshold : defaultThreshold;
        double activeBodyThresh = bodyThreshold != null ? bodyThreshold : 0.65;

        // 1. Chỉ lưu ảnh khi có người trùng khớp (tối ưu hóa Disk I/O cho live camera streaming)
        String capturedImageUrl = null;

        // 2. Gọi ML Service so khớp Fusion
        MlFusionResponse mlResult = mlServiceClient.detectAndMatchFusion(file, activeFaceThresh, activeBodyThresh);

        FusionDetectionResponse response = new FusionDetectionResponse();
        response.setMatched(Boolean.TRUE.equals(mlResult.getMatched()));

        List<FusionDetectionItemDto> dtoList = new ArrayList<>();
        int matchedCount = 0;
        String firstMatchedName = null;

        if (mlResult.getDetections() != null) {
            for (MlFusionDetectionItem item : mlResult.getDetections()) {
                FusionDetectionItemDto dto = new FusionDetectionItemDto();
                dto.setDetectionId(item.getDetectionId());
                dto.setBbox(item.getBbox());

                if (item.getFace() != null) {
                    dto.setFace(new FusionComponentResultDto(
                            item.getFace().getMatched(),
                            item.getFace().getPersonId(),
                            item.getFace().getSimilarity(),
                            item.getFace().getBbox()
                    ));
                }

                if (item.getBody() != null) {
                    dto.setBody(new FusionComponentResultDto(
                            item.getBody().getMatched(),
                            item.getBody().getPersonId(),
                            item.getBody().getSimilarity(),
                            item.getBody().getBbox()
                    ));
                }

                String status = "UNKNOWN";
                Long fusionPersonId = null;
                Boolean bodyWarning = false;
                String fusionMsg = null;

                if (item.getFusion() != null) {
                    status = item.getFusion().getStatus();
                    fusionPersonId = item.getFusion().getPersonId();
                    bodyWarning = Boolean.TRUE.equals(item.getFusion().getBodyWarning());
                    fusionMsg = item.getFusion().getMessage();

                    dto.setFusion(new FusionStatusDto(
                            status,
                            fusionPersonId,
                            bodyWarning,
                            fusionMsg
                    ));
                }

                // Tra cứu hồ sơ người mất tích
                NguoiMatTich matchedPerson = null;
                if (fusionPersonId != null) {
                    Optional<NguoiMatTich> nmtOpt = nguoiMatTichRepository.findByVectorIdFaiss(fusionPersonId);
                    if (nmtOpt.isPresent()) {
                        matchedPerson = nmtOpt.get();
                        dto.setHoSo(NguoiMatTichResponse.fromEntity(matchedPerson));
                    }
                }

                // Ghi log & gửi email khi nhận dạng thành công (Face) hoặc nghi vấn trang phục (Body)
                boolean isFaceMatch = "CONFIRMED".equals(status)
                        || "FACE_MATCH_BODY_MISMATCH".equals(status)
                        || "FACE_CANDIDATE".equals(status);
                boolean isBodyCandidate = "BODY_CANDIDATE".equals(status);

                if ((isFaceMatch || isBodyCandidate) && matchedPerson != null) {
                    matchedCount++;
                    if (firstMatchedName == null) {
                        firstMatchedName = matchedPerson.getHoTen();
                    }

                    // Lưu file ảnh chụp vào thư mục uploads khi có người thân trùng khớp hoặc nghi vấn trang phục
                    if (capturedImageUrl == null) {
                        capturedImageUrl = fileStorageService.storeFile(file);
                    }

                    LogPhatHien log = new LogPhatHien();
                    log.setNguoiMatTich(matchedPerson);
                    log.setThoiGian(LocalDateTime.now());

                    Float faceSim = item.getFace() != null ? item.getFace().getSimilarity() : null;
                    Float bodySim = item.getBody() != null ? item.getBody().getSimilarity() : null;

                    log.setFaceSimilarity(faceSim);
                    log.setBodySimilarity(bodySim);
                    log.setFusionStatus(status);
                    log.setBodyWarning(bodyWarning);
                    log.setDoTinCay(faceSim != null ? faceSim : (bodySim != null ? bodySim : 0f));
                    log.setAnhChupUrl(capturedImageUrl);

                    LogPhatHien savedLog = logPhatHienRepository.save(log);

                    // Kích hoạt alert email khi CONFIRMED hoặc FACE_MATCH_BODY_MISMATCH (không gửi spam khi chỉ mới nghi vấn trang phục BODY_CANDIDATE)
                    if ("CONFIRMED".equals(status) || "FACE_MATCH_BODY_MISMATCH".equals(status)) {
                        emailService.sendMissingPersonAlert(
                                matchedPerson.getLienHeNguoiThan(),
                                matchedPerson.getHoTen(),
                                faceSim != null ? faceSim : 0f,
                                savedLog.getThoiGian(),
                                capturedImageUrl
                        );
                    }
                }

                dtoList.add(dto);
            }
        }

        response.setDetections(dtoList);
        response.setAnhChupUrl(capturedImageUrl);

        if (Boolean.TRUE.equals(response.getMatched())) {
            response.setMessage("PHÁT HIỆN TRÙNG KHỚP: " +
                    (firstMatchedName != null ? firstMatchedName : "") +
                    (matchedCount > 1 ? " (+ " + (matchedCount - 1) + " người khác)" : ""));
        } else if (mlResult.getTotalPersons() != null && mlResult.getTotalPersons() > 0) {
            response.setMessage("Phát hiện " + mlResult.getTotalPersons() +
                    " người nhưng không khớp với danh sách người mất tích.");
        } else {
            response.setMessage("Không phát hiện đối tượng nào trong ảnh.");
        }

        return response;
    }

    /**
     * Nhận diện và so khớp Fusion (Face + Body Re-ID) từ file video tải lên:
     * 1. Lưu video vào ./uploads/
     * 2. Gọi ML Service phân tích fusion video
     * 3. Xử lý uniquePersons:
     *    - Lưu snapshot tốt nhất
     *    - Tra cứu hồ sơ MySQL
     *    - Lưu log_phat_hien & gửi email alert
     * 4. Xử lý timeline:
     *    - Lưu snapshot từng event
     *    - Gắn hồ sơ nếu khớp
     * 5. Trả về FusionVideoDetectionResponse
     */
    @Transactional
    public FusionVideoDetectionResponse detectAndMatchFusionVideo(MultipartFile file, Double faceThreshold, Double bodyThreshold, Double frameInterval) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Tệp video không được để trống.");
        }

        double activeFaceThresh = faceThreshold != null ? faceThreshold : defaultThreshold;
        double activeBodyThresh = bodyThreshold != null ? bodyThreshold : 0.65;
        double activeInterval = frameInterval != null ? frameInterval : 1.0;

        // 1. Lưu video vật lý vào uploads
        String storedVideoUrl = fileStorageService.storeFile(file);

        // 2. Gửi sang ML Service phân tích video
        MlFusionVideoResponse mlResult = mlServiceClient.detectAndMatchFusionVideo(file, activeFaceThresh, activeBodyThresh, activeInterval);

        FusionVideoDetectionResponse response = new FusionVideoDetectionResponse();
        response.setVideoUrl(storedVideoUrl);
        response.setDurationSeconds(mlResult.getDurationSeconds());
        response.setTotalFrames(mlResult.getTotalFrames());
        response.setProcessedFrames(mlResult.getProcessedFrames());
        response.setTotalDetections(mlResult.getTotalDetections());
        response.setMatched(Boolean.TRUE.equals(mlResult.getMatched()));
        response.setTotalMatches(mlResult.getTotalMatches());

        Map<Long, NguoiMatTich> personCache = new HashMap<>();

        // 3. Xử lý danh sách tóm tắt người mất tích (unique_persons)
        List<FusionVideoPersonSummaryDto> summaryList = new ArrayList<>();
        if (mlResult.getUniquePersons() != null) {
            for (MlFusionVideoPersonSummary pSum : mlResult.getUniquePersons()) {
                FusionVideoPersonSummaryDto sDto = new FusionVideoPersonSummaryDto();
                sDto.setPersonId(pSum.getPersonId());
                sDto.setFusionStatus(pSum.getFusionStatus());
                sDto.setMaxFaceSimilarity(pSum.getMaxFaceSimilarity());
                sDto.setMaxBodySimilarity(pSum.getMaxBodySimilarity());
                sDto.setOccurrencesCount(pSum.getOccurrencesCount());
                sDto.setFirstSeen(pSum.getFirstSeen());
                sDto.setFirstSeenSeconds(pSum.getFirstSeenSeconds());
                sDto.setLastSeen(pSum.getLastSeen());
                sDto.setLastSeenSeconds(pSum.getLastSeenSeconds());
                sDto.setTimestamps(pSum.getTimestamps());
                sDto.setBestSnapshotBase64(pSum.getBestSnapshotBase64());

                // Lưu ảnh snapshot khung hình tốt nhất
                String bestSnapshotUrl = fileStorageService.storeBase64Image(pSum.getBestSnapshotBase64(), "vid_fusion_best_");
                sDto.setBestSnapshotUrl(bestSnapshotUrl);

                if (pSum.getPersonId() != null) {
                    Optional<NguoiMatTich> nmtOpt = nguoiMatTichRepository.findByVectorIdFaiss(pSum.getPersonId());
                    if (nmtOpt.isPresent()) {
                        NguoiMatTich nmt = nmtOpt.get();
                        personCache.put(pSum.getPersonId(), nmt);
                        sDto.setHoSo(NguoiMatTichResponse.fromEntity(nmt));

                        // Lưu log_phat_hien khi khớp (Face) hoặc nghi vấn trang phục (Body)
                        String status = pSum.getFusionStatus();
                        boolean isFaceMatch = "CONFIRMED".equals(status)
                                || "FACE_MATCH_BODY_MISMATCH".equals(status)
                                || "FACE_CANDIDATE".equals(status);
                        boolean isBodyCandidate = "BODY_CANDIDATE".equals(status);

                        if (isFaceMatch || isBodyCandidate) {
                            LogPhatHien log = new LogPhatHien();
                            log.setNguoiMatTich(nmt);
                            log.setThoiGian(LocalDateTime.now());
                            log.setFaceSimilarity(pSum.getMaxFaceSimilarity());
                            log.setBodySimilarity(pSum.getMaxBodySimilarity());
                            log.setFusionStatus(status);
                            log.setBodyWarning("FACE_MATCH_BODY_MISMATCH".equals(status));
                            log.setDoTinCay(pSum.getMaxFaceSimilarity() != null ? pSum.getMaxFaceSimilarity() : (pSum.getMaxBodySimilarity() != null ? pSum.getMaxBodySimilarity() : 0f));
                            log.setAnhChupUrl(bestSnapshotUrl);
                            LogPhatHien savedLog = logPhatHienRepository.save(log);

                            // Gửi email cảnh báo khẩn cấp (Async)
                            if ("CONFIRMED".equals(status) || "FACE_MATCH_BODY_MISMATCH".equals(status)) {
                                emailService.sendMissingPersonAlert(
                                        nmt.getLienHeNguoiThan(),
                                        nmt.getHoTen(),
                                        pSum.getMaxFaceSimilarity() != null ? pSum.getMaxFaceSimilarity() : 0f,
                                        savedLog.getThoiGian(),
                                        bestSnapshotUrl
                                );
                            }
                        }
                    }
                }
                summaryList.add(sDto);
            }
        }
        response.setUniquePersons(summaryList);

        // 4. Xử lý danh sách timeline chi tiết
        List<FusionVideoTimelineItemDto> timelineList = new ArrayList<>();
        if (mlResult.getTimeline() != null) {
            for (MlFusionVideoTimelineItem item : mlResult.getTimeline()) {
                FusionVideoTimelineItemDto tDto = new FusionVideoTimelineItemDto();
                tDto.setTimestamp(item.getTimestamp());
                tDto.setTimestampFormatted(item.getTimestampFormatted());
                tDto.setFrameIndex(item.getFrameIndex());
                tDto.setPersonId(item.getPersonId());
                tDto.setFusionStatus(item.getFusionStatus());
                tDto.setFaceSimilarity(item.getFaceSimilarity());
                tDto.setBodySimilarity(item.getBodySimilarity());
                tDto.setBbox(item.getBbox());
                tDto.setBodyWarning(item.getBodyWarning());
                tDto.setMessage(item.getMessage());
                tDto.setSnapshotBase64(item.getSnapshotBase64());

                String snapUrl = fileStorageService.storeBase64Image(item.getSnapshotBase64(), "vid_fusion_event_");
                tDto.setSnapshotUrl(snapUrl);

                if (item.getPersonId() != null) {
                    NguoiMatTich nmt = personCache.computeIfAbsent(item.getPersonId(),
                            vid -> nguoiMatTichRepository.findByVectorIdFaiss(vid).orElse(null));
                    if (nmt != null) {
                        tDto.setHoSo(NguoiMatTichResponse.fromEntity(nmt));
                    }
                }

                timelineList.add(tDto);
            }
        }
        response.setTimeline(timelineList);

        // 5. Thông điệp phản hồi
        if (Boolean.TRUE.equals(response.getMatched())) {
            response.setMessage("PHÁT HIỆN " + summaryList.size() + " NGƯỜI THÂN TRONG VIDEO (FUSION) (Tổng " +
                    response.getTotalMatches() + " lượt xuất hiện trên " + response.getProcessedFrames() + " khung hình được quét).");
        } else if (response.getTotalDetections() != null && response.getTotalDetections() > 0) {
            response.setMessage("Đã quét video (" + response.getProcessedFrames() + " khung hình), phát hiện " +
                    response.getTotalDetections() + " lượt đối tượng nhưng không khớp với danh sách người mất tích.");
        } else {
            response.setMessage("Đã quét xong video nhưng không phát hiện đối tượng nào trong các khung hình.");
        }

        return response;
    }
}
