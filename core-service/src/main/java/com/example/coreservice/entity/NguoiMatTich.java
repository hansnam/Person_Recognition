package com.example.coreservice.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "nguoi_mat_tich", indexes = {
    @Index(name = "idx_vector_id_faiss", columnList = "vector_id_faiss", unique = true)
})
public class NguoiMatTich {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ho_ten", nullable = false)
    private String hoTen;

    @Column(name = "anh_dai_dien_url", length = 500)
    private String anhDaiDienUrl;

    @Column(name = "vector_id_faiss", nullable = false, unique = true)
    private Long vectorIdFaiss;

    @Column(name = "ngay_mat_tich")
    private LocalDate ngayMatTich;

    @Column(name = "khu_vuc")
    private String khuVuc;

    @Column(name = "lien_he_nguoi_than", nullable = false)
    private String lienHeNguoiThan;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public NguoiMatTich() {
    }

    public NguoiMatTich(Long id, String hoTen, String anhDaiDienUrl, Long vectorIdFaiss,
                        LocalDate ngayMatTich, String khuVuc, String lienHeNguoiThan) {
        this.id = id;
        this.hoTen = hoTen;
        this.anhDaiDienUrl = anhDaiDienUrl;
        this.vectorIdFaiss = vectorIdFaiss;
        this.ngayMatTich = ngayMatTich;
        this.khuVuc = khuVuc;
        this.lienHeNguoiThan = lienHeNguoiThan;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
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
