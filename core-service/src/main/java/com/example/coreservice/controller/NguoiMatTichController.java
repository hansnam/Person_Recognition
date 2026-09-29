package com.example.coreservice.controller;

import com.example.coreservice.dto.NguoiMatTichRequest;
import com.example.coreservice.dto.NguoiMatTichResponse;
import com.example.coreservice.service.NguoiMatTichService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/nguoi-mat-tich")
public class NguoiMatTichController {

    private final NguoiMatTichService nguoiMatTichService;

    public NguoiMatTichController(NguoiMatTichService nguoiMatTichService) {
        this.nguoiMatTichService = nguoiMatTichService;
    }

    /**
     * Đăng ký hồ sơ người mất tích kèm ảnh chân dung
     * Form-data: hoTen, ngayMatTich (YYYY-MM-DD), khuVuc, lienHeNguoiThan, file (ảnh)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<NguoiMatTichResponse> create(
            @ModelAttribute @Valid NguoiMatTichRequest request,
            @RequestParam("file") MultipartFile file) {
        NguoiMatTichResponse response = nguoiMatTichService.create(request, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lấy danh sách hồ sơ hoặc tìm kiếm theo họ tên
     */
    @GetMapping
    public ResponseEntity<List<NguoiMatTichResponse>> getAll(
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ResponseEntity.ok(nguoiMatTichService.getAll(keyword));
    }

    /**
     * Xem chi tiết hồ sơ
     */
    @GetMapping("/{id}")
    public ResponseEntity<NguoiMatTichResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(nguoiMatTichService.getById(id));
    }

    /**
     * Cập nhật thông tin hồ sơ (có thể cập nhật kèm ảnh mới)
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<NguoiMatTichResponse> update(
            @PathVariable Long id,
            @ModelAttribute @Valid NguoiMatTichRequest request,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.ok(nguoiMatTichService.update(id, request, file));
    }

    /**
     * Xoá hồ sơ người mất tích (đồng bộ xoá vector trong FAISS và MySQL)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        nguoiMatTichService.delete(id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã xoá thành công hồ sơ người mất tích id=" + id + " và vector liên quan trong FAISS."
        ));
    }
}
