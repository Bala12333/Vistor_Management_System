package com.vms.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.password:mock_password}")
    private String mailPassword;

    @Value("${app.email.from:noreply@vms.com}")
    private String fromEmail;

    public void sendEmail(String toEmail, String subject, String body) {
        if ("mock_password".equals(mailPassword)) {
            log.info("Email is disabled (mock mode). Simulating email to {}: Subject={}, Body={}", toEmail, subject, body);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            
            mailSender.send(message);
            log.info("Email sent successfully to {}", toEmail);
        } catch (Exception ex) {
            log.error("Failed to send email to {}", toEmail, ex);
        }
    }
}
