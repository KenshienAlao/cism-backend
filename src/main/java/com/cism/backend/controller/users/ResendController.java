package com.cism.backend.controller.users;

import java.util.Random;


import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cism.backend.dto.common.Api;
import com.cism.backend.dto.users.OtpDto;
import com.cism.backend.exception.BadrequestException;
import com.cism.backend.repository.users.RegisterRepository;
import com.cism.backend.service.users.EmailService;
import com.cism.backend.service.users.EmailValidationService;
import com.cism.backend.service.users.OtpService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/resend")
public class ResendController {

    private final RegisterRepository registerRepository;
    private final EmailService emailService;
    private final OtpService otpService;
    private final EmailValidationService emailValidationService;

    public ResendController(EmailService emailService, OtpService otpService, EmailValidationService emailValidationService, RegisterRepository registerRepository) {
        this.emailService = emailService;
        this.otpService = otpService;
        this.emailValidationService = emailValidationService;
        this.registerRepository = registerRepository;
    }

    @PostMapping("/send-otp")
    public ResponseEntity<Api<OtpDto>> sendOtp(@RequestBody OtpDto entity, HttpServletRequest request) throws Exception {
        String email = entity.email();
        String ipAddress = request.getRemoteAddr();

        if (email == null || email.trim().isEmpty()) {
            throw new BadrequestException("Email address is required.", "EMAIL_REQUIRED");
        }

        if (registerRepository.findByEmail(email).isPresent()) {
            throw new BadrequestException("Email address already exists.", "EMAIL_EXIST");
        }

        if (!emailValidationService.validateEmail(email, ipAddress)) {
            throw new BadrequestException("Email address doesn't exist or is invalid.", "INVALID_EMAIL");
        }

        String otp = String.format("%06d", new Random().nextInt(1000000));

        otpService.storeOtp(email, otp, ipAddress);

        OtpDto success = emailService.sendOtpEmail(email, otp);
        
        return ResponseEntity.ok(Api.ok("OTP sent successfully", "OTP_SENT", success));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity <Api<OtpDto>> verifyOtp(@RequestBody OtpDto entity) throws Exception {
        String email = entity.email();
        String otp = entity.otp();
        OtpDto success = otpService.verifyOtp(email, otp);
        return ResponseEntity.ok(Api.ok("OTP verified successfully", "OTP_VERIFIED", success));
    }

}
