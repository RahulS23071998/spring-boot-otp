package com.starter.springboot.constants;

public final class AdminConstants {

    // Admin Endpoint Paths
    public static final String ADMIN_BASE_PATH = "/admin";
    public static final String USERS_LOCK_ENDPOINT = "/users/{userId}/lock";
    public static final String USERS_UNLOCK_ENDPOINT = "/users/{userId}/unlock";
    public static final String USERS_REVOKE_TOKENS_ENDPOINT = "/users/{userId}/revoke-tokens";
    public static final String TOKENS_INSPECT_ENDPOINT = "/tokens/inspect";
    public static final String CURRENT_OTP_ENDPOINT = "/otp/{username}";
    public static final String AUDIT_LOGS_ENDPOINT = "/audit/logs";
    public static final String BULK_USER_IMPORT_ENDPOINT = "/users/bulk";
    public static final String BULK_OTP_SEND_ENDPOINT = "/otp/bulk-send";

    // Request Parameters
    public static final String FILE_PARAM = "file";
    public static final String TOKEN_PARAM = "token";
    public static final String USERNAMES_PARAM = "usernames";
    public static final String PAGE_PARAM = "page";
    public static final String SIZE_PARAM = "size";
    public static final String SORT_BY_PARAM = "sortBy";
    public static final String SORT_DIRECTION_PARAM = "sortDirection";

    // Default Values
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final String DEFAULT_SORT_BY = "createdDate";
    public static final String DEFAULT_SORT_DIRECTION = "desc";

    // Redis Keys
    public static final String OTP_REDIS_KEY_PREFIX = "OTP:";

    // Response Keys
    public static final String MESSAGE_KEY = "message";
    public static final String VALID_KEY = "valid";
    public static final String USER_ID_KEY = "userId";
    public static final String EXPIRES_AT_KEY = "expiresAt";
    public static final String CREATED_AT_KEY = "createdAt";
    public static final String USERNAME_KEY = "username";
    public static final String OTP_HASH_KEY = "otpHash";
    public static final String TOTAL_USERS_KEY = "totalUsers";
    public static final String OTP_SENT_KEY = "otpSent";
    public static final String OTP_FAILED_KEY = "otpFailed";
    public static final String SUCCESS_KEY = "success";

    // Admin Messages - Token Management
    public static final String REVOKE_TOKENS_SUCCESS_MESSAGE = "All refresh tokens revoked for user ";
    public static final String TOKEN_INVALID_MESSAGE = "Token is invalid, expired, or revoked";

    // Admin Messages - Bulk OTP
    public static final String USERNAMES_EMPTY_MESSAGE = "Usernames list cannot be empty";

    // File Upload Messages
    public static final String UNSUPPORTED_FILE_FORMAT_MESSAGE = "Unsupported file format. Only CSV and XLSX are supported.";
    public static final String FILE_PROCESSING_ERROR_MESSAGE = "File processing error: ";

    // Bulk Import - Validation Messages
    public static final String USERNAME_REQUIRED_MESSAGE = "Username is required";
    public static final String PASSWORD_REQUIRED_MESSAGE = "Password is required";
    public static final String EMAIL_REQUIRED_MESSAGE = "Email is required";
    public static final String FIRST_NAME_REQUIRED_MESSAGE = "First name is required";
    public static final String LAST_NAME_REQUIRED_MESSAGE = "Last name is required";

    // Bulk Import - Success/Error Messages
    public static final String USER_CREATED_SUCCESS_MESSAGE = "User created successfully";
    public static final String USER_ALREADY_EXISTS_ADMIN_MESSAGE = "User already exists";
    public static final String USER_CREATION_FAILED_MESSAGE = "Failed to create user: ";

    // File Format Extensions
    public static final String CSV_FILE_EXTENSION = ".csv";
    public static final String XLSX_FILE_EXTENSION = ".xlsx";

    // CSV Headers
    public static final String[] CSV_HEADERS = {
        "username",
        "password",
        "email",
        "firstName",
        "lastName",
        "otpRequired"
    };

    // OTP Required Flags
    public static final String OTP_FLAG_TRUE = "true";
    public static final String OTP_FLAG_YES = "yes";
    public static final String OTP_FLAG_ONE = "1";
    public static final String OTP_FLAG_DEFAULT_FALSE = "false";

    // Log Messages
    public static final String LOG_ERROR_IMPORTING_USERS = "Error importing bulk users: {}";
    public static final String LOG_ERROR_PROCESSING_CSV_ROW = "Error processing CSV row {}: {}";
    public static final String LOG_ERROR_PROCESSING_EXCEL_ROW = "Error processing Excel row {}: {}";
    public static final String LOG_ERROR_CREATING_USER = "Error creating user {}: {}";
    public static final String LOG_WARN_PROCESSING_ROW = "Error processing row {}: {}";

    private AdminConstants() {
        // Private constructor to prevent instantiation
    }
}
