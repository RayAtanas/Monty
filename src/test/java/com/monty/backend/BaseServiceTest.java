package com.monty.backend;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for integration tests that provides common configuration
 * Extend this class for integration tests that need Spring context
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public abstract class BaseServiceTest {

    @BeforeEach
    void baseSetUp() {
        // Common setup for all integration tests
        // Can be overridden in subclasses
    }

    /**
     * Helper method to create test user data
     */
    protected com.monty.backend.Model.User createTestUser(String name, String email, String password, Integer age, Boolean active) {
        com.monty.backend.Model.User user = new com.monty.backend.Model.User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);
        user.setAge(age);
        user.setActive(active);
        return user;
    }

    /**
     * Helper method to create test OTP data
     */
    protected com.monty.backend.Model.Otp createTestOtp(Long userId, String code, java.time.LocalDateTime expirationTime, Boolean verified) {
        com.monty.backend.Model.Otp otp = new com.monty.backend.Model.Otp();
        otp.setUserId(userId);
        otp.setCode(code);
        otp.setExpirationTime(expirationTime);
        otp.setVerified(verified);
        return otp;
    }

    /**
     * Helper method to create register DTO
     */
    protected com.monty.backend.DTO.RegisterDTO createRegisterDTO(String name, String email, String password, Integer age) {
        com.monty.backend.DTO.RegisterDTO dto = new com.monty.backend.DTO.RegisterDTO();
        dto.setName(name);
        dto.setEmail(email);
        dto.setPassword(password);
        dto.setAge(age);
        return dto;
    }

    /**
     * Helper method to create login DTO
     */
    protected com.monty.backend.DTO.LoginDTO createLoginDTO(String email, String password) {
        com.monty.backend.DTO.LoginDTO dto = new com.monty.backend.DTO.LoginDTO();
        dto.setEmail(email);
        dto.setPassword(password);
        return dto;
    }

    /**
     * Helper method to create OTP verification DTO
     */
    protected com.monty.backend.DTO.OtpVerificationDTO createOtpVerificationDTO(String email, String otpCode) {
        com.monty.backend.DTO.OtpVerificationDTO dto = new com.monty.backend.DTO.OtpVerificationDTO();
        dto.setEmail(email);
        dto.setOtpCode(otpCode);
        return dto;
    }

    /**
     * Helper method to create OTP event
     */
    protected com.monty.backend.DTO.OtpEvent createOtpEvent(String email, String otpCode, String userName) {
        return new com.monty.backend.DTO.OtpEvent(email, otpCode, userName);
    }
}