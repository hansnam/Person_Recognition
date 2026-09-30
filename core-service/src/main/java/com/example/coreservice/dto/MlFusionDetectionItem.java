package com.example.coreservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class MlFusionDetectionItem {

    @JsonProperty("detection_id")
    private Integer detectionId;

    private List<Integer> bbox;
    private MlFusionComponentResult face;
    private MlFusionComponentResult body;
    private MlFusionResult fusion;

    public MlFusionDetectionItem() {
    }

    public Integer getDetectionId() {
        return detectionId;
    }

    public void setDetectionId(Integer detectionId) {
        this.detectionId = detectionId;
    }

    public List<Integer> getBbox() {
        return bbox;
    }

    public void setBbox(List<Integer> bbox) {
        this.bbox = bbox;
    }

    public MlFusionComponentResult getFace() {
        return face;
    }

    public void setFace(MlFusionComponentResult face) {
        this.face = face;
    }

    public MlFusionComponentResult getBody() {
        return body;
    }

    public void setBody(MlFusionComponentResult body) {
        this.body = body;
    }

    public MlFusionResult getFusion() {
        return fusion;
    }

    public void setFusion(MlFusionResult fusion) {
        this.fusion = fusion;
    }
}
