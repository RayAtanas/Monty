package com.monty.backend.Util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class OtpUtil {

    private static final String NUMBERS = "0123456789";
    private static final int OTP_LENGTH = 6;
    private final SecureRandom random = new SecureRandom();

    /**
     * Generates a 6-digit OTP
     * @return String representing the OTP
     */
    public String generateOtp() {
        StringBuilder otp = new StringBuilder();

        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(NUMBERS.charAt(random.nextInt(NUMBERS.length())));
        }

        return otp.toString();
    }

    /**
     * Validates if the OTP format is correct
     * @param otp the OTP to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidOtpFormat(String otp) {
        if (otp == null || otp.length() != OTP_LENGTH) {
            return false;
        }

        return otp.matches("\\d{6}");
    }
}