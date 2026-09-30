package com.example.coreservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class MlFusionResponse {

    private Boolean matched;

    @JsonProperty("total_persons")
    private Integer totalPersons;

    @JsonProperty("total_ms")
    private Long totalMs;

    private List<MlFusionDetectionItem> detections;

    public MlFusionResponse() {
    }

    public Boolean getMatched() {
        return matched;
    }

    public void setMatched(Boolean matched) {
        this.matched = matched;
    }

    public Integer getTotalPersons() {
        return totalPersons;
    }

    public void setTotalPersons(Integer totalPersons) {
        this.totalPersons = totalPersons;
    }

    public Long getTotalMs() {
        return totalMs;
    }

    public void setTotalMs(Long totalMs) {
        this.totalMs = totalMs;
    }

    public List<MlFusionDetectionItem> getDetections() {
        return detections;
    }

    public void setDetections(List<MlFusionDetectionItem> detections) {
        this.detections = detections;
    }
}
