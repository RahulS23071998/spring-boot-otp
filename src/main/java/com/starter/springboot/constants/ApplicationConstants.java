package com.starter.springboot.constants;

/**
 * General application-wide constants.
 */
public final class ApplicationConstants {

    // Application Info
    public static final String APPLICATION_RUNNING_MESSAGE = "Application is running!";

    // Endpoint paths
    public static final String AUTH_ENDPOINT = "/auth";
    public static final String API_BASE_PATH = "/api";
    public static final String USERS_ENDPOINT = "/users";
    public static final String AUTHENTICATE_ENDPOINT = "/authenticate";
    public static final String VERIFY_ENDPOINT = "/verify";
    public static final String REFRESH_ENDPOINT = "/refresh";
    public static final String PUBLIC_ENDPOINT = "/public";
    public static final String PASSWORD_ENDPOINT = "/password";
    public static final String CHANGE_PASSWORD_ENDPOINT = "/public/password";
    public static final String STATUS_ENDPOINT = "/{id}/status";

    // HTTP Headers
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String BEARER_TOKEN_TYPE = "Bearer";
    
    // HTTP Status Messages
    public static final String SUCCESS_STATUS = "SUCCESS";
    public static final String OTP_PENDING_STATUS = "OTP_PENDING";
    public static final String RATE_LIMITED_STATUS = "RATE_LIMITED";
    public static final String FAILED_STATUS = "FAILED";
    public static final String SUCCESS_MESSAGE = "Authentication successful";

    // Request parameters
    public static final String STATUS_PARAM = "status";
    public static final String ENABLED_PARAM = "enabled";
    public static final String USERID_PARAM = "userid";
    public static final String USERNAME_PARAM = "username";
    public static final String ID_PARAM = "id";

    // Error Messages
    public static final String INVALID_USERID_FORMAT_MESSAGE = "Invalid userid format";
    public static final String USERID_OR_USERNAME_REQUIRED_MESSAGE = "Provide either userid or username in payload";
    public static final String PASSWORD_CHANGE_ERROR_MESSAGE = "Unable to change password";
    public static final String CLIENT_REST_REQUEST_MESSAGE = "CLIENT REST REQUEST!";
    public static final String USER_NOT_FOUND_MESSAGE = "User with username ";
    public static final String USER_NOT_FOUND_SIMPLE_MESSAGE = "User not found!";
    public static final String USER_ALREADY_EXISTS_MESSAGE = "User with username ";
    public static final String DEFAULT_ROLE_NOT_CONFIGURED_MESSAGE = "Default role ROLE_USER not configured";
    public static final String STATUS_OR_ENABLED_REQUIRED_MESSAGE = "Either status or enabled must be provided";
    public static final String USER_ID_NOT_FOUND_MESSAGE = "User with id ";
    public static final String PASSWORD_FIELDS_REQUIRED_MESSAGE = "New password and confirm new password must be provided";
    public static final String PASSWORD_MISMATCH_MESSAGE = "New password and confirm new password do not match";
    public static final String OLD_PASSWORD_INCORRECT_MESSAGE = "Old password is incorrect";
    public static final String OLD_PASSWORD_FIELD = "oldpassword";
    public static final String NEW_PASSWORD_FIELD = "newpassword";
    public static final String CONFIRM_PASSWORD_FIELD = "confirmnewpassword";
    
    // Security Exception Messages
    public static final String USER_NOT_ACTIVATED_MESSAGE = " was not activated";
    public static final String USER_NOT_FOUND_DATABASE_MESSAGE = " was not found in the database";
    
    private ApplicationConstants() {
        // Private constructor to prevent instantiation
    }
}