package com.example.coreservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class MlDetectionItem {

    private List<Integer> bbox;
    private Float confidence;
    private Boolean matched;

    @JsonProperty("vector_id")
    private Long vectorId;

    private Float similarity;

    public MlDetectionItem() {
    }

    public List<Integer> getBbox() {
        return bbox;
    }

    public void setBbox(List<Integer> bbox) {
        this.bbox = bbox;
    }

    public Float getConfidence() {
        return confidence;
    }

    public void setConfidence(Float confidence) {
        this.confidence = confidence;
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
}
