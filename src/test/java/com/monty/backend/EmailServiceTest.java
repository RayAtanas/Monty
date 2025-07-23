package com.monty.backend;

import com.monty.backend.DTO.OtpEvent;
import com.monty.backend.Service.EmailService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    private OtpEvent otpEvent;

    @BeforeEach
    public void setUp() {
        otpEvent = new OtpEvent("john.doe@example.com", "123456", "John Doe");
    }

    @Test
    public void sendOtpEmail_Success() throws MessagingException {
        // Arrange
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // Act
        assertDoesNotThrow(() -> emailService.sendOtpEmail(otpEvent));

        // Assert
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    public void sendOtpEmail_SendThrowsRuntimeException_PropagatesException() {
        // Arrange
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("Failed to send email"))
                .when(mailSender).send(any(MimeMessage.class));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> emailService.sendOtpEmail(otpEvent));

        assertEquals("Failed to send email", exception.getMessage());

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    public void sendOtpEmail_CreateMimeMessageThrowsException_ThrowsRuntimeException() {
        // Arrange
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Failed to create message"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> emailService.sendOtpEmail(otpEvent));

        assertEquals("Failed to create message", exception.getMessage());

        verify(mailSender).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    public void sendOtpEmail_WithNullOtpEvent_ThrowsException() {
        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> emailService.sendOtpEmail(null));
    }


    @Test
    public void sendOtpEmail_WithEmptyEmail_ThrowsRuntimeException() {
        // Arrange
        OtpEvent emptyEmailEvent = new OtpEvent("", "123456", "John Doe");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act & Assert — now expect the RuntimeException your service throws
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> emailService.sendOtpEmail(emptyEmailEvent));

        assertEquals("Failed to send OTP email", ex.getMessage());
        verify(mailSender).createMimeMessage();
    }

    @Test
    public void sendOtpEmail_WithNullUserName_StillProcesses() throws MessagingException {
        // Arrange
        OtpEvent nullNameEvent = new OtpEvent("john.doe@example.com", "123456", null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // Act
        assertDoesNotThrow(() -> emailService.sendOtpEmail(nullNameEvent));

        // Assert
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    public void sendOtpEmail_WithNullOtpCode_StillProcesses() throws MessagingException {
        // Arrange
        OtpEvent nullOtpEvent = new OtpEvent("john.doe@example.com", null, "John Doe");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // Act
        assertDoesNotThrow(() -> emailService.sendOtpEmail(nullOtpEvent));

        // Assert
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }
}