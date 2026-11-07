package com.example.coreservice.dto;

public class KeypointDto {

    private Integer id;
    private String name;
    private Double x;
    private Double y;
    private Double conf;

    public KeypointDto() {
    }

    public KeypointDto(Integer id, String name, Double x, Double y, Double conf) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.conf = conf;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }

    public Double getConf() {
        return conf;
    }

    public void setConf(Double conf) {
        this.conf = conf;
    }
}
