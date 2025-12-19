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
    public static final String OTP_VERIFY_URL_TEMPLATE = "http://localhost:8080/auth/verify-otp?email=%s";
    public static final String OTP_EMAIL_CONTENT_TEMPLATE = """
            <p style="color:white; font-size:15px; margin:0 0 10px 0;">
                Hello,
            </p>
            
            <p style="color:white; font-size:15px; margin:0 0 15px 0;">
                Your One-Time Password (OTP) is:
            </p>
            
            <p style="color:#0a6ed1; font-size:24px; font-weight:bold; letter-spacing:3px; margin:0 0 25px 0;">
                %s
            </p>
            
            <p style="text-align:center; margin:30px 0;">
                <a href="%s"
                   style="background-color:#0a6ed1; color:white; padding:12px 24px;
                   text-decoration:none; border-radius:5px; font-size:16px; display:inline-block;">
                    Verify OTP
                </a>
            </p>
            
            <p style="color:#cccccc; font-size:12px; margin-top:30px;">
                This OTP is valid for a limited time. Do not share it with anyone.
            </p>
            """;


    // Email Validation Messages
    public static final String NO_RECIPIENTS_PROVIDED_MESSAGE = "No recipients provided for email with subject: {}";
    public static final String EMAIL_SENT_SUCCESS_MESSAGE = "Email successfully sent to: {}";
    public static final String EMAIL_SENDING_ERROR_MESSAGE = "Sending e-mail error: {}";
    public static final String NO_EMAIL_FOR_USERNAME_MESSAGE = "No email found for username: {}";
    public static final String FAILED_TO_SEND_OTP_EMAIL_MESSAGE = "Failed to send OTP email to user: {}";

    // System notification subjects
    public static final String OTP_LOCKED_EMAIL_SUBJECT = "OTP access temporarily locked";
    public static final String OTP_DELIVERY_FAILURE_SUBJECT = "OTP delivery failed";

    // Password activation email constants
    public static final String PASSWORD_ACTIVATION_SUBJECT = "Set Your Password";

    // activation URL template expects one placeholder for temporary token
    public static final String ACTIVATION_URL_TEMPLATE = "http://localhost:8080/auth/set-password?temporaryToken=%s";

    public static final String EMAIL_WRAPPER_TEMPLATE = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>%s</title>
            </head>
            
            <body style="margin:0; padding:0; background-color:#f3f3f3; font-family:Arial, sans-serif;">
            
            <table width="100%%" cellpadding="0" cellspacing="0" bgcolor="#f3f3f3">
                <tr>
                    <!-- CENTERING MUST HAPPEN HERE -->
                    <td align="center" style="padding:24px 12px;">
            
                        <!-- INNER CARD -->
                        <table width="100%%" cellpadding="0" cellspacing="0"
                               style="max-width:600px; background-color:#1c1c1e; border-radius:8px;">
                            <tr>
                                <td style="padding:30px;">
            
                                    <!-- HEADER -->
                                    <h2 style="color:white; font-size:22px; margin:0 0 20px 0;">
                                        IDENTITY AUTHENTICATION
                                    </h2>
            
                                    %s  <!-- CONTENT GOES HERE -->
            
                                    <!-- FOOTER -->
                                    <p style="color:white; font-size:14px; margin:40px 0 0 0;">
                                        Best regards,<br>
                                        OTP Authentication Team
                                    </p>
            
                                </td>
                            </tr>
                        </table>
            
                    </td>
                </tr>
            </table>
            
            </body>
            </html>
            """;


    public static final String ACTIVATION_CONTENT_TEMPLATE = """
            <p style="color:white; font-size:15px; margin:0 0 10px 0;">
                Hello %s,
            </p>
            
            <p style="color:white; font-size:15px; margin:0 0 20px 0;">
                An account has been created for you. Please click the button below to set your password.
            </p>
            
            <p style="text-align:center; margin:30px 0;">
                <a href="%s"
                   style="background-color:#0a6ed1; color:white; padding:12px 24px; 
                   text-decoration:none; border-radius:5px; font-size:16px; display:inline-block;">
                    Set Password
                </a>
            </p>
            
            <p style="color:#cccccc; font-size:12px; margin-top:30px;">
                If the button above is not displayed or does not work, copy and paste the following link into your browser:
            </p>
            
            <p style="color:#4aa3ff; font-size:12px; word-break:break-all;">
                %s
            </p>
            """;

    // Password set confirmation email
    public static final String PASSWORD_SET_SUBJECT = "Password Set Successfully";

    public static final String PASSWORD_SET_CONTENT_TEMPLATE = """
            <p style="color:white; font-size:15px; margin:0 0 10px 0;">
                Hello %s,
            </p>
            
            <p style="color:white; font-size:15px; margin:0 0 15px 0;">
                Your password has been set successfully for your account.
            </p>
            
            <p style="color:white; font-size:15px; margin:0 0 30px 0;">
                You can now log in using your email and password.
            </p>
            """;


    // Configuration Properties
    public static final String MAIL_CONFIG_PREFIX = "spring.mail";

    private EmailConstants() {
        // Private constructor to prevent instantiation
    }
}