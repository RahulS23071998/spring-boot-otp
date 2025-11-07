package com.starter.springboot.exception;

/**
 * Exception raised when a user must complete an OTP challenge before receiving a JWT.
 */
public class OtpRequiredException extends RuntimeException {

    public OtpRequiredException(String message) {
        super(message);
    }

    public OtpRequiredException(String message, Throwable cause) {
        super(message, cause);
    }
}