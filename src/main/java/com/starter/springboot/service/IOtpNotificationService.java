package com.starter.springboot.service;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for OTP-related notifications.
 */
public interface IOtpNotificationService {

    /**
     * Sends a lockout notification email.
     * @param key the user key (e.g., username)
     * @param userEmail the user's email
     */
    void notifyLockout(String key, String userEmail);

    /**
     * Sends a delivery failure notification email.
     * @param userEmail the user's email
     * @param key the user key (e.g., username)
     */
    void notifyDeliveryFailure(String userEmail, String key);

    /**
     * Sends an OTP email asynchronously.
     * @param key the user key
     * @param userEmail the user's email
     * @param otpValue the OTP value
     * @return CompletableFuture indicating success
     */
    CompletableFuture<Boolean> sendOtpEmailAsync(String key, String userEmail, Integer otpValue);
}