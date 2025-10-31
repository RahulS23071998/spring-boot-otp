package com.starter.springboot.services;

import com.starter.springboot.services.dto.OtpValidationResult;

/**
 * Interface for OTP generation and validation operations.
 * Provides contract for generating OTPs and validating OTP status.
 */
public interface IOtpGenerator {

    /**
     * Generate a new OTP for the given key
     *
     * @param key - the key to generate OTP for (e.g., username)
     * @return the generated OTP value, or -1 if generation failed
     */
    Integer generateOTP(String key);

    /**
     * Validate OTP status for the given key and OTP number
     *
     * @param key - the key to validate OTP for
     * @param otpNumber - the OTP number to validate
     * @return validation result with status
     */
    OtpValidationResult validateOtpStatus(String key, int otpNumber);

    /**
     * Clear OTP from cache
     *
     * @param key - the key to clear OTP for
     */
    void clearOTPFromCache(String key);
}