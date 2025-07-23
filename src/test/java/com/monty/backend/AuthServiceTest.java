package com.monty.backend;

import com.monty.backend.DTO.*;
import com.monty.backend.Model.*;
import com.monty.backend.Repository.*;
import com.monty.backend.Service.AuthService;
import com.monty.backend.Service.RabbitMqService;
import com.monty.backend.Util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private IUserRepository userRepository;

    @Mock
    private IOtpRepository otpRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OtpUtil otpUtil;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private RabbitMqService rabbitMqService;

    @InjectMocks
    private AuthService authService;

    private RegisterDTO registerDTO;
    private User user;
    private OtpVerificationDTO otpVerificationDTO;
    private LoginDTO loginDTO;

    @BeforeEach
    public void setUp() {
        registerDTO = new RegisterDTO();
        registerDTO.setName("John Doe");
        registerDTO.setEmail("john.doe@example.com");
        registerDTO.setPassword("password123");
        registerDTO.setAge(25);

        user = new User();
        user.setName("John Doe");
        user.setEmail("john.doe@example.com");
        user.setPassword("encodedPassword");
        user.setAge(25);
        user.setActive(false);

        otpVerificationDTO = new OtpVerificationDTO();
        otpVerificationDTO.setEmail("john.doe@example.com");
        otpVerificationDTO.setOtpCode("123456");

        loginDTO = new LoginDTO();
        loginDTO.setEmail("john.doe@example.com");
        loginDTO.setPassword("password123");
    }

    @Test
    public void register_Success() {
        // Arrange
        when(userRepository.existsByEmail(registerDTO.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerDTO.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(otpUtil.generateOtp()).thenReturn("123456");
        when(otpRepository.save(any(Otp.class))).thenReturn(new Otp());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        AuthResponse response = authService.register(registerDTO);

        // Assert
        assertNotNull(response);
        assertEquals("User registered successfully. Please verify your OTP.", response.getMessage());

        verify(userRepository).existsByEmail(registerDTO.getEmail());
        verify(passwordEncoder).encode(registerDTO.getPassword());
        verify(userRepository).save(any(User.class));
        verify(otpRepository).save(any(Otp.class));
        verify(valueOperations).set(eq("otp:" + registerDTO.getEmail()), eq("123456"), eq(5L), eq(TimeUnit.MINUTES)); // Fixed: use 5L instead of 5
        verify(rabbitMqService).sendOtpNotification(any(OtpEvent.class));
    }

    @Test
    public void register_EmailAlreadyExists_ThrowsException() {
        // Arrange
        when(userRepository.existsByEmail(registerDTO.getEmail())).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.register(registerDTO));

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository).existsByEmail(registerDTO.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void verifyOtp_Success() {
        // Arrange
        when(userRepository.findByEmail(otpVerificationDTO.getEmail())).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:" + otpVerificationDTO.getEmail())).thenReturn("123456");

        Otp otp = new Otp();
        otp.setUserId(1L);
        otp.setCode("123456");
        otp.setExpirationTime(LocalDateTime.now().plusMinutes(5));
        otp.setVerified(false);

        when(otpRepository.findByUserIdAndCodeAndVerifiedFalse(user.getId(), "123456")).thenReturn(Optional.of(otp)); // Fixed: use user.getId()
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(otpRepository.save(any(Otp.class))).thenReturn(otp);

        // Act
        AuthResponse response = authService.verifyOtp(otpVerificationDTO);

        // Assert
        assertNotNull(response);
        assertEquals("OTP verified successfully. Account activated.", response.getMessage());
        assertTrue(user.getActive());
        assertTrue(otp.getVerified());

        verify(userRepository).findByEmail(otpVerificationDTO.getEmail());
        verify(valueOperations).get("otp:" + otpVerificationDTO.getEmail());
        verify(otpRepository).findByUserIdAndCodeAndVerifiedFalse(user.getId(), "123456");
        verify(userRepository).save(user);
        verify(otpRepository).save(otp);
        verify(redisTemplate).delete("otp:" + otpVerificationDTO.getEmail());
    }

    @Test
    public void verifyOtp_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail(otpVerificationDTO.getEmail())).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.verifyOtp(otpVerificationDTO));

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findByEmail(otpVerificationDTO.getEmail());
    }

    @Test
    public void verifyOtp_InvalidOtp_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail(otpVerificationDTO.getEmail())).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:" + otpVerificationDTO.getEmail())).thenReturn("654321");

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.verifyOtp(otpVerificationDTO));

        assertEquals("Invalid or expired OTP", exception.getMessage());
        verify(userRepository).findByEmail(otpVerificationDTO.getEmail());
        verify(valueOperations).get("otp:" + otpVerificationDTO.getEmail());
    }

    @Test
    public void verifyOtp_ExpiredOtp_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail(otpVerificationDTO.getEmail())).thenReturn(Optional.of(user));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("otp:" + otpVerificationDTO.getEmail())).thenReturn("123456");

        Otp expiredOtp = new Otp();
        expiredOtp.setUserId(1L);
        expiredOtp.setCode("123456");
        expiredOtp.setExpirationTime(LocalDateTime.now().minusMinutes(1)); // Expired
        expiredOtp.setVerified(false);

        when(otpRepository.findByUserIdAndCodeAndVerifiedFalse(user.getId(), "123456")).thenReturn(Optional.of(expiredOtp)); // Fixed: use user.getId()

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.verifyOtp(otpVerificationDTO));

        assertEquals("OTP has expired", exception.getMessage());
    }

    @Test
    public void login_Success() {
        // Arrange
        user.setActive(true);
        when(userRepository.findByEmail(loginDTO.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(user.getEmail(), user.getId())).thenReturn("jwt-token");

        // Act
        AuthResponse response = authService.login(loginDTO);

        // Assert
        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("Login successful", response.getMessage());
        assertNotNull(response.getUser());
        assertEquals(user.getEmail(), response.getUser().getEmail());

        verify(userRepository).findByEmail(loginDTO.getEmail());
        verify(passwordEncoder).matches(loginDTO.getPassword(), user.getPassword());
        verify(jwtUtil).generateToken(user.getEmail(), user.getId());
    }

    @Test
    public void login_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail(loginDTO.getEmail())).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.login(loginDTO));

        assertEquals("Invalid credentials", exception.getMessage());
        verify(userRepository).findByEmail(loginDTO.getEmail());
    }

    @Test
    public void login_InvalidPassword_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail(loginDTO.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.login(loginDTO));

        assertEquals("Invalid credentials", exception.getMessage());
        verify(userRepository).findByEmail(loginDTO.getEmail());
        verify(passwordEncoder).matches(loginDTO.getPassword(), user.getPassword());
    }

    @Test
    public void login_AccountNotActivated_ThrowsException() {
        // Arrange
        user.setActive(false);
        when(userRepository.findByEmail(loginDTO.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.login(loginDTO));

        assertEquals("Account not activated. Please verify your OTP first.", exception.getMessage());
        verify(userRepository).findByEmail(loginDTO.getEmail());
        verify(passwordEncoder).matches(loginDTO.getPassword(), user.getPassword());
    }

    @Test
    public void getCurrentUser_Success() {
        // Arrange
        user.setActive(true);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        // Act
        UserDTO result = authService.getCurrentUser(user.getEmail());

        // Assert
        assertNotNull(result);
        assertEquals(user.getId(), result.getId());
        assertEquals(user.getName(), result.getName());
        assertEquals(user.getEmail(), result.getEmail());
        assertEquals(user.getAge(), result.getAge());
        assertEquals(user.getActive(), result.getActive());

        verify(userRepository).findByEmail(user.getEmail());
    }

    @Test
    public void getCurrentUser_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.getCurrentUser(user.getEmail()));

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findByEmail(user.getEmail());
    }
}