package com.example.coreservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class MlFusionVideoTimelineItem {

    private Double timestamp;

    @JsonProperty("timestamp_formatted")
    private String timestampFormatted;

    @JsonProperty("frame_index")
    private Integer frameIndex;

    @JsonProperty("person_id")
    private Long personId;

    @JsonProperty("fusion_status")
    private String fusionStatus;

    @JsonProperty("face_similarity")
    private Float faceSimilarity;

    @JsonProperty("body_similarity")
    private Float bodySimilarity;

    private List<Integer> bbox;

    @JsonProperty("body_warning")
    private Boolean bodyWarning;

    private String message;

    @JsonProperty("snapshot_base64")
    private String snapshotBase64;

    public MlFusionVideoTimelineItem() {
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
}
