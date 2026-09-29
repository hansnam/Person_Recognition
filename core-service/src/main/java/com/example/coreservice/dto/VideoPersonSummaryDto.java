package com.example.coreservice.dto;

import java.util.List;

public class VideoPersonSummaryDto {

    private NguoiMatTichResponse person;
    private Float maxSimilarity;
    private Integer occurrencesCount;
    private String firstSeen;
    private Double firstSeenSeconds;
    private String lastSeen;
    private Double lastSeenSeconds;
    private String bestSnapshotUrl;
    private List<String> timestamps;
    private Long logId;

    public VideoPersonSummaryDto() {
    }

    public NguoiMatTichResponse getPerson() {
        return person;
    }

    public void setPerson(NguoiMatTichResponse person) {
        this.person = person;
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

    public String getBestSnapshotUrl() {
        return bestSnapshotUrl;
    }

    public void setBestSnapshotUrl(String bestSnapshotUrl) {
        this.bestSnapshotUrl = bestSnapshotUrl;
    }

    public List<String> getTimestamps() {
        return timestamps;
    }

    public void setTimestamps(List<String> timestamps) {
        this.timestamps = timestamps;
    }

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }
}
