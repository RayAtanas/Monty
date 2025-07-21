package com.monty.backend.Service;

import com.monty.backend.DTO.*;
import com.monty.backend.Model.*;
import com.monty.backend.Repository.*;
import com.monty.backend.Util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    private final IUserRepository userRepository;
    private final IOtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpUtil otpUtil;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RabbitMqService rabbitMqService;
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int OTP_EXPIRATION_MINUTES = 5;
    private static final String REDIS_OTP_PREFIX = "otp:";

    @Autowired
    public AuthService(IUserRepository userRepository, IOtpRepository otpRepository, PasswordEncoder passwordEncoder, OtpUtil otpUtil, JwtUtil jwtUtil, RedisTemplate<String, Object> redisTemplate, RabbitMqService rabbitMqService) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpUtil = otpUtil;
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
        this.rabbitMqService = rabbitMqService;
    }

    @Transactional
    public AuthResponse register(RegisterDTO request) {
        log.info("Attempting to register user with email: {}", request.getEmail());

        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: Email {} already exists", request.getEmail());
            throw new RuntimeException("Email already exists");
        }

        // Create new user
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setAge(request.getAge());
        user.setActive(false);

        User savedUser = userRepository.save(user);
        log.info("User created with ID: {}", savedUser.getId());

        // Generate and store OTP
        String otpCode = otpUtil.generateOtp();

        // Store in PostgreSQL
        Otp otp = new Otp();
        otp.setUserId(savedUser.getId());
        otp.setCode(otpCode);
        otp.setExpirationTime(LocalDateTime.now().plusMinutes(OTP_EXPIRATION_MINUTES));
        otp.setVerified(false);
        otpRepository.save(otp);

        // Store in Redis with TTL
        String redisKey = REDIS_OTP_PREFIX + savedUser.getEmail();
        redisTemplate.opsForValue().set(redisKey, otpCode, OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);

        // Send OTP via RabbitMQ
        OtpEvent otpEvent = new OtpEvent(savedUser.getEmail(), otpCode, savedUser.getName());
        rabbitMqService.sendOtpNotification(otpEvent);

        log.info("OTP generated and sent for user: {}", savedUser.getEmail());

        return new AuthResponse("User registered successfully. Please verify your OTP.");
    }

    @Transactional
    public AuthResponse verifyOtp(OtpVerificationDTO request) {
        log.info("Attempting to verify OTP for email: {}", request.getEmail());

        // Find user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check OTP from Redis first
        String redisKey = REDIS_OTP_PREFIX + request.getEmail();
        String storedOtp = (String) redisTemplate.opsForValue().get(redisKey);

        if (storedOtp == null || !storedOtp.equals(request.getOtpCode())) {
            log.warn("OTP verification failed for email: {}", request.getEmail());
            throw new RuntimeException("Invalid or expired OTP");
        }

        // Verify OTP in database and mark as verified
        Otp otp = otpRepository.findByUserIdAndCodeAndVerifiedFalse(user.getId(), request.getOtpCode())
                .orElseThrow(() -> new RuntimeException("Invalid OTP"));

        if (otp.getExpirationTime().isBefore(LocalDateTime.now())) {
            log.warn("OTP expired for email: {}", request.getEmail());
            throw new RuntimeException("OTP has expired");
        }

        // Activate user account
        user.setActive(true);
        userRepository.save(user);

        // Mark OTP as verified
        otp.setVerified(true);
        otpRepository.save(otp);

        // Remove OTP from Redis
        redisTemplate.delete(redisKey);

        log.info("OTP verified successfully for user: {}", request.getEmail());

        return new AuthResponse("OTP verified successfully. Account activated.");
    }

    public AuthResponse login(LoginDTO request) {
        log.info("Attempting login for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed: Invalid password for email: {}", request.getEmail());
            throw new RuntimeException("Invalid credentials");
        }

        if (!user.getActive()) {
            log.warn("Login failed: Account not activated for email: {}", request.getEmail());
            throw new RuntimeException("Account not activated. Please verify your OTP first.");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getId());
        UserDTO userDto = new UserDTO(user.getId(), user.getName(), user.getEmail(), user.getAge(), user.getActive());

        log.info("Login successful for user: {}", request.getEmail());

        return new AuthResponse(token, "Login successful", userDto);
    }

    public UserDTO getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return new UserDTO(user.getId(), user.getName(), user.getEmail(), user.getAge(), user.getActive());
    }
}