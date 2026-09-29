package com.example.coreservice.repository;

import com.example.coreservice.entity.NguoiDung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NguoiDungRepository extends JpaRepository<NguoiDung, Long> {

    /**
     * Tìm kiếm người dùng theo email (dùng cho đăng nhập / xác thực JWT)
     */
    Optional<NguoiDung> findByEmail(String email);

    /**
     * Kiểm tra email đã được đăng ký chưa
     */
    boolean existsByEmail(String email);
}
