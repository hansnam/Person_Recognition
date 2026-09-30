package com.example.coreservice.dto;

import java.util.List;

public class FusionDetectionResponse {

    private Boolean matched;
    private String anhChupUrl;
    private String message;
    private List<FusionDetectionItemDto> detections;

    public FusionDetectionResponse() {
    }

    public FusionDetectionResponse(Boolean matched, String anhChupUrl, String message, List<FusionDetectionItemDto> detections) {
        this.matched = matched;
        this.anhChupUrl = anhChupUrl;
        this.message = message;
        this.detections = detections;
    }

    public Boolean getMatched() {
        return matched;
    }

    public void setMatched(Boolean matched) {
        this.matched = matched;
    }

    public String getAnhChupUrl() {
        return anhChupUrl;
    }

    public void setAnhChupUrl(String anhChupUrl) {
        this.anhChupUrl = anhChupUrl;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<FusionDetectionItemDto> getDetections() {
        return detections;
    }

    public void setDetections(List<FusionDetectionItemDto> detections) {
        this.detections = detections;
    }
}
