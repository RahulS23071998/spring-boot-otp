package com.starter.springboot.constants;

/**
 * OTP (One-Time Password) related constants.
 */
public final class OtpConstants {

    // Redis Keys
    public static final String OTP_REDIS_KEY_PREFIX = "otp:";
    public static final String ATTEMPTS_KEY_SUFFIX = ":attempts";
    public static final String STATUS_KEY_SUFFIX = ":status";
    public static final String FAILURE_KEY_SUFFIX = ":failures";

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_LOCKED = "LOCKED";

    public static final String INVALID_OTP_MESSAGE = "Invalid OTP provided.";
    public static final String LOCKED_OTP_MESSAGE = "OTP has been locked due to too many invalid attempts.";

    // OTP Service Description
    public static final String OTP_SERVICE_DESCRIPTION = "Service responsible for handling OTP related functionality.";


    private OtpConstants() {
        // Private constructor to prevent instantiation
    }
}