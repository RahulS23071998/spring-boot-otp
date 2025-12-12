package com.starter.springboot.service;

import org.springframework.stereotype.Service;

@Service
public class PasswordValidationService {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 32;

    public PasswordValidationResult validate(String password) {
        if (password == null || password.isBlank()) {
            return PasswordValidationResult.failed("Password cannot be empty");
        }

        if (password.length() < MIN_LENGTH) {
            return PasswordValidationResult.failed("Password must be at least " + MIN_LENGTH + " characters long");
        }

        if (password.length() > MAX_LENGTH) {
            return PasswordValidationResult.failed("Password must not exceed " + MAX_LENGTH + " characters");
        }

        if (!password.matches(".*[a-z].*")) {
            return PasswordValidationResult.failed("Password must contain at least one lowercase letter");
        }

        if (!password.matches(".*[A-Z].*")) {
            return PasswordValidationResult.failed("Password must contain at least one uppercase letter");
        }

        if (!password.matches(".*\\d.*")) {
            return PasswordValidationResult.failed("Password must contain at least one digit");
        }

        if (!password.matches(".*[@$!%*?&].*")) {
            return PasswordValidationResult.failed("Password must contain at least one special character (@$!%*?&)");
        }

        return PasswordValidationResult.success();
    }

    public record PasswordValidationResult(boolean valid, String message) {

        public static PasswordValidationResult success() {
                return new PasswordValidationResult(true, null);
            }

            public static PasswordValidationResult failed(String message) {
                return new PasswordValidationResult(false, message);
            }
        }
}
