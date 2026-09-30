package com.example.coreservice.dto;

import java.util.List;

public class FusionVideoPersonSummaryDto {

    private Long personId;
    private String fusionStatus;
    private Float maxFaceSimilarity;
    private Float maxBodySimilarity;
    private Integer occurrencesCount;
    private String firstSeen;
    private Double firstSeenSeconds;
    private String lastSeen;
    private Double lastSeenSeconds;
    private String bestSnapshotBase64;
    private String bestSnapshotUrl;
    private List<String> timestamps;
    private NguoiMatTichResponse hoSo;

    public FusionVideoPersonSummaryDto() {
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

    public Float getMaxFaceSimilarity() {
        return maxFaceSimilarity;
    }

    public void setMaxFaceSimilarity(Float maxFaceSimilarity) {
        this.maxFaceSimilarity = maxFaceSimilarity;
    }

    public Float getMaxBodySimilarity() {
        return maxBodySimilarity;
    }

    public void setMaxBodySimilarity(Float maxBodySimilarity) {
        this.maxBodySimilarity = maxBodySimilarity;
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

    public NguoiMatTichResponse getHoSo() {
        return hoSo;
    }

    public void setHoSo(NguoiMatTichResponse hoSo) {
        this.hoSo = hoSo;
    }
}
