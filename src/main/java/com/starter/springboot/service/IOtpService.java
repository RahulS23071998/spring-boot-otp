package com.starter.springboot.service;

import com.starter.springboot.dto.OtpGenerationResult;
import com.starter.springboot.dto.OtpValidationResult;

/**
 * Interface for OTP service operations.
 * Provides contract for OTP generation and validation functionality.
 */
public interface IOtpService {

    /**
     * Method for generate OTP number
     *
     * @param key - provided key (username in this case)
     * @param userEmail - user's email address
     * @return OtpGenerationResult containing success status and message
     */
    OtpGenerationResult generateOtp(String key, String userEmail);

    /**
     * Method for validating provided OTP
     *
     * @param key - provided key
     * @param otpNumber - provided OTP number
     * @return validation result
     */
    OtpValidationResult validateOTP(String key, Integer otpNumber);
}