package com.monty.backend;

import com.monty.backend.DTO.OtpEvent;
import com.monty.backend.Service.EmailService;
import com.monty.backend.Service.RabbitMqService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RabbitMqServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private RabbitMqService rabbitMqService;

    private OtpEvent otpEvent;

    @BeforeEach
    void setUp() {
        otpEvent = new OtpEvent("john.doe@example.com", "123456", "John Doe");
    }

    @Test
    void sendOtpNotification_Success() {
        // Arrange
        doNothing().when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(OtpEvent.class));

        // Act
        assertDoesNotThrow(() -> rabbitMqService.sendOtpNotification(otpEvent));

        // Assert
        verify(rabbitTemplate).convertAndSend(
                RabbitMqService.OTP_EXCHANGE,
                RabbitMqService.OTP_ROUTING_KEY,
                otpEvent
        );
    }

    @Test
    void sendOtpNotification_ThrowsException() {
        // Arrange
        doThrow(new RuntimeException("RabbitMQ connection failed"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(OtpEvent.class));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> rabbitMqService.sendOtpNotification(otpEvent));

        assertEquals("Failed to send OTP notification", exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals("RabbitMQ connection failed", exception.getCause().getMessage());

        verify(rabbitTemplate).convertAndSend(
                RabbitMqService.OTP_EXCHANGE,
                RabbitMqService.OTP_ROUTING_KEY,
                otpEvent
        );
    }

    @Test
    void consumeOtpMessage_EmailEnabled_SendsRealEmail() {
        // Arrange
        ReflectionTestUtils.setField(rabbitMqService, "emailEnabled", true);
        doNothing().when(emailService).sendOtpEmail(otpEvent);

        // Act
        assertDoesNotThrow(() -> rabbitMqService.consumeOtpMessage(otpEvent));

        // Assert
        verify(emailService).sendOtpEmail(otpEvent);
    }

    @Test
    void consumeOtpMessage_EmailDisabled_SimulatesEmail() {
        // Arrange
        ReflectionTestUtils.setField(rabbitMqService, "emailEnabled", false);

        // Act
        assertDoesNotThrow(() -> rabbitMqService.consumeOtpMessage(otpEvent));

        // Assert
        verify(emailService, never()).sendOtpEmail(any(OtpEvent.class));
        // Note: We can't easily verify the simulation logic without exposing it,
        // but we can verify that no exception is thrown and no real email is sent
    }

    @Test
    void consumeOtpMessage_EmailServiceThrowsException_HandlesGracefully() {
        // Arrange
        ReflectionTestUtils.setField(rabbitMqService, "emailEnabled", true);
        doThrow(new RuntimeException("Email service failed"))
                .when(emailService).sendOtpEmail(otpEvent);

        // Act & Assert
        assertDoesNotThrow(() -> rabbitMqService.consumeOtpMessage(otpEvent));

        verify(emailService).sendOtpEmail(otpEvent);
    }

    @Test
    void constants_AreCorrect() {
        // Assert
        assertEquals("otp.exchange", RabbitMqService.OTP_EXCHANGE);
        assertEquals("otp.queue", RabbitMqService.OTP_QUEUE);
        assertEquals("otp.send", RabbitMqService.OTP_ROUTING_KEY);
    }
}