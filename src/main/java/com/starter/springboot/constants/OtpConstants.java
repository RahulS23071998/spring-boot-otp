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
    public static final String RATE_LIMIT_KEY_SUFFIX = ":last_sent";

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_LOCKED = "LOCKED";

    public static final String INVALID_OTP_MESSAGE = "Invalid OTP provided.";
    public static final String LOCKED_OTP_MESSAGE = "OTP has been locked due to too many invalid attempts.";

    public static final String MAX_ATTEMPTS_EXCEEDED_MESSAGE = "Maximum OTP attempts exceeded. Try again later.";
    public static final String OTP_DELIVERY_FAILURE_MESSAGE_TEMPLATE = "Unable to deliver OTP e-mail for account '%s' at this time. Please try again.";
    public static final String OTP_RATE_LIMIT_MESSAGE = "Please wait 15 seconds before requesting a new OTP.";
    public static final int OTP_RATE_LIMIT_SECONDS = 15;

    public static final String OTP_AUDIT_PURGE_DESCRIPTION = "Scheduled task that removes expired OTP audit entries.";
    public static final String OTP_AUDIT_PURGE_CRON = "${otp.audit-purge.cron:0 0 2 * * *}";

    // OTP Service Description
    public static final String OTP_SERVICE_DESCRIPTION = "Service responsible for handling OTP related functionality.";


    private OtpConstants() {
        // Private constructor to prevent instantiation
    }
}