package com.starter.springboot.constants;

/**
 * Security-related constants including JWT, authorities, and authentication.
 */
public final class SecurityConstants {


    // Authority Constants
    public static final String ADMIN_AUTHORITY = "ROLE_ADMIN";
    public static final String USER_AUTHORITY = "ROLE_USER";
    public static final String ROLE_PREFIX = "ROLE_";

    // Authentication Messages
    public static final String INVALID_CREDENTIALS_MESSAGE = "Invalid credentials";


    private SecurityConstants() {
        // Private constructor to prevent instantiation
    }
}