package com.example.coreservice.dto;

import java.util.List;

public class VideoTimelineItemDto {

    private Double timestamp;
    private String timestampFormatted;
    private Integer frameIndex;
    private NguoiMatTichResponse person;
    private Float similarity;
    private List<Integer> bbox;
    private String snapshotUrl;

    public VideoTimelineItemDto() {
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

    public NguoiMatTichResponse getPerson() {
        return person;
    }

    public void setPerson(NguoiMatTichResponse person) {
        this.person = person;
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

    public String getSnapshotUrl() {
        return snapshotUrl;
    }

    public void setSnapshotUrl(String snapshotUrl) {
        this.snapshotUrl = snapshotUrl;
    }
}
