package com.monty.backend.Service;

import com.monty.backend.DTO.OtpEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service

public class RabbitMqService {

    private final RabbitTemplate rabbitTemplate;

    public static final String OTP_EXCHANGE = "otp.exchange";
    public static final String OTP_QUEUE = "otp.queue";
    public static final String OTP_ROUTING_KEY = "otp.send";
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    public RabbitMqService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Sends OTP notification to RabbitMQ queue
     */



    public void sendOtpNotification(OtpEvent otpEvent) {
        try {
            rabbitTemplate.convertAndSend(OTP_EXCHANGE, OTP_ROUTING_KEY, otpEvent);
            log.info("OTP notification sent to queue for email: {}", otpEvent.getEmail());
        } catch (Exception e) {
            log.error("Failed to send OTP notification to queue for email: {}", otpEvent.getEmail(), e);
            throw new RuntimeException("Failed to send OTP notification", e);
        }
    }

    /**
     * Consumes OTP messages from the queue and simulates email sending
     */
    @RabbitListener(queues = OTP_QUEUE)
    public void consumeOtpMessage(OtpEvent otpEvent) {
        try {
            log.info("Received OTP message for email: {}", otpEvent.getEmail());

            // Simulate email sending (in real application, integrate with email service)
            simulateEmailSending(otpEvent);

            log.info("OTP email sent successfully to: {}", otpEvent.getEmail());
        } catch (Exception e) {
            log.error("Failed to process OTP message for email: {}", otpEvent.getEmail(), e);
        }
    }

    /**
     * Simulates sending email with OTP
     */
    private void simulateEmailSending(OtpEvent otpEvent) {
        // In a real application, you would integrate with an email service like:
        // - SendGrid
        // - Amazon SES
        // - JavaMail API
        // - etc.

        log.info("=== EMAIL SIMULATION ===");
        log.info("To: {}", otpEvent.getEmail());
        log.info("Subject: Your OTP Code");
        log.info("Body: Hello {}, your OTP code is: {}", otpEvent.getUserName(), otpEvent.getOtpCode());
        log.info("This code will expire in 5 minutes.");
        log.info("========================");

        // Simulate email sending delay
        try {
            Thread.sleep(1000); // 1 second delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}