package com.starter.springboot.services;

import com.starter.springboot.services.dto.OtpValidationResult;

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
     * @return boolean value (true|false)
     */
    Boolean generateOtp(String key, String userEmail);

    /**
     * Method for validating provided OTP
     *
     * @param key - provided key
     * @param otpNumber - provided OTP number
     * @return validation result
     */
    OtpValidationResult validateOTP(String key, Integer otpNumber);
}