package com.example.coreservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "log_phat_hien", indexes = {
    @Index(name = "idx_log_thoi_gian", columnList = "thoi_gian"),
    @Index(name = "idx_log_nguoi_mat_tich", columnList = "nguoi_mat_tich_id")
})
public class LogPhatHien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "nguoi_mat_tich_id", nullable = false)
    private NguoiMatTich nguoiMatTich;

    @Column(name = "thoi_gian", nullable = false)
    private LocalDateTime thoiGian;

    @Column(name = "do_tin_cay", nullable = false)
    private Float doTinCay;

    @Column(name = "anh_chup_url", length = 500)
    private String anhChupUrl;

    public LogPhatHien() {
    }

    public LogPhatHien(Long id, NguoiMatTich nguoiMatTich, LocalDateTime thoiGian, Float doTinCay, String anhChupUrl) {
        this.id = id;
        this.nguoiMatTich = nguoiMatTich;
        this.thoiGian = thoiGian;
        this.doTinCay = doTinCay;
        this.anhChupUrl = anhChupUrl;
    }

    @PrePersist
    protected void onCreate() {
        if (this.thoiGian == null) {
            this.thoiGian = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public NguoiMatTich getNguoiMatTich() {
        return nguoiMatTich;
    }

    public void setNguoiMatTich(NguoiMatTich nguoiMatTich) {
        this.nguoiMatTich = nguoiMatTich;
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

    public String getAnhChupUrl() {
        return anhChupUrl;
    }

    public void setAnhChupUrl(String anhChupUrl) {
        this.anhChupUrl = anhChupUrl;
    }
}
