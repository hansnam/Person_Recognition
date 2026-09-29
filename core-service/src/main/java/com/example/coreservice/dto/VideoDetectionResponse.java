package com.example.coreservice.dto;

import java.util.List;

public class VideoDetectionResponse {

    private String videoUrl;
    private Double durationSeconds;
    private Integer totalFrames;
    private Integer processedFrames;
    private Integer totalFacesDetected;
    private Boolean matched;
    private Integer totalMatches;
    private List<VideoPersonSummaryDto> uniquePersons;
    private List<VideoTimelineItemDto> timeline;
    private String message;

    public VideoDetectionResponse() {
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
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

    public Integer getTotalFacesDetected() {
        return totalFacesDetected;
    }

    public void setTotalFacesDetected(Integer totalFacesDetected) {
        this.totalFacesDetected = totalFacesDetected;
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

    public List<VideoPersonSummaryDto> getUniquePersons() {
        return uniquePersons;
    }

    public void setUniquePersons(List<VideoPersonSummaryDto> uniquePersons) {
        this.uniquePersons = uniquePersons;
    }

    public List<VideoTimelineItemDto> getTimeline() {
        return timeline;
    }

    public void setTimeline(List<VideoTimelineItemDto> timeline) {
        this.timeline = timeline;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
