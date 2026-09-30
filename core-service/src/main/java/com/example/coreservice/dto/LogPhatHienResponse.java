package com.example.coreservice.dto;

import com.example.coreservice.entity.LogPhatHien;

import java.time.LocalDateTime;

public class LogPhatHienResponse {

    private Long id;
    private Long nguoiMatTichId;
    private String hoTenNguoiMatTich;
    private String lienHeNguoiThan;
    private LocalDateTime thoiGian;
    private Float doTinCay;
    private Float faceSimilarity;
    private Float bodySimilarity;
    private String fusionStatus;
    private Boolean bodyWarning;
    private String anhChupUrl;

    public LogPhatHienResponse() {
    }

    public static LogPhatHienResponse fromEntity(LogPhatHien entity) {
        if (entity == null) return null;
        LogPhatHienResponse dto = new LogPhatHienResponse();
        dto.setId(entity.getId());
        if (entity.getNguoiMatTich() != null) {
            dto.setNguoiMatTichId(entity.getNguoiMatTich().getId());
            dto.setHoTenNguoiMatTich(entity.getNguoiMatTich().getHoTen());
            dto.setLienHeNguoiThan(entity.getNguoiMatTich().getLienHeNguoiThan());
        }
        dto.setThoiGian(entity.getThoiGian());
        dto.setDoTinCay(entity.getDoTinCay());
        dto.setFaceSimilarity(entity.getFaceSimilarity());
        dto.setBodySimilarity(entity.getBodySimilarity());
        dto.setFusionStatus(entity.getFusionStatus());
        dto.setBodyWarning(entity.getBodyWarning());
        dto.setAnhChupUrl(entity.getAnhChupUrl());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNguoiMatTichId() {
        return nguoiMatTichId;
    }

    public void setNguoiMatTichId(Long nguoiMatTichId) {
        this.nguoiMatTichId = nguoiMatTichId;
    }

    public String getHoTenNguoiMatTich() {
        return hoTenNguoiMatTich;
    }

    public void setHoTenNguoiMatTich(String hoTenNguoiMatTich) {
        this.hoTenNguoiMatTich = hoTenNguoiMatTich;
    }

    public String getLienHeNguoiThan() {
        return lienHeNguoiThan;
    }

    public void setLienHeNguoiThan(String lienHeNguoiThan) {
        this.lienHeNguoiThan = lienHeNguoiThan;
    }

    public LocalDateTime getThoiGian() {
        return thoiGian;
    }

    public void setThoiGian(LocalDateTime thoiGian) {
        this.thoiGian = thoiGian;
    }

    public Float getDoTinCay() {
        return doTinCay;
    }

    public void setDoTinCay(Float doTinCay) {
        this.doTinCay = doTinCay;
    }

    public Float getFaceSimilarity() {
        return faceSimilarity;
    }

    public void setFaceSimilarity(Float faceSimilarity) {
        this.faceSimilarity = faceSimilarity;
    }

    public Float getBodySimilarity() {
        return bodySimilarity;
    }

    public void setBodySimilarity(Float bodySimilarity) {
        this.bodySimilarity = bodySimilarity;
    }

    public String getFusionStatus() {
        return fusionStatus;
    }

    public void setFusionStatus(String fusionStatus) {
        this.fusionStatus = fusionStatus;
    }

    public Boolean getBodyWarning() {
        return bodyWarning;
    }

    public void setBodyWarning(Boolean bodyWarning) {
        this.bodyWarning = bodyWarning;
    }

    public String getAnhChupUrl() {
        return anhChupUrl;
    }

    public void setAnhChupUrl(String anhChupUrl) {
        this.anhChupUrl = anhChupUrl;
    }
}
