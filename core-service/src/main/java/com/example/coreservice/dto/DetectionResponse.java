package com.example.coreservice.dto;

import java.util.List;

public class DetectionResponse {

    private Boolean matched;
    private Float similarity;
    private NguoiMatTichResponse hoSo;
    private Long logId;
    private String anhChupUrl;
    private List<Integer> bbox;
    private List<FaceDetectionDto> detections;
    private String message;

    public DetectionResponse() {
    }

    public List<FaceDetectionDto> getDetections() {
        return detections;
    }

    public void setDetections(List<FaceDetectionDto> detections) {
        this.detections = detections;
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

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public String getAnhChupUrl() {
        return anhChupUrl;
    }

    public void setAnhChupUrl(String anhChupUrl) {
        this.anhChupUrl = anhChupUrl;
    }

    public List<Integer> getBbox() {
        return bbox;
    }

    public void setBbox(List<Integer> bbox) {
        this.bbox = bbox;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
