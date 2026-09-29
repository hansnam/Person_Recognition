package com.example.coreservice.service;

import com.example.coreservice.dto.LogPhatHienResponse;
import com.example.coreservice.entity.LogPhatHien;
import com.example.coreservice.repository.LogPhatHienRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LogPhatHienService {

    private final LogPhatHienRepository logPhatHienRepository;

    public LogPhatHienService(LogPhatHienRepository logPhatHienRepository) {
        this.logPhatHienRepository = logPhatHienRepository;
    }

    /**
     * Lấy toàn bộ lịch sử phát hiện, sắp xếp mới nhất trước
     */
    @Transactional(readOnly = true)
    public List<LogPhatHienResponse> getAllLogs() {
        return logPhatHienRepository.findAllByOrderByThoiGianDesc()
                .stream()
                .map(LogPhatHienResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lấy lịch sử phát hiện theo một người mất tích cụ thể
     */
    @Transactional(readOnly = true)
    public List<LogPhatHienResponse> getLogsByNguoiMatTich(Long nguoiMatTichId) {
        return logPhatHienRepository.findByNguoiMatTichIdOrderByThoiGianDesc(nguoiMatTichId)
                .stream()
                .map(LogPhatHienResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
