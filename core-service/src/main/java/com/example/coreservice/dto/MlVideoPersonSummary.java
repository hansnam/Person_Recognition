package com.example.coreservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class MlVideoPersonSummary {

    @JsonProperty("vector_id")
    private Long vectorId;

    @JsonProperty("max_similarity")
    private Float maxSimilarity;

    @JsonProperty("occurrences_count")
    private Integer occurrencesCount;

    @JsonProperty("first_seen")
    private String firstSeen;

    @JsonProperty("first_seen_seconds")
    private Double firstSeenSeconds;

    @JsonProperty("last_seen")
    private String lastSeen;

    @JsonProperty("last_seen_seconds")
    private Double lastSeenSeconds;

    @JsonProperty("best_snapshot_base64")
    private String bestSnapshotBase64;

    private List<String> timestamps;

    public MlVideoPersonSummary() {
    }

    public Long getVectorId() {
        return vectorId;
    }

    public void setVectorId(Long vectorId) {
        this.vectorId = vectorId;
    }

    public Float getMaxSimilarity() {
        return maxSimilarity;
    }

    public void setMaxSimilarity(Float maxSimilarity) {
        this.maxSimilarity = maxSimilarity;
    }

    public Integer getOccurrencesCount() {
        return occurrencesCount;
    }

    public void setOccurrencesCount(Integer occurrencesCount) {
        this.occurrencesCount = occurrencesCount;
    }

    public String getFirstSeen() {
        return firstSeen;
    }

    public void setFirstSeen(String firstSeen) {
        this.firstSeen = firstSeen;
    }

    public Double getFirstSeenSeconds() {
        return firstSeenSeconds;
    }

    public void setFirstSeenSeconds(Double firstSeenSeconds) {
        this.firstSeenSeconds = firstSeenSeconds;
    }

    public String getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(String lastSeen) {
        this.lastSeen = lastSeen;
    }

    public Double getLastSeenSeconds() {
        return lastSeenSeconds;
    }

    public void setLastSeenSeconds(Double lastSeenSeconds) {
        this.lastSeenSeconds = lastSeenSeconds;
    }

    public String getBestSnapshotBase64() {
        return bestSnapshotBase64;
    }

    public void setBestSnapshotBase64(String bestSnapshotBase64) {
        this.bestSnapshotBase64 = bestSnapshotBase64;
    }

    public List<String> getTimestamps() {
        return timestamps;
    }

    public void setTimestamps(List<String> timestamps) {
        this.timestamps = timestamps;
    }
}
