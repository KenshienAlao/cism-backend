package com.cism.backend.service.users;

import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendOtpEmail(String to, String otp) {
        log.info("Sending OTP email to {} in a separate thread...", to);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("OTP Verification");
            helper.setText("<p>Your verification code is: <strong>" + otp + "</strong></p>", true);

            mailSender.send(message);
            log.info("OTP email successfully sent to {}", to);
        } catch (Exception e) {
            log.error("CRITICAL: Failed to send OTP email to {}", to, e);
        }
    }
}
