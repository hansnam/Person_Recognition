package com.example.coreservice.dto;

import com.example.coreservice.entity.NguoiMatTich;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class NguoiMatTichResponse {

    private Long id;
    private String hoTen;
    private String anhDaiDienUrl;
    private Long vectorIdFaiss;
    private LocalDate ngayMatTich;
    private String khuVuc;
    private String lienHeNguoiThan;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public NguoiMatTichResponse() {
    }

    public static NguoiMatTichResponse fromEntity(NguoiMatTich entity) {
        if (entity == null) return null;
        NguoiMatTichResponse dto = new NguoiMatTichResponse();
        dto.setId(entity.getId());
        dto.setHoTen(entity.getHoTen());
        dto.setAnhDaiDienUrl(entity.getAnhDaiDienUrl());
        dto.setVectorIdFaiss(entity.getVectorIdFaiss());
        dto.setNgayMatTich(entity.getNgayMatTich());
        dto.setKhuVuc(entity.getKhuVuc());
        dto.setLienHeNguoiThan(entity.getLienHeNguoiThan());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public String getAnhDaiDienUrl() {
        return anhDaiDienUrl;
    }

    public void setAnhDaiDienUrl(String anhDaiDienUrl) {
        this.anhDaiDienUrl = anhDaiDienUrl;
    }

    public Long getVectorIdFaiss() {
        return vectorIdFaiss;
    }

    public void setVectorIdFaiss(Long vectorIdFaiss) {
        this.vectorIdFaiss = vectorIdFaiss;
    }

    public LocalDate getNgayMatTich() {
        return ngayMatTich;
    }

    public void setNgayMatTich(LocalDate ngayMatTich) {
        this.ngayMatTich = ngayMatTich;
    }

    public String getKhuVuc() {
        return khuVuc;
    }

    public void setKhuVuc(String khuVuc) {
        this.khuVuc = khuVuc;
    }

    public String getLienHeNguoiThan() {
        return lienHeNguoiThan;
    }

    public void setLienHeNguoiThan(String lienHeNguoiThan) {
        this.lienHeNguoiThan = lienHeNguoiThan;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
