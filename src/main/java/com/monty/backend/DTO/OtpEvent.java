package com.monty.backend.DTO;

import lombok.AllArgsConstructor;



public class OtpEvent {
    private String email;
    private String otpCode;
    private String userName;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public OtpEvent(String email, String otpCode, String userName) {
        this.email = email;
        this.otpCode = otpCode;
        this.userName = userName;
    }
}