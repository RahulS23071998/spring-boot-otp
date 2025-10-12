package com.starter.springboot.constants;

/**
 * Validation-related constants including regex patterns and validation messages.
 */
public final class ValidationConstants {

    // Validation Patterns
    public static final String USERNAME_PATTERN = "^(?=.{1,50}$)(?![_.])(?!.*[_.]{2})[a-zA-Z0-9._]+(?<![_.])$";
    
    // Date Format Patterns
    public static final String DATE_PATTERN = "yyyy-MM-dd";

    
    private ValidationConstants() {
        // Private constructor to prevent instantiation
    }
}