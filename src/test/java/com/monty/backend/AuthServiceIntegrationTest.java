package com.monty.backend;

import com.monty.backend.DTO.*;
import com.monty.backend.Model.*;
import com.monty.backend.Repository.*;
import com.monty.backend.Service.AuthService;
import com.monty.backend.Service.RabbitMqService;
import com.monty.backend.Util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Integration test for AuthService that tests with actual Spring context
 * Uses H2 in-memory database for testing as configured in your pom.xml
 */
@SpringBootTest(
        properties = {
                "spring.datasource.url=jdbc:h2:mem:testdb",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "email.enabled=false"
        }
)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "email.enabled=false"
})
@Transactional
@EnableAutoConfiguration(exclude = {
        RedisAutoConfiguration.class,
        RedisRepositoriesAutoConfiguration.class
})
public class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private IOtpRepository otpRepository;

    @MockBean
    private OtpUtil otpUtil;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockBean
    private RabbitMqService rabbitMqService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private RegisterDTO registerDTO;

    @BeforeEach
    public void setUp() {
        registerDTO = new RegisterDTO();
        registerDTO.setName("John Doe");
        registerDTO.setEmail("john.doe@example.com");
        registerDTO.setPassword("password123");
        registerDTO.setAge(25);

        // Clean up database before each test
        otpRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void register_Integration_Success() {
        // Arrange
        when(otpUtil.generateOtp()).thenReturn("123456");
        when(redisTemplate.opsForValue()).thenReturn(mock(org.springframework.data.redis.core.ValueOperations.class));
        doNothing().when(rabbitMqService).sendOtpNotification(any(OtpEvent.class));

        // Act
        AuthResponse response = authService.register(registerDTO);

        // Assert
        assertNotNull(response);
        assertEquals("User registered successfully. Please verify your OTP.", response.getMessage());

        // Verify user was created in database
        Optional<User> savedUser = userRepository.findByEmail(registerDTO.getEmail());
        assertTrue(savedUser.isPresent());
        assertEquals(registerDTO.getName(), savedUser.get().getName());
        assertEquals(registerDTO.getEmail(), savedUser.get().getEmail());
        assertFalse(savedUser.get().getActive()); // Should be inactive initially
        assertTrue(passwordEncoder.matches(registerDTO.getPassword(), savedUser.get().getPassword()));

        // Verify OTP was created in database
        Optional<Otp> savedOtp = otpRepository.findByUserIdAndCodeAndVerifiedFalse(savedUser.get().getId(), "123456");
        assertTrue(savedOtp.isPresent());
        assertEquals("123456", savedOtp.get().getCode());
        assertFalse(savedOtp.get().getVerified());
        assertTrue(savedOtp.get().getExpirationTime().isAfter(LocalDateTime.now()));

        verify(otpUtil).generateOtp();
        verify(rabbitMqService).sendOtpNotification(any(OtpEvent.class));
    }

    @Test
    public void register_Integration_EmailAlreadyExists() {
        // Arrange - Create a user first
        User existingUser = new User();
        existingUser.setName("Existing User");
        existingUser.setEmail(registerDTO.getEmail());
        existingUser.setPassword("encodedPassword");
        existingUser.setAge(30);
        existingUser.setActive(true);
        userRepository.save(existingUser);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.register(registerDTO));

        assertEquals("Email already exists", exception.getMessage());
    }

    @Test
    public void verifyOtp_Integration_Success() {
        // Arrange - Create user and OTP first
        User user = new User();
        user.setName("John Doe");
        user.setEmail("john.doe@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setAge(25);
        user.setActive(false);
        User savedUser = userRepository.save(user);

        Otp otp = new Otp();
        otp.setUserId(savedUser.getId());
        otp.setCode("123456");
        otp.setExpirationTime(LocalDateTime.now().plusMinutes(5));
        otp.setVerified(false);
        otpRepository.save(otp);

        // Mock Redis operations
        org.springframework.data.redis.core.ValueOperations<String, Object> valueOps =
                mock(org.springframework.data.redis.core.ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("otp:" + savedUser.getEmail())).thenReturn("123456");

        OtpVerificationDTO verificationDTO = new OtpVerificationDTO();
        verificationDTO.setEmail(savedUser.getEmail());
        verificationDTO.setOtpCode("123456");

        // Act
        AuthResponse response = authService.verifyOtp(verificationDTO);

        // Assert
        assertNotNull(response);
        assertEquals("OTP verified successfully. Account activated.", response.getMessage());

        // Verify user is now active
        Optional<User> updatedUser = userRepository.findByEmail(savedUser.getEmail());
        assertTrue(updatedUser.isPresent());
        assertTrue(updatedUser.get().getActive());

        // Verify OTP is marked as verified
        Optional<Otp> updatedOtp = otpRepository.findById(otp.getId());
        assertTrue(updatedOtp.isPresent());
        assertTrue(updatedOtp.get().getVerified());

        verify(redisTemplate).delete("otp:" + savedUser.getEmail());
    }

    @Test
    public void login_Integration_Success() {
        // Arrange - Create active user
        User user = new User();
        user.setName("John Doe");
        user.setEmail("john.doe@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setAge(25);
        user.setActive(true);
        User savedUser = userRepository.save(user);

        when(jwtUtil.generateToken(savedUser.getEmail(), savedUser.getId())).thenReturn("jwt-token");

        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setEmail(savedUser.getEmail());
        loginDTO.setPassword("password123");

        // Act
        AuthResponse response = authService.login(loginDTO);

        // Assert
        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("Login successful", response.getMessage());
        assertNotNull(response.getUser());
        assertEquals(savedUser.getEmail(), response.getUser().getEmail());
        assertEquals(savedUser.getName(), response.getUser().getName());

        verify(jwtUtil).generateToken(savedUser.getEmail(), savedUser.getId());
    }

    @Test
    public void login_Integration_AccountNotActivated() {
        // Arrange - Create inactive user
        User user = new User();
        user.setName("John Doe");
        user.setEmail("john.doe@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setAge(25);
        user.setActive(false); // Not activated
        userRepository.save(user);

        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setEmail(user.getEmail());
        loginDTO.setPassword("password123");

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.login(loginDTO));

        assertEquals("Account not activated. Please verify your OTP first.", exception.getMessage());
    }

    @Test
    public void getCurrentUser_Integration_Success() {
        // Arrange - Create user
        User user = new User();
        user.setName("John Doe");
        user.setEmail("john.doe@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setAge(25);
        user.setActive(true);
        User savedUser = userRepository.save(user);

        // Act
        UserDTO result = authService.getCurrentUser(savedUser.getEmail());

        // Assert
        assertNotNull(result);
        assertEquals(savedUser.getId(), result.getId());
        assertEquals(savedUser.getName(), result.getName());
        assertEquals(savedUser.getEmail(), result.getEmail());
        assertEquals(savedUser.getAge(), result.getAge());
        assertEquals(savedUser.getActive(), result.getActive());
    }

    @Test
    public void fullRegistrationFlow_Integration_Success() {
        // Arrange
        when(otpUtil.generateOtp()).thenReturn("123456");
        when(jwtUtil.generateToken(anyString(), any(Long.class))).thenReturn("jwt-token");

        org.springframework.data.redis.core.ValueOperations<String, Object> valueOps =
                mock(org.springframework.data.redis.core.ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("otp:" + registerDTO.getEmail())).thenReturn("123456");

        doNothing().when(rabbitMqService).sendOtpNotification(any(OtpEvent.class));

        // Act & Assert - Step 1: Register
        AuthResponse registerResponse = authService.register(registerDTO);
        assertNotNull(registerResponse);
        assertEquals("User registered successfully. Please verify your OTP.", registerResponse.getMessage());

        // Act & Assert - Step 2: Verify OTP
        OtpVerificationDTO verificationDTO = new OtpVerificationDTO();
        verificationDTO.setEmail(registerDTO.getEmail());
        verificationDTO.setOtpCode("123456");

        AuthResponse verifyResponse = authService.verifyOtp(verificationDTO);
        assertNotNull(verifyResponse);
        assertEquals("OTP verified successfully. Account activated.", verifyResponse.getMessage());

        // Act & Assert - Step 3: Login
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setEmail(registerDTO.getEmail());
        loginDTO.setPassword(registerDTO.getPassword());

        AuthResponse loginResponse = authService.login(loginDTO);
        assertNotNull(loginResponse);
        assertEquals("jwt-token", loginResponse.getToken());
        assertEquals("Login successful", loginResponse.getMessage());
        assertNotNull(loginResponse.getUser());
        assertEquals(registerDTO.getEmail(), loginResponse.getUser().getEmail());

        // Verify final state in database
        Optional<User> finalUser = userRepository.findByEmail(registerDTO.getEmail());
        assertTrue(finalUser.isPresent());
        assertTrue(finalUser.get().getActive());
    }
}