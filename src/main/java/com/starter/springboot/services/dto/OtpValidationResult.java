package com.starter.springboot.services.dto;

/**
 * Represents the result of an OTP validation attempt, including status and helper checks.
 */
public final class OtpValidationResult {

    private final OtpValidationStatus status;

    private OtpValidationResult(OtpValidationStatus status) {
        this.status = status;
    }

    public static OtpValidationResult success() {
        return new OtpValidationResult(OtpValidationStatus.SUCCESS);
    }

    public static OtpValidationResult invalid() {
        return new OtpValidationResult(OtpValidationStatus.INVALID);
    }

    public static OtpValidationResult locked() {
        return new OtpValidationResult(OtpValidationStatus.LOCKED);
    }

    public OtpValidationStatus getStatus() {
        return status;
    }

    public boolean isSuccess() {
        return status == OtpValidationStatus.SUCCESS;
    }

    public boolean isLocked() {
        return status == OtpValidationStatus.LOCKED;
    }

    public boolean isInvalid() {
        return status == OtpValidationStatus.INVALID;
    }
}