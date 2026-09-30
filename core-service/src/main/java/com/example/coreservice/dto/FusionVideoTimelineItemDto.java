package com.example.coreservice.dto;

import java.util.List;

public class FusionVideoTimelineItemDto {

    private Double timestamp;
    private String timestampFormatted;
    private Integer frameIndex;
    private Long personId;
    private String fusionStatus;
    private Float faceSimilarity;
    private Float bodySimilarity;
    private List<Integer> bbox;
    private Boolean bodyWarning;
    private String message;
    private String snapshotBase64;
    private String snapshotUrl;
    private NguoiMatTichResponse hoSo;

    public FusionVideoTimelineItemDto() {
    }

    public Double getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Double timestamp) {
        this.timestamp = timestamp;
    }

    public String getTimestampFormatted() {
        return timestampFormatted;
    }

    public void setTimestampFormatted(String timestampFormatted) {
        this.timestampFormatted = timestampFormatted;
    }

    public Integer getFrameIndex() {
        return frameIndex;
    }

    public void setFrameIndex(Integer frameIndex) {
        this.frameIndex = frameIndex;
    }

    public Long getPersonId() {
        return personId;
    }

    public void setPersonId(Long personId) {
        this.personId = personId;
    }

    public String getFusionStatus() {
        return fusionStatus;
    }

    public void setFusionStatus(String fusionStatus) {
        this.fusionStatus = fusionStatus;
    }

    public Float getFaceSimilarity() {
        return faceSimilarity;
    }

    public void setFaceSimilarity(Float faceSimilarity) {
        this.faceSimilarity = faceSimilarity;
    }

    public Float getBodySimilarity() {
        return bodySimilarity;
    }

    public void setBodySimilarity(Float bodySimilarity) {
        this.bodySimilarity = bodySimilarity;
    }

    public List<Integer> getBbox() {
        return bbox;
    }

    public void setBbox(List<Integer> bbox) {
        this.bbox = bbox;
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

    public String getSnapshotBase64() {
        return snapshotBase64;
    }

    public void setSnapshotBase64(String snapshotBase64) {
        this.snapshotBase64 = snapshotBase64;
    }

    public String getSnapshotUrl() {
        return snapshotUrl;
    }

    public void setSnapshotUrl(String snapshotUrl) {
        this.snapshotUrl = snapshotUrl;
    }

    public NguoiMatTichResponse getHoSo() {
        return hoSo;
    }

    public void setHoSo(NguoiMatTichResponse hoSo) {
        this.hoSo = hoSo;
    }
}
