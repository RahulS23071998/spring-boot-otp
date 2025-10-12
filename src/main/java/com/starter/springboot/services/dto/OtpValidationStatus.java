package com.starter.springboot.services.dto;

/**
 * Represents the possible outcomes when validating an OTP.
 */
public enum OtpValidationStatus {
    SUCCESS,
    INVALID,
    LOCKED
}