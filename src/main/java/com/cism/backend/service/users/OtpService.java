package com.cism.backend.service.users;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cism.backend.dto.users.OtpDto;
import com.cism.backend.exception.BadrequestException;
import com.cism.backend.model.users.OtpModel;
import com.cism.backend.repository.users.OtpRepository;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
public class OtpService {

    private static final int COOLDOW_MINUTES = 5;

    private final OtpRepository otpRepository;

    public OtpService(OtpRepository otpRepository) {
        this.otpRepository = otpRepository;
    }

    
    @Transactional
    public OtpDto storeOtp(String email, String otp, String ipAddress) {

        Instant fiveMinAgo = Instant.now().minus(Duration.ofMinutes(COOLDOW_MINUTES));


        long requestCount = otpRepository.countByIpAddressAndCreateAtAfter(ipAddress, fiveMinAgo);

        if (requestCount >= 5) { // people
            throw new BadrequestException("Too many requests for this network, please try again later", "TOO_MANY_REQUESTS");
        }


        Optional<OtpModel> existing = otpRepository.findByEmail(email);
        if (existing.isPresent()) {
            Instant created = existing.get().getCreateAt();
            Instant expiryTime = created.plus(Duration.ofMinutes(COOLDOW_MINUTES));

            if (expiryTime.isAfter(Instant.now())) {
                long remainingSeconds = Duration.between(Instant.now(), expiryTime).getSeconds();
                throw new BadrequestException(
                    "You already have an Valid OTP, please wait " + ((remainingSeconds / 60) + 1) + " minutes before requesting a new OTP", 
                    "OTP_ALREADY_SENT",
                    Map.of("retryAfterSeconds", remainingSeconds)
                );
            }
            otpRepository.delete(existing.get());
            otpRepository.flush();
        }

        OtpModel newEntry = OtpModel.builder()
            .email(email)
            .otp(otp)
            .ipAddress(ipAddress)
            .createAt(Instant.now())
            .build();

            otpRepository.save(newEntry);
            return new OtpDto(email, otp);
        }


    @Transactional
    public OtpDto verifyOtp(String email, String codeFromUser) throws Exception {
        System.out.println("Attempting to verify OTP. Provided Email: '" + email + "', Provided Code: '" + codeFromUser + "'");
        OtpModel stored = otpRepository.findByEmail(email).orElseThrow(() -> {
            System.out.println("Verification Failed: OTP_NOT_FOUND for email " + email);
            return new BadrequestException("OTP not found", "OTP_NOT_FOUND");
        });

        if (stored.getCreateAt().plus(Duration.ofMinutes(COOLDOW_MINUTES)).isBefore(Instant.now())) {
            otpRepository.delete(stored);
            otpRepository.flush();
            System.out.println("Verification Failed: OTP_EXPIRED for email " + email);
            throw new BadrequestException("OTP expired", "OTP_EXPIRED");
        }

        System.out.println("Stored Code: '" + stored.getOtp() + "'");
        if (!stored.getOtp().equals(codeFromUser)) {
            System.out.println("Verification Failed: OTP_NOT_MATCH. Expected " + stored.getOtp() + ", got " + codeFromUser);
            throw new BadrequestException("OTP not match", "OTP_NOT_MATCH");
        }
        
        System.out.println("OTP Verified Successfully! Deleting OTP from db...");
        otpRepository.delete(stored);
        otpRepository.flush();
        return new OtpDto(email, codeFromUser);
    }

    // Runs every 5 minutes — deletes all OTPs that are older than 5 minutes (expired)
    @Scheduled(fixedRate = 5 * 60 * 1000)
    @Transactional
    public void cleanupExpiredOtps() {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(COOLDOW_MINUTES));
        otpRepository.deleteByCreateAtBefore(cutoff);
        log.info("Scheduled OTP cleanup complete — removed entries older than {} minutes.", COOLDOW_MINUTES);
    }

    // Runs on server startup — clears ALL pending OTPs so users must re-request after a restart
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void clearAllOtpsOnStartup() {
        otpRepository.deleteAll();
        log.info("Server started: all pending OTPs have been invalidated.");
    }

}
