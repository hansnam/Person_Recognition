package com.example.coreservice.dto;

public class FusionStatusDto {

    private String status;
    private Long personId;
    private Boolean bodyWarning;
    private String message;

    public FusionStatusDto() {
    }

    public FusionStatusDto(String status, Long personId, Boolean bodyWarning, String message) {
        this.status = status;
        this.personId = personId;
        this.bodyWarning = bodyWarning;
        this.message = message;
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
