package com.example.coreservice.repository;

import com.example.coreservice.entity.NguoiMatTich;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NguoiMatTichRepository extends JpaRepository<NguoiMatTich, Long> {

    /**
     * Tra cứu hồ sơ người mất tích dựa vào vector_id_faiss nhận từ ML Service
     */
    Optional<NguoiMatTich> findByVectorIdFaiss(Long vectorIdFaiss);

    /**
     * Tìm kiếm hồ sơ theo họ tên (không phân biệt hoa/thường)
     */
    List<NguoiMatTich> findByHoTenContainingIgnoreCase(String hoTen);

    /**
     * Kiểm tra sự tồn tại của vectorIdFaiss
     */
    boolean existsByVectorIdFaiss(Long vectorIdFaiss);
}
