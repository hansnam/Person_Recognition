package com.example.coreservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class MlFusionComponentResult {

    private Boolean matched;

    @JsonProperty("person_id")
    private Long personId;

    private Float similarity;
    private List<Integer> bbox;

    public MlFusionComponentResult() {
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
