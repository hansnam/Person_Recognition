package com.example.coreservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class MlMatchResponse {

    private Boolean matched;

    @JsonProperty("vector_id")
    private Long vectorId;

    private Float similarity;
    private List<Integer> bbox;

    @JsonProperty("total_faces_detected")
    private Integer totalFacesDetected;

    @JsonProperty("all_detections")
    private List<MlDetectionItem> allDetections;

    private String message;

    public MlMatchResponse() {
    }

    public List<MlDetectionItem> getAllDetections() {
        return allDetections;
    }

    public void setAllDetections(List<MlDetectionItem> allDetections) {
        this.allDetections = allDetections;
    }

    public Boolean getMatched() {
        return matched;
    }

    public void setMatched(Boolean matched) {
        this.matched = matched;
    }

    public Long getVectorId() {
        return vectorId;
    }

    public void setVectorId(Long vectorId) {
        this.vectorId = vectorId;
    }

    public Float getSimilarity() {
        return similarity;
    }

    public void setSimilarity(Float similarity) {
        this.similarity = similarity;
    }

    public List<Integer> getBbox() {
        return bbox;
    }

    public void setBbox(List<Integer> bbox) {
        this.bbox = bbox;
    }

    public Integer getTotalFacesDetected() {
        return totalFacesDetected;
    }

    public void setTotalFacesDetected(Integer totalFacesDetected) {
        this.totalFacesDetected = totalFacesDetected;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
