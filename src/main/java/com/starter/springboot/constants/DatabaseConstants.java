package com.starter.springboot.constants;

/**
 * Database-related constants including table names, column names, and constraints.
 */
public final class DatabaseConstants {

    // Table Names
    public static final String USER_TABLE = "user_account";
    public static final String ROLE_TABLE = "role";
    public static final String AUTHORITY_TABLE = "authority";
    public static final String OTP_AUDIT_ENTRIES_TABLE = "otp_audit_entries";
    public static final String REFRESH_TOKENS_TABLE = "refresh_tokens";
    public static final String PASSWORD_HISTORY_TABLE = "password_history";
    
    // User Table Columns
    public static final String USER_ID_COLUMN = "id";
    public static final String USERNAME_COLUMN = "username";
    public static final String PASSWORD_COLUMN = "password";
    public static final String FIRST_NAME_COLUMN = "first_name";
    public static final String LAST_NAME_COLUMN = "last_name";
    public static final String EMAIL_COLUMN = "email";
    public static final String ENABLED_COLUMN = "enabled";
    public static final String STATUS_COLUMN = "status";
    public static final String LAST_PASSWORD_RESET_DATE_COLUMN = "last_password_reset_date";
    public static final String IS_OTP_REQUIRED_COLUMN = "is_otp_required";
    public static final String ROLE_ID_COLUMN = "role_id";
    public static final String AUTHORITY_ID_COLUMN = "authority_id";
    public static final String AUTH_TYPE_COLUMN = "auth_type";
    public static final String GOOGLE_ID_COLUMN = "google_id";
    public static final String EMAIL_VERIFIED_COLUMN = "email_verified";
    public static final String PASSWORD_SET_COLUMN = "password_set";
    public static final String TOTP_SECRET_COLUMN = "totp_secret";
    
    // Role Table Columns
    public static final String ROLE_NAME_COLUMN = "name";
    public static final String ROLE_DESCRIPTION_COLUMN = "description";
    
    // Authority Table Columns
    public static final String AUTHORITY_NAME_COLUMN = "name";
    public static final String AUTHORITY_DESCRIPTION_COLUMN = "description";
    
    // OTP Audit Table Columns
    public static final String ISSUED_ON_COLUMN = "issued_on";
    public static final String EXPIRES_ON_COLUMN = "expires_on";
    public static final String PARTNER_EXPIRY_COLUMN = "partner_expiry";
    public static final String OTP_USERNAME_COLUMN = "username";

    // Refresh Tokens Table Columns
    public static final String REFRESH_TOKEN_USER_ID_COLUMN = "user_id";
    public static final String REFRESH_TOKEN_TOKEN_COLUMN = "token";
    public static final String REFRESH_TOKEN_EXPIRES_AT_COLUMN = "expires_at";
    public static final String REFRESH_TOKEN_CREATED_AT_COLUMN = "created_at";
    public static final String REFRESH_TOKEN_REVOKED_AT_COLUMN = "revoked_at";
    public static final String REFRESH_TOKEN_REPLACED_BY_TOKEN_COLUMN = "replaced_by_token";
    public static final String IP_ADDRESS_COLUMN = "ip_address";
    public static final String USER_AGENT_COLUMN = "user_agent";
    public static final String IS_ACTIVE_COLUMN = "is_active";
    
    // Password History Table Columns
    public static final String PASSWORD_HISTORY_ID_COLUMN = "password_history_id";

    // Auditing Columns
    public static final String CREATED_DATE_COLUMN = "created_date";
    public static final String CREATED_BY_COLUMN = "created_by";
    public static final String LAST_MODIFIED_DATE_COLUMN = "last_modified_date";
    public static final String LAST_MODIFIED_BY_COLUMN = "last_modified_by";
    
    // Column Constraints
    public static final int USERNAME_MAX_LENGTH = 50;
    public static final int PASSWORD_MAX_LENGTH = 100;
    public static final int FIRST_NAME_MAX_LENGTH = 50;
    public static final int LAST_NAME_MAX_LENGTH = 50;
    public static final int EMAIL_MAX_LENGTH = 50;
    public static final int STATUS_MAX_LENGTH = 20;
    public static final int AUTH_TYPE_MAX_LENGTH = 20;
    public static final int GOOGLE_ID_MAX_LENGTH = 255;
    public static final int TOTP_SECRET_MAX_LENGTH = 255;
    public static final int AUTHORITY_NAME_MAX_LENGTH = 50;
    public static final int ROLE_NAME_MAX_LENGTH = 255;
    public static final int REFRESH_TOKEN_MAX_LENGTH = 500;
    public static final int IP_ADDRESS_MAX_LENGTH = 45;
    public static final int USER_AGENT_MAX_LENGTH = 500;
    public static final int DESCRIPTION_MAX_LENGTH = 500;
    public static final int AUDIT_USER_MAX_LENGTH = 50;
    
    // Validation Constraints
    public static final int MIN_USERNAME_LENGTH = 1;
    public static final int MIN_PASSWORD_LENGTH = 4;
    public static final int MIN_NAME_LENGTH = 4;
    public static final int DTO_PASSWORD_MAX_LENGTH = 100;
    public static final int CLIENT_ID_MAX_LENGTH = 64;
    public static final int DEVICE_ID_MAX_LENGTH = 128;
    
    // Column Definitions
    public static final String DATE_COLUMN_DEFINITION = "TIMESTAMP";
    
    // JPA Mappings
    public static final String ROLE_MAPPING_FIELD = "role";

    // Index Names
    public static final String IDX_REFRESH_TOKEN_USER = "idx_refresh_token_user";
    public static final String IDX_REFRESH_TOKEN_EXPIRES = "idx_refresh_token_expires";
    public static final String IDX_REFRESH_TOKEN_TOKEN = "idx_refresh_token_token";
    public static final String IDX_USER_EMAIL = "idx_user_email";
    public static final String IDX_USER_STATUS = "idx_user_status";
    public static final String IDX_USER_ENABLED = "idx_user_enabled";
    public static final String IDX_USER_OTP_REQUIRED = "idx_user_otp_required";
    public static final String IDX_USER_EMAIL_VERIFIED = "idx_user_email_verified";
    public static final String IDX_USER_GOOGLE_ID = "idx_user_google_id";
    public static final String IDX_USER_AUTH_TYPE = "idx_user_auth_type";
    public static final String IDX_USER_ROLE_ID = "idx_user_role_id";
    public static final String IDX_USER_AUTHORITY_ID = "idx_user_authority_id";
    public static final String IDX_USER_CREATED_DATE = "idx_user_created_date";
    public static final String IDX_OTP_AUDIT_USERNAME = "idx_otp_audit_username";
    public static final String IDX_OTP_AUDIT_CREATED_DATE = "idx_otp_audit_created_date";
    public static final String IDX_PASSWORD_HISTORY_USER_ID = "idx_password_history_user_id";
    public static final String IDX_PASSWORD_HISTORY_CREATED_DATE = "idx_password_history_created_date";
    
    private DatabaseConstants() {
        // Private constructor to prevent instantiation
    }
}