package com.cism.backend.service.users;

import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final int MAX_RETRIES = 3;

    @Async
    public void sendOtpEmail(String to, String otp) {
        log.info("Sending OTP email to {} in a separate thread...", to);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(fromEmail);
                helper.setTo(to);
                helper.setSubject("OTP Verification");
                helper.setText("<p>Your verification code is: <strong>" + otp + "</strong></p>", true);

                mailSender.send(message);
                log.info("OTP email successfully sent to {} (attempt {})", to, attempt);
                return; // success — exit immediately
            } catch (Exception e) {
                log.warn("Attempt {}/{} failed to send OTP email to {}: {}", attempt, MAX_RETRIES, to, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try {
                        long backoff = attempt * 5000L; // 5s, 10s
                        log.info("Retrying in {}ms...", backoff);
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Retry interrupted for email to {}", to);
                        return;
                    }
                } else {
                    log.error("CRITICAL: All {} attempts failed to send OTP email to {}: {}", MAX_RETRIES, to, e.getMessage());
                }
            }
        }
    }
}

