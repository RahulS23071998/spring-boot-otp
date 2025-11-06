package com.starter.springboot.services.dto;

/**
 * Represents the result of an OTP generation attempt, including success status and message.
 */
public final class OtpGenerationResult {

    private final boolean success;
    private final String message;

    private OtpGenerationResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static OtpGenerationResult success() {
        return new OtpGenerationResult(true, null);
    }

    public static OtpGenerationResult rateLimited() {
        return new OtpGenerationResult(false, "Please wait 15 seconds before requesting a new OTP.");
    }

    public static OtpGenerationResult maxAttemptsExceeded() {
        return new OtpGenerationResult(false, "Maximum OTP attempts exceeded. Try again later.");
    }

    public static OtpGenerationResult deliveryFailed() {
        return new OtpGenerationResult(false, "Unable to deliver OTP at this time. Please try again.");
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}