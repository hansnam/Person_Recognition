package com.example.coreservice.dto;

import java.util.List;

public class FusionDetectionItemDto {

    private Integer detectionId;
    private List<Integer> bbox;
    private FusionComponentResultDto face;
    private FusionComponentResultDto body;
    private FusionStatusDto fusion;
    private NguoiMatTichResponse hoSo;

    public FusionDetectionItemDto() {
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

    public FusionComponentResultDto getFace() {
        return face;
    }

    public void setFace(FusionComponentResultDto face) {
        this.face = face;
    }

    public FusionComponentResultDto getBody() {
        return body;
    }

    public void setBody(FusionComponentResultDto body) {
        this.body = body;
    }

    public FusionStatusDto getFusion() {
        return fusion;
    }

    public void setFusion(FusionStatusDto fusion) {
        this.fusion = fusion;
    }

    public NguoiMatTichResponse getHoSo() {
        return hoSo;
    }

    public void setHoSo(NguoiMatTichResponse hoSo) {
        this.hoSo = hoSo;
    }
}
