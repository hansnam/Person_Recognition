package com.example.coreservice.controller;

import com.example.coreservice.dto.LogPhatHienResponse;
import com.example.coreservice.service.LogPhatHienService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
public class LogPhatHienController {

    private final LogPhatHienService logPhatHienService;

    public LogPhatHienController(LogPhatHienService logPhatHienService) {
        this.logPhatHienService = logPhatHienService;
    }

    /**
     * Lấy toàn bộ lịch sử phát hiện, sắp xếp theo thời gian mới nhất
     */
    @GetMapping
    public ResponseEntity<List<LogPhatHienResponse>> getAllLogs() {
        return ResponseEntity.ok(logPhatHienService.getAllLogs());
    }

    /**
     * Lấy lịch sử phát hiện của một hồ sơ người mất tích cụ thể
     */
    @GetMapping("/nguoi-mat-tich/{id}")
    public ResponseEntity<List<LogPhatHienResponse>> getLogsByNguoiMatTich(@PathVariable Long id) {
        return ResponseEntity.ok(logPhatHienService.getLogsByNguoiMatTich(id));
    }
}
