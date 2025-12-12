package com.starter.springboot.constants;

/**
 * Validation-related constants including regex patterns and validation messages.
 */
public final class ValidationConstants {

    // Validation Patterns
    // Allows email format (with @) or alphanumeric usernames with underscores and dots
    public static final String USERNAME_PATTERN = "^[a-zA-Z0-9._@-]{3,50}$";
    
    // Date Format Patterns
    public static final String DATE_PATTERN = "yyyy-MM-dd";

    
    private ValidationConstants() {
        // Private constructor to prevent instantiation
    }
}