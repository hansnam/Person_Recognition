package com.example.coreservice.dto;

import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class NguoiMatTichRequest {

    @NotBlank(message = "Họ tên không được để trống")
    private String hoTen;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate ngayMatTich;

    private String khuVuc;

    @NotBlank(message = "Email hoặc liên hệ người thân không được để trống")
    private String lienHeNguoiThan;

    public NguoiMatTichRequest() {
    }

    public NguoiMatTichRequest(String hoTen, LocalDate ngayMatTich, String khuVuc, String lienHeNguoiThan) {
        this.hoTen = hoTen;
        this.ngayMatTich = ngayMatTich;
        this.khuVuc = khuVuc;
        this.lienHeNguoiThan = lienHeNguoiThan;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
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
}
