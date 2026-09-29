package com.example.coreservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class MlVideoTimelineItem {

    private Double timestamp;

    @JsonProperty("timestamp_formatted")
    private String timestampFormatted;

    @JsonProperty("frame_index")
    private Integer frameIndex;

    @JsonProperty("vector_id")
    private Long vectorId;

    private Float similarity;
    private List<Integer> bbox;

    @JsonProperty("snapshot_base64")
    private String snapshotBase64;

    public MlVideoTimelineItem() {
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

    public String getSnapshotBase64() {
        return snapshotBase64;
    }

    public void setSnapshotBase64(String snapshotBase64) {
        this.snapshotBase64 = snapshotBase64;
    }
}
