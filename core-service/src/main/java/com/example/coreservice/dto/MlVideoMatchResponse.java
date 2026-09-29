package com.example.coreservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class MlVideoMatchResponse {

    @JsonProperty("duration_seconds")
    private Double durationSeconds;

    @JsonProperty("total_frames")
    private Integer totalFrames;

    @JsonProperty("processed_frames")
    private Integer processedFrames;

    @JsonProperty("total_faces_detected")
    private Integer totalFacesDetected;

    private Boolean matched;

    @JsonProperty("total_matches")
    private Integer totalMatches;

    @JsonProperty("unique_persons")
    private List<MlVideoPersonSummary> uniquePersons;

    private List<MlVideoTimelineItem> timeline;

    private String message;

    public MlVideoMatchResponse() {
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

    public List<MlVideoPersonSummary> getUniquePersons() {
        return uniquePersons;
    }

    public void setUniquePersons(List<MlVideoPersonSummary> uniquePersons) {
        this.uniquePersons = uniquePersons;
    }

    public List<MlVideoTimelineItem> getTimeline() {
        return timeline;
    }

    public void setTimeline(List<MlVideoTimelineItem> timeline) {
        this.timeline = timeline;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
