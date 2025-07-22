package com.monty.backend.Service;

import com.monty.backend.DTO.OtpEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtpEmail(OtpEvent otpEvent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(otpEvent.getEmail());
            helper.setSubject("Your OTP Code - Monty Mobile");
            helper.setText(buildEmailContent(otpEvent), true);

            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", otpEvent.getEmail());
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to: {}", otpEvent.getEmail(), e);
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }

    private String buildEmailContent(OtpEvent otpEvent) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #007bff; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .otp-code { font-size: 24px; font-weight: bold; color: #007bff; text-align: center; 
                               background-color: white; padding: 15px; border-radius: 5px; margin: 20px 0; }
                    .footer { padding: 20px; text-align: center; color: #666; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Monty Mobile - OTP Verification</h1>
                    </div>
                    <div class="content">
                        <h2>Hello %s!</h2>
                        <p>Thank you for registering with Monty Mobile. To complete your registration, 
                           please use the following One-Time Password (OTP):</p>
                        
                        <div class="otp-code">%s</div>
                        
                        <p><strong>Important:</strong></p>
                        <ul>
                            <li>This code will expire in 5 minutes</li>
                            <li>Do not share this code with anyone</li>
                            <li>If you didn't request this code, please ignore this email</li>
                        </ul>
                    </div>
                    <div class="footer">
                        <p>This is an automated message from Monty Mobile.<br>
                           If you have any questions, please contact our support team.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(otpEvent.getUserName(), otpEvent.getOtpCode());
    }
}