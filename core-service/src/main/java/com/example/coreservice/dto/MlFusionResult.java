package com.example.coreservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MlFusionResult {

    private String status;

    @JsonProperty("person_id")
    private Long personId;

    @JsonProperty("body_warning")
    private Boolean bodyWarning;

    private String message;

    public MlFusionResult() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getPersonId() {
        return personId;
    }

    public void setPersonId(Long personId) {
        this.personId = personId;
    }

    public Boolean getBodyWarning() {
        return bodyWarning;
    }

    public void setBodyWarning(Boolean bodyWarning) {
        this.bodyWarning = bodyWarning;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
