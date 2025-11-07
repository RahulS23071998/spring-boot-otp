package com.starter.springboot.service.impl;

import com.starter.springboot.constants.OtpConstants;

import com.starter.springboot.dto.OtpGenerationResult;
import com.starter.springboot.dto.OtpValidationResult;
import com.starter.springboot.dto.OtpValidationStatus;
import com.starter.springboot.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Description(value = OtpConstants.OTP_SERVICE_DESCRIPTION)
@Service
public class OtpService implements IOtpService {

    private final Logger LOGGER = LoggerFactory.getLogger(OtpService.class);

    private final IOtpGenerator otpGenerator;
    private final IOtpNotificationService notificationService;
    private final IOtpRateLimiter rateLimiter;
    private final IOtpAuditService auditService;

    public OtpService(IOtpGenerator otpGenerator,
                       IOtpNotificationService notificationService,
                       IOtpRateLimiter rateLimiter,
                       IOtpAuditService auditService) {
        this.otpGenerator = otpGenerator;
        this.notificationService = notificationService;
        this.rateLimiter = rateLimiter;
        this.auditService = auditService;
    }

    /**
     * Method for generate OTP number
     *
     * @param key - provided key (username in this case)
     * @param userEmail - user's email address
     * @return OtpGenerationResult containing success status and message
     */
    @Override
    public OtpGenerationResult generateOtp(String key, String userEmail) {
        if (Objects.isNull(userEmail) || userEmail.isBlank()) {
            return OtpGenerationResult.deliveryFailed(); // or add a new result type
        }

        // Check rate limit
        OtpGenerationResult rateLimitResult = rateLimiter.checkRateLimit(key);
        if (rateLimitResult != null) {
            return rateLimitResult;
        }

        // Check and increment attempts
        OtpGenerationResult attemptResult = rateLimiter.checkAndIncrementAttempts(key);
        if (attemptResult != null) {
            if (attemptResult.isMaxAttemptsExceeded()) {
                notificationService.notifyLockout(key, userEmail);
            }
            return attemptResult;
        }

        Integer otpValue = otpGenerator.generateOTP(key);
        if (otpValue == -1) {
            LOGGER.error("OTP generator returned error code for key: {}", key);
            notificationService.notifyDeliveryFailure(userEmail, key);
            return OtpGenerationResult.deliveryFailed();
        }

        LOGGER.debug("Generated OTP for key: {}", key);

        // Send OTP email asynchronously
        final String otpKey = key;
        final String recipientEmail = userEmail;
        CompletableFuture<Boolean> emailDispatch = notificationService.sendOtpEmailAsync(key, userEmail, otpValue);
        emailDispatch.thenAccept(emailSent -> {
            if (!Boolean.TRUE.equals(emailSent)) {
                LOGGER.error("Failed to send OTP email to: {}", recipientEmail);
                notificationService.notifyDeliveryFailure(recipientEmail, otpKey);
            } else {
                LOGGER.debug("OTP email sent successfully to: {}", recipientEmail);
            }
        }).exceptionally(ex -> {
            LOGGER.error("Error dispatching OTP email to: {}", userEmail, ex);
            notificationService.notifyDeliveryFailure(userEmail, key);
            return null;
        });

        // Persist audit entry
        auditService.persistAuditEntry(key);

        // Record rate limit timestamp
        rateLimiter.recordRateLimitTimestamp(key);

        return OtpGenerationResult.success();
    }



    /**
     * Method for validating provided OTP
     *
     * @param key - provided key
     * @param otpNumber - provided OTP number
     * @return validation result
     */
    @Override
    public OtpValidationResult validateOTP(String key, Integer otpNumber) {
        if (Objects.isNull(otpNumber)) {
            return OtpValidationResult.invalid();
        }
        OtpValidationResult result = otpGenerator.validateOtpStatus(key, otpNumber);
        if (result.getStatus() == OtpValidationStatus.SUCCESS) {
            // Reset attempts counter on successful validation
            rateLimiter.resetAttempts(key);
            return OtpValidationResult.success();
        }
        if (result.getStatus() == OtpValidationStatus.LOCKED) {
            return OtpValidationResult.locked();
        }
        return OtpValidationResult.invalid();
    }
}
