package com.example.coreservice.repository;

import com.example.coreservice.entity.LogPhatHien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogPhatHienRepository extends JpaRepository<LogPhatHien, Long> {

    /**
     * Lấy danh sách lịch sử phát hiện của một người mất tích cụ thể, sắp xếp mới nhất trước
     */
    List<LogPhatHien> findByNguoiMatTichIdOrderByThoiGianDesc(Long nguoiMatTichId);

    /**
     * Lấy toàn bộ lịch sử phát hiện sắp xếp theo thời gian mới nhất trước
     */
    List<LogPhatHien> findAllByOrderByThoiGianDesc();
}
