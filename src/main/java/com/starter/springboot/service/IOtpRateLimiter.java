package com.starter.springboot.service;

import com.starter.springboot.dto.OtpGenerationResult;

/**
 * Interface for OTP rate limiting and attempt management.
 */
public interface IOtpRateLimiter {

    /**
     * Checks and enforces rate limiting for OTP generation.
     * @param key the user key
     * @return OtpGenerationResult if rate limited, or null if allowed
     */
    OtpGenerationResult checkRateLimit(String key);

    /**
     * Increments attempt count and checks if max attempts exceeded.
     * @param key the user key
     * @return OtpGenerationResult if max attempts exceeded, or null if allowed
     */
    OtpGenerationResult checkAndIncrementAttempts(String key);

    /**
     * Records successful rate limit check (sets timestamp).
     * @param key the user key
     */
    void recordRateLimitTimestamp(String key);

    /**
     * Resets attempts on successful validation.
     * @param key the user key
     */
    void resetAttempts(String key);
}