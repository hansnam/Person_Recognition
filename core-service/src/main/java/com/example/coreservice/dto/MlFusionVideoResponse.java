package com.example.coreservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class MlFusionVideoResponse {

    @JsonProperty("duration_seconds")
    private Double durationSeconds;

    @JsonProperty("total_frames")
    private Integer totalFrames;

    @JsonProperty("processed_frames")
    private Integer processedFrames;

    @JsonProperty("total_detections")
    private Integer totalDetections;

    private Boolean matched;

    @JsonProperty("total_matches")
    private Integer totalMatches;

    @JsonProperty("unique_persons")
    private List<MlFusionVideoPersonSummary> uniquePersons;

    private List<MlFusionVideoTimelineItem> timeline;

    public MlFusionVideoResponse() {
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

    public List<MlFusionVideoPersonSummary> getUniquePersons() {
        return uniquePersons;
    }

    public void setUniquePersons(List<MlFusionVideoPersonSummary> uniquePersons) {
        this.uniquePersons = uniquePersons;
    }

    public List<MlFusionVideoTimelineItem> getTimeline() {
        return timeline;
    }

    public void setTimeline(List<MlFusionVideoTimelineItem> timeline) {
        this.timeline = timeline;
    }
}
