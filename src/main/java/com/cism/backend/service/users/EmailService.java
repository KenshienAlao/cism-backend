package com.cism.backend.service.users;

import com.cism.backend.dto.users.OtpDto;
import com.cism.backend.exception.BadrequestException;

import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public OtpDto sendOtpEmail(String to, String otp) throws Exception {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("OTP Verification");
            helper.setText("<p>Your verification code is: <strong>" + otp + "</strong></p>", true);

            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
            throw new BadrequestException("Failed to send OTP: " + e.getMessage(), "OTP_FAILED");
        }

        return new OtpDto(to, otp);
    }
}
