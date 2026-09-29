package com.example.coreservice.dto;

import com.example.coreservice.entity.VaiTro;

public class LoginResponse {

    private String token;
    private String type = "Bearer";
    private String email;
    private VaiTro vaiTro;

    public LoginResponse() {
    }

    public LoginResponse(String token, String email, VaiTro vaiTro) {
        this.token = token;
        this.type = "Bearer";
        this.email = email;
        this.vaiTro = vaiTro;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public VaiTro getVaiTro() {
        return vaiTro;
    }

    public void setVaiTro(VaiTro vaiTro) {
        this.vaiTro = vaiTro;
    }
}
