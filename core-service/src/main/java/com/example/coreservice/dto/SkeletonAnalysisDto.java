package com.example.coreservice.dto;

import java.util.Map;

public class SkeletonAnalysisDto {

    private Boolean valid;
    private String orientation;
    private Map<String, Object> occlusion;
    private Map<String, Object> measurements;
    private Map<String, Object> anthropometricRatios;

    public SkeletonAnalysisDto() {
    }

    public Boolean getValid() {
        return valid;
    }

    public void setValid(Boolean valid) {
        this.valid = valid;
    }

    public String getOrientation() {
        return orientation;
    }

    public void setOrientation(String orientation) {
        this.orientation = orientation;
    }

    public Map<String, Object> getOcclusion() {
        return occlusion;
    }

    public void setOcclusion(Map<String, Object> occlusion) {
        this.occlusion = occlusion;
    }

    public Map<String, Object> getMeasurements() {
        return measurements;
    }

    public void setMeasurements(Map<String, Object> measurements) {
        this.measurements = measurements;
    }

    public Map<String, Object> getAnthropometricRatios() {
        return anthropometricRatios;
    }

    public void setAnthropometricRatios(Map<String, Object> anthropometricRatios) {
        this.anthropometricRatios = anthropometricRatios;
    }
}
