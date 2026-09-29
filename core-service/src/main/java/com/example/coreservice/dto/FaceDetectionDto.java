package com.example.coreservice.dto;

import java.util.List;

public class FaceDetectionDto {

    private List<Integer> bbox;
    private Boolean matched;
    private Float similarity;
    private NguoiMatTichResponse hoSo;

    public FaceDetectionDto() {
    }

    public FaceDetectionDto(List<Integer> bbox, Boolean matched, Float similarity, NguoiMatTichResponse hoSo) {
        this.bbox = bbox;
        this.matched = matched;
        this.similarity = similarity;
        this.hoSo = hoSo;
    }

    public List<Integer> getBbox() {
        return bbox;
    }

    public void setBbox(List<Integer> bbox) {
        this.bbox = bbox;
    }

    public Boolean getMatched() {
        return matched;
    }

    public void setMatched(Boolean matched) {
        this.matched = matched;
    }

    public Float getSimilarity() {
        return similarity;
    }

    public void setSimilarity(Float similarity) {
        this.similarity = similarity;
    }

    public NguoiMatTichResponse getHoSo() {
        return hoSo;
    }

    public void setHoSo(NguoiMatTichResponse hoSo) {
        this.hoSo = hoSo;
    }
}
