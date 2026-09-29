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
}
