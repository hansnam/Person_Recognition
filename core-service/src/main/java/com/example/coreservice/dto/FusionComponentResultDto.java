package com.example.coreservice.dto;

import java.util.List;

public class FusionComponentResultDto {

    private Boolean matched;
    private Long personId;
    private Float similarity;
    private List<Integer> bbox;

    public FusionComponentResultDto() {
    }

    public FusionComponentResultDto(Boolean matched, Long personId, Float similarity, List<Integer> bbox) {
        this.matched = matched;
        this.personId = personId;
        this.similarity = similarity;
        this.bbox = bbox;
    }

    public Boolean getMatched() {
        return matched;
    }

    public void setMatched(Boolean matched) {
        this.matched = matched;
    }

    public Long getPersonId() {
        return personId;
    }

    public void setPersonId(Long personId) {
        this.personId = personId;
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
}
