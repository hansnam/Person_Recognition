package com.example.coreservice.dto;

import java.util.List;

public class SkeletonDto {

    private List<KeypointDto> keypoints;
    private SkeletonAnalysisDto analysis;

    public SkeletonDto() {
    }

    public SkeletonDto(List<KeypointDto> keypoints, SkeletonAnalysisDto analysis) {
        this.keypoints = keypoints;
        this.analysis = analysis;
    }

    public List<KeypointDto> getKeypoints() {
        return keypoints;
    }

    public void setKeypoints(List<KeypointDto> keypoints) {
        this.keypoints = keypoints;
    }

    public SkeletonAnalysisDto getAnalysis() {
        return analysis;
    }

    public void setAnalysis(SkeletonAnalysisDto analysis) {
        this.analysis = analysis;
    }
}

