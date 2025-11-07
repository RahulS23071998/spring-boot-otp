package com.starter.springboot.service;

/**
 * Interface for OTP configuration properties.
 */
public interface IOtpProperties {

    /**
     * Gets the maximum number of OTP attempts allowed.
     * @return max attempts
     */
    int getMaxAttempts();

    /**
     * Gets the attempt window in minutes.
     * @return attempt window minutes
     */
    int getAttemptWindowMinutes();

    /**
     * Gets the OTP expiry in minutes.
     * @return expiry minutes
     */
    int getExpiryMinutes();
}