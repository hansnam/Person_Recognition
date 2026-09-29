package com.example.coreservice.service;

import com.example.coreservice.client.MlServiceClient;
import com.example.coreservice.dto.MlRegisterResponse;
import com.example.coreservice.dto.NguoiMatTichRequest;
import com.example.coreservice.dto.NguoiMatTichResponse;
import com.example.coreservice.entity.LogPhatHien;
import com.example.coreservice.entity.NguoiMatTich;
import com.example.coreservice.exception.ResourceNotFoundException;
import com.example.coreservice.repository.LogPhatHienRepository;
import com.example.coreservice.repository.NguoiMatTichRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NguoiMatTichService {

    private final NguoiMatTichRepository nguoiMatTichRepository;
    private final LogPhatHienRepository logPhatHienRepository;
    private final MlServiceClient mlServiceClient;
    private final FileStorageService fileStorageService;

    public NguoiMatTichService(NguoiMatTichRepository nguoiMatTichRepository,
                               LogPhatHienRepository logPhatHienRepository,
                               MlServiceClient mlServiceClient,
                               FileStorageService fileStorageService) {
        this.nguoiMatTichRepository = nguoiMatTichRepository;
        this.logPhatHienRepository = logPhatHienRepository;
        this.mlServiceClient = mlServiceClient;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Đăng ký hồ sơ người mất tích:
     * 1. Lưu ảnh upload vào ./uploads
     * 2. Gửi ảnh sang ML Service để trích xuất embedding và lưu vào FAISS
     * 3. Lưu thông tin hồ sơ kèm vector_id_faiss vào MySQL
     */
    @Transactional
    public NguoiMatTichResponse create(NguoiMatTichRequest request, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Ảnh chân dung không được để trống.");
        }

        // 1. Lưu file ảnh vào thư mục server
        String savedImageUrl = fileStorageService.storeFile(file);

        try {
            // 2. Đăng ký khuôn mặt vào FAISS qua ML Service
            MlRegisterResponse mlResponse = mlServiceClient.registerFace(file, null);
            Long vectorId = mlResponse.getVectorId();

            // 3. Tạo entity và lưu vào MySQL
            NguoiMatTich entity = new NguoiMatTich();
            entity.setHoTen(request.getHoTen());
            entity.setNgayMatTich(request.getNgayMatTich());
            entity.setKhuVuc(request.getKhuVuc());
            entity.setLienHeNguoiThan(request.getLienHeNguoiThan());
            entity.setAnhDaiDienUrl(savedImageUrl);
            entity.setVectorIdFaiss(vectorId);

            NguoiMatTich savedEntity = nguoiMatTichRepository.save(entity);
            return NguoiMatTichResponse.fromEntity(savedEntity);

        } catch (Exception e) {
            // Rollback xoá file nếu có lỗi xảy ra
            fileStorageService.deleteFile(savedImageUrl);
            throw e;
        }
    }

    /**
     * Lấy danh sách hồ sơ (hỗ trợ lọc theo họ tên)
     */
    @Transactional(readOnly = true)
    public List<NguoiMatTichResponse> getAll(String keyword) {
        List<NguoiMatTich> list;
        if (keyword != null && !keyword.trim().isEmpty()) {
            list = nguoiMatTichRepository.findByHoTenContainingIgnoreCase(keyword.trim());
        } else {
            list = nguoiMatTichRepository.findAll();
        }
        return list.stream()
                .map(NguoiMatTichResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lấy chi tiết một hồ sơ theo id
     */
    @Transactional(readOnly = true)
    public NguoiMatTichResponse getById(Long id) {
        NguoiMatTich entity = nguoiMatTichRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ người mất tích có id=" + id));
        return NguoiMatTichResponse.fromEntity(entity);
    }

    /**
     * Cập nhật thông tin hồ sơ (có thể cập nhật kèm ảnh mới)
     */
    @Transactional
    public NguoiMatTichResponse update(Long id, NguoiMatTichRequest request, MultipartFile newFile) {
        NguoiMatTich entity = nguoiMatTichRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ người mất tích có id=" + id));

        entity.setHoTen(request.getHoTen());
        entity.setNgayMatTich(request.getNgayMatTich());
        entity.setKhuVuc(request.getKhuVuc());
        entity.setLienHeNguoiThan(request.getLienHeNguoiThan());

        if (newFile != null && !newFile.isEmpty()) {
            // Lưu ảnh mới
            String newImageUrl = fileStorageService.storeFile(newFile);

            // Xoá vector cũ trên FAISS
            mlServiceClient.deleteFace(entity.getVectorIdFaiss());

            // Đăng ký vector mới trên FAISS
            MlRegisterResponse mlResponse = mlServiceClient.registerFace(newFile, null);
            entity.setVectorIdFaiss(mlResponse.getVectorId());

            // Xoá ảnh cũ
            fileStorageService.deleteFile(entity.getAnhDaiDienUrl());
            entity.setAnhDaiDienUrl(newImageUrl);
        }

        NguoiMatTich updated = nguoiMatTichRepository.save(entity);
        return NguoiMatTichResponse.fromEntity(updated);
    }

    /**
     * Xoá hồ sơ người mất tích:
     * 1. Xoá vector tương ứng trong FAISS của ML Service
     * 2. Xoá các bản ghi log phát hiện liên quan trong MySQL
     * 3. Xoá hồ sơ trong MySQL
     * 4. Xoá file ảnh đã lưu
     */
    @Transactional
    public void delete(Long id) {
        NguoiMatTich entity = nguoiMatTichRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ người mất tích có id=" + id));

        // 1. Xoá vector trong FAISS
        try {
            mlServiceClient.deleteFace(entity.getVectorIdFaiss());
        } catch (Exception e) {
            // Log cảnh báo nhưng vẫn tiếp tục xoá dữ liệu trong MySQL
            System.err.println("[Cảnh báo] Lỗi khi xoá vector trong FAISS: " + e.getMessage());
        }

        // 2. Xoá toàn bộ log phát hiện liên kết
        List<LogPhatHien> logs = logPhatHienRepository.findByNguoiMatTichIdOrderByThoiGianDesc(id);
        logPhatHienRepository.deleteAll(logs);

        // 3. Xoá file ảnh lưu trữ
        fileStorageService.deleteFile(entity.getAnhDaiDienUrl());

        // 4. Xoá hồ sơ trong MySQL
        nguoiMatTichRepository.delete(entity);
    }
}
