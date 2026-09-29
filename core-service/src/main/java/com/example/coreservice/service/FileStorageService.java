package com.example.coreservice.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Không thể khởi tạo thư mục lưu trữ ảnh: " + uploadDir, e);
        }
    }

    /**
     * Lưu file upload vào thư mục uploads với tên UUID duy nhất
     * @return đường dẫn tương đối URL, ví dụ: "/uploads/abc-123.jpg"
     */
    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File tải lên không được rỗng");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        } else {
            extension = ".jpg";
        }

        String newFilename = UUID.randomUUID().toString() + extension;
        Path targetLocation = this.rootLocation.resolve(newFilename);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi lưu file ảnh lên server: " + newFilename, e);
        }

        return "/uploads/" + newFilename;
    }

    /**
     * Xoá file ảnh theo URL tương đối
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("/uploads/")) {
            return;
        }
        String filename = fileUrl.replace("/uploads/", "");
        try {
            Path filePath = this.rootLocation.resolve(filename);
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {
        }
    }

    /**
     * Lưu ảnh từ chuỗi Base64 (ví dụ: snapshot từ khung hình video)
     * @param base64Data chuỗi base64 có hoặc không có prefix data:image/...;base64,
     * @param prefix tiền tố tên file (ví dụ "vid_snap_")
     * @return đường dẫn tương đối URL, ví dụ: "/uploads/vid_snap_abc-123.jpg"
     */
    public String storeBase64Image(String base64Data, String prefix) {
        if (base64Data == null || base64Data.trim().isEmpty()) {
            return null;
        }

        try {
            String cleanBase64 = base64Data;
            if (cleanBase64.contains(",")) {
                cleanBase64 = cleanBase64.substring(cleanBase64.indexOf(",") + 1);
            }

            byte[] decodedBytes = java.util.Base64.getDecoder().decode(cleanBase64.trim());
            String p = prefix != null ? prefix : "snap_";
            String newFilename = p + UUID.randomUUID().toString() + ".jpg";
            Path targetLocation = this.rootLocation.resolve(newFilename);

            Files.write(targetLocation, decodedBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return "/uploads/" + newFilename;
        } catch (Exception e) {
            return null;
        }
    }
}
