package com.starter.springboot.controller;

import com.starter.springboot.constants.ApplicationConstants;
import com.starter.springboot.dto.AuthResponseDTO;
import com.starter.springboot.dto.GoogleTokenDTO;
import com.starter.springboot.dto.OAuthRedirectDTO;
import com.starter.springboot.dto.OtpGenerationResult;
import com.starter.springboot.entity.RefreshToken;
import com.starter.springboot.entity.User;
import com.starter.springboot.security.jwt.ITokenProvider;
import com.starter.springboot.security.jwt.JWTToken;
import com.starter.springboot.service.IGoogleOAuthService;
import com.starter.springboot.service.IOtpAuditService;
import com.starter.springboot.service.IOtpRateLimiter;
import com.starter.springboot.service.IPasswordSetupService;
import com.starter.springboot.service.IRefreshTokenService;
import com.starter.springboot.service.ITemporaryPasswordTokenService;
import com.starter.springboot.service.ITotpService;
import com.starter.springboot.service.IUserService;
import com.starter.springboot.utils.RequestContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(ApplicationConstants.AUTH_ENDPOINT)
@Validated
@Tag(name = "Google OAuth MFA", description = "Google OAuth authentication endpoints with MFA/TOTP")
public class GoogleOAuthMfaController {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleOAuthMfaController.class);

    private final IGoogleOAuthService googleOAuthService;
    private final IUserService userService;
    private final ITokenProvider tokenProvider;
    private final IRefreshTokenService refreshTokenService;
    private final IOtpRateLimiter otpRateLimiter;
    private final IOtpAuditService otpAuditService;
    private final ITemporaryPasswordTokenService temporaryPasswordTokenService;
    private final IPasswordSetupService passwordSetupService;
    private final ITotpService totpService;

    public GoogleOAuthMfaController(IGoogleOAuthService googleOAuthService,
                                    IUserService userService,
                                    ITokenProvider tokenProvider,
                                    IRefreshTokenService refreshTokenService,
                                    IOtpRateLimiter otpRateLimiter,
                                    IOtpAuditService otpAuditService,
                                    ITemporaryPasswordTokenService temporaryPasswordTokenService,
                                    IPasswordSetupService passwordSetupService,
                                    ITotpService totpService) {
        this.googleOAuthService = googleOAuthService;
        this.userService = userService;
        this.tokenProvider = tokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.otpRateLimiter = otpRateLimiter;
        this.otpAuditService = otpAuditService;
        this.temporaryPasswordTokenService = temporaryPasswordTokenService;
        this.passwordSetupService = passwordSetupService;
        this.totpService = totpService;
    }

    @PostMapping(value = "/google-oauth-mfa")
    @Operation(summary = "Authenticate user with Google OAuth and MFA",
            description = "Authenticate a user using Google OAuth 2.0. Send the Google ID token from the client. " +
                    "The backend will verify the token, create/update the user, generate TOTP secret, and return QR code URL. " +
                    "User must verify the TOTP code before receiving JWT tokens. If user needs to set password, returns 202 status.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Google OAuth token request with MFA",
            content = @Content(schema = @Schema(implementation = GoogleTokenDTO.class),
                    examples = @ExampleObject(value = """
                {
                  "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjEifQ...",
                  "rememberMe": true,
                  "clientId": "postman",
                  "deviceId": "postman-test"
                }
                """)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Google OAuth authentication successful, TOTP QR code provided",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "202", description = "Google OAuth user created but needs to set password",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Invalid or expired Google ID token"),
            @ApiResponse(responseCode = "400", description = "Invalid request format or missing idToken")
    })
    public ResponseEntity<?> googleOAuthMfa(@Valid @RequestBody GoogleTokenDTO googleTokenDTO) {
        LOGGER.info("Google OAuth MFA authentication attempt");

        if (!StringUtils.hasText(googleTokenDTO.getIdToken())) {
            return ResponseEntity.badRequest()
                    .body(AuthResponseDTO.failed(null, "Missing Google ID token"));
        }

        String rateLimitKey = "google-oauth-mfa:" + (googleTokenDTO.getClientId() != null ? googleTokenDTO.getClientId() : "default");

        OtpGenerationResult rateLimitCheck = otpRateLimiter.checkRateLimit(rateLimitKey);
        if (rateLimitCheck != null) {
            LOGGER.warn("Google OAuth MFA rate limit exceeded for client: {}", googleTokenDTO.getClientId());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(AuthResponseDTO.failed(null, rateLimitCheck.getMessage()));
        }

        OtpGenerationResult attemptCheck = otpRateLimiter.checkAndIncrementAttempts(rateLimitKey);
        if (attemptCheck != null) {
            LOGGER.warn("Google OAuth MFA max attempts exceeded for client: {}", googleTokenDTO.getClientId());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(AuthResponseDTO.failed(null, "Maximum authentication attempts exceeded. Please try again later"));
        }

        Map<String, Object> googleUserInfo = googleOAuthService.verifyAndExtractUserInfo(googleTokenDTO.getIdToken());
        if (MapUtils.isEmpty(googleUserInfo)) {
            LOGGER.warn("Google OAuth MFA authentication failed: Invalid ID token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponseDTO.failed(null, "Invalid Google ID token"));
        }

        try {
            User user = userService.findOrCreateGoogleOAuthUser(googleUserInfo);

            if (!Boolean.TRUE.equals(user.getPasswordSet())) {
                LOGGER.info("Google OAuth MFA user {} needs to set password", user.getUsername());
                String temporaryToken = temporaryPasswordTokenService.generateTemporaryToken(user.getUsername());

                try {
                    passwordSetupService.sendPasswordActivationNotification(user, temporaryToken);
                } catch (Exception e) {
                    LOGGER.warn("Failed to send password activation email for user {}: {}", user.getUsername(), e.getMessage());
                }

                return ResponseEntity.status(HttpStatus.ACCEPTED)
                        .body(new OAuthRedirectDTO("SET_PASSWORD_REQUIRED", user.getUsername(),
                                "Please set your password to complete registration", temporaryToken));
            }

            String totpSecret;
            boolean isNewSecret = false;

            if (totpService.hasTotpSecretPersistent(user.getId())) {
                totpSecret = totpService.getTotpSecretPersistent(user.getId());
                LOGGER.info("Using existing TOTP secret for user: {}", user.getUsername());
            } else {
                totpSecret = totpService.generateTotpSecret(user.getUsername());
                totpService.saveTotpSecretPersistent(user.getId(), totpSecret);
                isNewSecret = true;
                LOGGER.info("Generated new TOTP secret for user: {}", user.getUsername());
            }

            totpService.saveTotpSecret(user.getUsername(), totpSecret);
            String qrCodeUrl = totpService.generateQrCodeUrl(user.getUsername(), totpSecret, "Spring Boot OTP - Google");

            Map<String, Object> mfaResponse = new HashMap<>();
            mfaResponse.put("status", "MFA_REQUIRED");
            mfaResponse.put("username", user.getUsername());
            String message = isNewSecret 
                ? "Please scan the QR code with your authenticator app and verify the 6-digit code"
                : "Enter the 6-digit code from your authenticator app";
            mfaResponse.put("message", message);
            mfaResponse.put("qr_code_url", isNewSecret ? qrCodeUrl : null);
            mfaResponse.put("secret", isNewSecret ? totpSecret : null);
            mfaResponse.put("is_new_secret", isNewSecret);

            otpRateLimiter.recordRateLimitTimestamp(rateLimitKey);
            otpRateLimiter.resetAttempts(rateLimitKey);

            LOGGER.info("Google OAuth MFA authentication initiated for user: {}", user.getUsername());
            return ResponseEntity.ok(mfaResponse);

        } catch (Exception e) {
            LOGGER.error("Google OAuth MFA authentication failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponseDTO.failed(null, "Google OAuth MFA authentication failed"));
        }
    }

    @PostMapping(value = "/google-mfa-verify")
    @Operation(summary = "Verify Google OAuth MFA code",
            description = "After scanning the QR code and getting the 6-digit code from authenticator app, verify it here. " +
                    "On successful verification, JWT and refresh tokens will be issued.")
    public ResponseEntity<?> verifyGoogleMfa(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String totpCode = request.get("totp_code");
        String rememberMeStr = request.get("remember_me");
        String clientId = request.get("client_id");
        String deviceId = request.get("device_id");

        if (!StringUtils.hasText(username) || !StringUtils.hasText(totpCode)) {
            return ResponseEntity.badRequest()
                    .body(AuthResponseDTO.failed(null, "Username and TOTP code are required"));
        }

        String rateLimitKey = "google-mfa-verify:" + username;

        // Check rate limit (too many requests in short time)
        OtpGenerationResult rateLimitCheck = otpRateLimiter.checkRateLimit(rateLimitKey);
        if (rateLimitCheck != null) {
            LOGGER.warn("Google MFA verify rate limit exceeded for user: {}", username);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(AuthResponseDTO.failed(username, rateLimitCheck.getMessage()));
        }

        // Check max attempts
        OtpGenerationResult attemptCheck = otpRateLimiter.checkAndIncrementAttempts(rateLimitKey);
        if (attemptCheck != null) {
            LOGGER.warn("Google MFA verify max attempts exceeded for user: {}", username);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(AuthResponseDTO.failed(username, "Maximum verification attempts exceeded. Please try again later"));
        }

        try {
            User user = userService.findUserByUsername(username);
            var validationResult = totpService.validateTotpPersistent(user.getId(), totpCode);
            if (!validationResult.isSuccess()) {
                LOGGER.warn("Google MFA verification failed for user: {}", username);
                // Record the timestamp for rate limiting on failure too
                otpRateLimiter.recordRateLimitTimestamp(rateLimitKey);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AuthResponseDTO.failed(username, "Invalid or expired TOTP code"));
            }

            // Reset attempts on success
            otpRateLimiter.resetAttempts(rateLimitKey);

            SecurityContextHolder.clearContext();
            refreshTokenService.revokeAllUserRefreshTokens(user.getId());

            boolean rememberMe = Boolean.parseBoolean(rememberMeStr);
            JWTToken token = tokenProvider.createAccessTokenAfterVerifiedOtp(user.getUsername(), rememberMe);
            String ipAddress = RequestContextUtil.getClientIpAddress();
            String userAgent = RequestContextUtil.getUserAgent();
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId(), tokenProvider.getRefreshTokenValidityInSeconds(), ipAddress, userAgent);

            JWTToken fullToken = new JWTToken(
                    token.getIdToken(),
                    refreshToken.getToken(),
                    token.getTokenType(),
                    token.getExpiresIn(),
                    tokenProvider.getRefreshTokenValidityInSeconds()
            );

            totpService.revokeTotpSecret(username);
            otpAuditService.persistAuditEntry(username);

            AuthResponseDTO response = AuthResponseDTO.success(user.getUsername(), fullToken, rememberMe)
                    .withContext(rememberMe, clientId, deviceId);

            LOGGER.info("Google OAuth MFA verification successful for user: {}", username);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            LOGGER.error("Google MFA verification failed for user {}: {}", username, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponseDTO.failed(username, "MFA verification failed"));
        }
    }
}