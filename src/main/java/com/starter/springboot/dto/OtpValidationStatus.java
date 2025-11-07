package com.starter.springboot.dto;

/**
 * Represents the possible outcomes when validating an OTP.
 */
public enum OtpValidationStatus {
    SUCCESS,
    INVALID,
    LOCKED
}