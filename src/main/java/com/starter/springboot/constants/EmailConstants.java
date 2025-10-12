package com.starter.springboot.constants;

/**
 * Email-related constants for mail configuration and messages.
 */
public final class EmailConstants {

    // Mail Properties
    public static final String MAIL_TRANSPORT_PROTOCOL_KEY = "mail.transport.protocol";
    public static final String MAIL_SMTP_AUTH_KEY = "mail.smtp.auth";
    public static final String MAIL_SMTP_STARTTLS_ENABLE_KEY = "mail.smtp.starttls.enable";
    public static final String MAIL_DEBUG_KEY = "mail.debug";
    
    // Mail Protocol
    public static final String SMTP_PROTOCOL = "smtp";
    
    // OTP Email Content
    public static final String OTP_EMAIL_SUBJECT = "Spring Boot OTP Password.";
    public static final String OTP_EMAIL_BODY_PREFIX = "OTP Password: ";
    
    // Email Validation Messages
    public static final String NO_RECIPIENTS_PROVIDED_MESSAGE = "No recipients provided for email with subject: {}";
    public static final String EMAIL_SENT_SUCCESS_MESSAGE = "Email successfully sent to: {}";
    public static final String EMAIL_SENDING_ERROR_MESSAGE = "Sending e-mail error: {}";
    public static final String NO_EMAIL_FOR_USERNAME_MESSAGE = "No email found for username: {}";
    public static final String FAILED_TO_SEND_OTP_EMAIL_MESSAGE = "Failed to send OTP email to user: {}";
    
    // Configuration Properties
    public static final String MAIL_CONFIG_PREFIX = "spring.mail";

    private EmailConstants() {
        // Private constructor to prevent instantiation
    }
}