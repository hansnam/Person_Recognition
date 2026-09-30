package com.example.coreservice.controller;

import com.example.coreservice.dto.DetectionResponse;
import com.example.coreservice.dto.FusionDetectionResponse;
import com.example.coreservice.dto.FusionVideoDetectionResponse;
import com.example.coreservice.dto.VideoDetectionResponse;
import com.example.coreservice.service.DetectionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/detection")
public class DetectionController {

    private final DetectionService detectionService;

    public DetectionController(DetectionService detectionService) {
        this.detectionService = detectionService;
    }

    /**
     * Gửi ảnh/khung hình giám sát để nhận diện và so khớp với danh sách người mất tích (Face-only baseline)
     * @param file ảnh chụp từ camera hoặc tải lên từ web
     * @param threshold ngưỡng tương đồng (tuỳ chọn, mặc định 0.45)
     */
    @PostMapping(value = "/match", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DetectionResponse> detectAndMatch(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "threshold", required = false) Double threshold) {
        DetectionResponse response = detectionService.detectAndMatch(file, threshold);
        return ResponseEntity.ok(response);
    }

    /**
     * Tải lên file video giám sát để quét và nhận diện khuôn mặt người mất tích (Face-only baseline)
     * @param file file video (.mp4, .avi, .mov, .mkv, .webm)
     * @param threshold ngưỡng tương đồng (tuỳ chọn, mặc định 0.45)
     * @param frameInterval khoảng cách giây giữa các khung hình được quét (tuỳ chọn, mặc định 1.0 giây)
     */
    @PostMapping(value = "/match-video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VideoDetectionResponse> detectAndMatchVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "threshold", required = false) Double threshold,
            @RequestParam(value = "frameInterval", required = false) Double frameInterval) {
        VideoDetectionResponse response = detectionService.detectAndMatchVideo(file, threshold, frameInterval);
        return ResponseEntity.ok(response);
    }

    /**
     * Gửi ảnh/khung hình giám sát để nhận diện Fusion (Face Recognition + Body Re-ID)
     * @param file ảnh chụp từ camera hoặc tải lên từ web
     * @param faceThreshold ngưỡng tương đồng khuôn mặt (tuỳ chọn, mặc định 0.45)
     * @param bodyThreshold ngưỡng tương đồng thân hình (tuỳ chọn, mặc định 0.65)
     */
    @PostMapping(value = "/match/fusion", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FusionDetectionResponse> detectAndMatchFusion(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "faceThreshold", required = false) Double faceThreshold,
            @RequestParam(value = "bodyThreshold", required = false) Double bodyThreshold) {
        FusionDetectionResponse response = detectionService.detectAndMatchFusion(file, faceThreshold, bodyThreshold);
        return ResponseEntity.ok(response);
    }

    /**
     * Tải lên file video giám sát để quét và nhận diện Fusion (Face Recognition + Body Re-ID)
     * @param file file video (.mp4, .avi, .mov, .mkv, .webm)
     * @param faceThreshold ngưỡng tương đồng khuôn mặt (tuỳ chọn, mặc định 0.45)
     * @param bodyThreshold ngưỡng tương đồng thân hình (tuỳ chọn, mặc định 0.65)
     * @param frameInterval khoảng cách giây giữa các khung hình được quét (tuỳ chọn, mặc định 1.0 giây)
     */
    @PostMapping(value = "/match/fusion-video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FusionVideoDetectionResponse> detectAndMatchFusionVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "faceThreshold", required = false) Double faceThreshold,
            @RequestParam(value = "bodyThreshold", required = false) Double bodyThreshold,
            @RequestParam(value = "frameInterval", required = false) Double frameInterval) {
        FusionVideoDetectionResponse response = detectionService.detectAndMatchFusionVideo(file, faceThreshold, bodyThreshold, frameInterval);
        return ResponseEntity.ok(response);
    }
}
