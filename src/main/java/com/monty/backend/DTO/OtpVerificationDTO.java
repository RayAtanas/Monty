package com.monty.backend.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;


public class OtpVerificationDTO {
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "OTP code is required")
    private String otpCode;

    public @NotBlank(message = "Email is required") @Email(message = "Email should be valid") String getEmail() {
        return email;
    }

    public void setEmail(@NotBlank(message = "Email is required") @Email(message = "Email should be valid") String email) {
        this.email = email;
    }

    public @NotBlank(message = "OTP code is required") String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(@NotBlank(message = "OTP code is required") String otpCode) {
        this.otpCode = otpCode;
    }
}
