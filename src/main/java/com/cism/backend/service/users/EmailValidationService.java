package com.cism.backend.service.users;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import com.cism.backend.exception.BadrequestException;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmailValidationService {

    private final String apiKey;
    private final RestTemplate restTemplate;

    // Per email+IP tracking — unique per person per email
    private final ConcurrentHashMap<String, Instant> bannedKeys = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Deque<Instant>> requestTimes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Deque<Instant>> banViolations = new ConcurrentHashMap<>();

    // Global IP tracking — limits total requests from one IP regardless of email
    // High enough for shared WiFi (10 people), low enough to stop bots (100s/min)
    private final ConcurrentHashMap<String, Instant> globalBanned = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Deque<Instant>> globalIpRequestTimes = new ConcurrentHashMap<>();

    private record ApiLayerResponse(
        @JsonProperty("format_valid") boolean formatValid,
        @JsonProperty("smtp_check") boolean smtpCheck
    ) {}

    public EmailValidationService(@Value("${apilayer.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        this.restTemplate = new RestTemplate();
    }

    private void enforceRateLimit(String email, String ipAddress) {
        String compositeKey = ipAddress + ":" + email;
        Instant now = Instant.now();
        Duration emailWindow = Duration.ofMinutes(10);
        Duration globalWindow = Duration.ofHours(1);

        // ── Layer 1: Global IP gate ──────────────────────────────────────────
        // 10 requests/hr per IP — blocks bots, allows up to 10 people on same WiFi
        Instant globalBanExpiry = globalBanned.get(ipAddress);
        if (globalBanExpiry != null && now.isBefore(globalBanExpiry)) {
            long remaining = Duration.between(now, globalBanExpiry).toMinutes();
            throw new BadrequestException(
                "Too many requests from this network. Try again in " + (remaining + 1) + " mins.", "GLOBAL_BANNED");
        }

        Deque<Instant> globalAttempts = globalIpRequestTimes.computeIfAbsent(ipAddress, k -> new ConcurrentLinkedDeque<>());
        globalAttempts.addLast(now);
        while (!globalAttempts.isEmpty() && globalAttempts.peekFirst().isBefore(now.minus(globalWindow))) {
            globalAttempts.pollFirst();
        }
        if (globalAttempts.size() > 10) {
            globalBanned.put(ipAddress, now.plus(globalWindow));
            globalIpRequestTimes.remove(ipAddress);
            throw new BadrequestException("Too many OTP requests from this network. Try again in 1 hour.", "GLOBAL_BANNED");
        }

        // ── Layer 2: Per email+IP gate ───────────────────────────────────────
        // 3 requests per 10 mins per specific email — with escalating 2-hour ban
        Instant banExpiry = bannedKeys.get(compositeKey);
        if (banExpiry != null) {
            if (now.isBefore(banExpiry)) {
                // Track violations while banned — escalate if they keep trying
                Deque<Instant> violations = banViolations.computeIfAbsent(compositeKey, k -> new ConcurrentLinkedDeque<>());
                violations.addLast(now);
                while (!violations.isEmpty() && violations.peekFirst().isBefore(now.minus(Duration.ofMinutes(1)))) {
                    violations.pollFirst();
                }
                if (violations.size() >= 2) {
                    bannedKeys.put(compositeKey, now.plus(Duration.ofHours(2)));
                    banViolations.remove(compositeKey);
                    log.warn("Escalated to 2-hour ban for key {}", compositeKey);
                    throw new BadrequestException("Persistent spam detected. You are now banned for 2 hours.", "ESCALATED_BAN");
                }
                long remaining = Duration.between(now, banExpiry).toMinutes();
                throw new BadrequestException(
                    "Too many attempts for this email. Try again in " + (remaining + 1) + " mins.", "RATE_LIMITED");
            }
            bannedKeys.remove(compositeKey);
            banViolations.remove(compositeKey);
        }

        Deque<Instant> attempts = requestTimes.computeIfAbsent(compositeKey, k -> new ConcurrentLinkedDeque<>());
        attempts.addLast(now);
        while (!attempts.isEmpty() && attempts.peekFirst().isBefore(now.minus(emailWindow))) {
            attempts.pollFirst();
        }
        if (attempts.size() > 3) {
            bannedKeys.put(compositeKey, now.plus(emailWindow));
            requestTimes.remove(compositeKey);
            throw new BadrequestException("Verification limit reached for this email. Try again in 10 minutes.", "RATE_LIMITED");
        }
    }

    public boolean validateEmail(String email, String ipAddress) {

        


        enforceRateLimit(email, ipAddress);
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("EMAIL_API key is missing. Skipping validation.");
            return true;
        }

        String url = String.format("https://apilayer.net/api/check?access_key=%s&email=%s", apiKey, email);

        try {
            ResponseEntity<ApiLayerResponse> responseEntity = restTemplate.getForEntity(url, ApiLayerResponse.class);
            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                ApiLayerResponse body = responseEntity.getBody();
                return body.formatValid() && body.smtpCheck();
            }
        } catch (Exception e) {
            log.error("API Layer email validation failed: {}", e.getMessage());
            return true;
        }

        return false;
    }
}
