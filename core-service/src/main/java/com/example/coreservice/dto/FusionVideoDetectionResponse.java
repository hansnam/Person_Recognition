package com.example.coreservice.dto;

import java.util.List;

public class FusionVideoDetectionResponse {

    private Double durationSeconds;
    private Integer totalFrames;
    private Integer processedFrames;
    private Integer totalDetections;
    private Boolean matched;
    private Integer totalMatches;
    private List<FusionVideoPersonSummaryDto> uniquePersons;
    private List<FusionVideoTimelineItemDto> timeline;
    private String videoUrl;
    private String message;

    public FusionVideoDetectionResponse() {
    }

    public Double getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Double durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Integer getTotalFrames() {
        return totalFrames;
    }

    public void setTotalFrames(Integer totalFrames) {
        this.totalFrames = totalFrames;
    }

    public Integer getProcessedFrames() {
        return processedFrames;
    }

    public void setProcessedFrames(Integer processedFrames) {
        this.processedFrames = processedFrames;
    }

    public Integer getTotalDetections() {
        return totalDetections;
    }

    public void setTotalDetections(Integer totalDetections) {
        this.totalDetections = totalDetections;
    }

    public Boolean getMatched() {
        return matched;
    }

    public void setMatched(Boolean matched) {
        this.matched = matched;
    }

    public Integer getTotalMatches() {
        return totalMatches;
    }

    public void setTotalMatches(Integer totalMatches) {
        this.totalMatches = totalMatches;
    }

    public List<FusionVideoPersonSummaryDto> getUniquePersons() {
        return uniquePersons;
    }

    public void setUniquePersons(List<FusionVideoPersonSummaryDto> uniquePersons) {
        this.uniquePersons = uniquePersons;
    }

    public List<FusionVideoTimelineItemDto> getTimeline() {
        return timeline;
    }

    public void setTimeline(List<FusionVideoTimelineItemDto> timeline) {
        this.timeline = timeline;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
