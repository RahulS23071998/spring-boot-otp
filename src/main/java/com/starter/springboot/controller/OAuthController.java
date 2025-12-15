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
import com.starter.springboot.service.IUserService;
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
import java.util.Map;

@RestController
@RequestMapping(ApplicationConstants.AUTH_ENDPOINT)
@Validated
@Tag(name = "OAuth", description = "OAuth authentication endpoints")
public class OAuthController {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAuthController.class);

    private final IGoogleOAuthService googleOAuthService;
    private final IUserService userService;
    private final ITokenProvider tokenProvider;
    private final IRefreshTokenService refreshTokenService;
    private final IOtpRateLimiter otpRateLimiter;
    private final IOtpAuditService otpAuditService;
    private final ITemporaryPasswordTokenService temporaryPasswordTokenService;
    private final IPasswordSetupService passwordSetupService;

    public OAuthController(IGoogleOAuthService googleOAuthService,
                          IUserService userService,
                          ITokenProvider tokenProvider,
                          IRefreshTokenService refreshTokenService,
                          IOtpRateLimiter otpRateLimiter,
                          IOtpAuditService otpAuditService,
                          ITemporaryPasswordTokenService temporaryPasswordTokenService,
                          IPasswordSetupService passwordSetupService) {
        this.googleOAuthService = googleOAuthService;
        this.userService = userService;
        this.tokenProvider = tokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.otpRateLimiter = otpRateLimiter;
        this.otpAuditService = otpAuditService;
        this.temporaryPasswordTokenService = temporaryPasswordTokenService;
        this.passwordSetupService = passwordSetupService;
    }

    @PostMapping(value = ApplicationConstants.GOOGLE_OAUTH_ENDPOINT)
    @Operation(summary = "Authenticate user with Google OAuth",
        description = "Authenticate a user using Google OAuth 2.0. Send the Google ID token from the client. " +
                      "The backend will verify the token, create/update the user, and issue JWT tokens directly without OTP. " +
                      "If user needs to set password, returns 202 status with SET_PASSWORD_REQUIRED.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Google OAuth token request",
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
        @ApiResponse(responseCode = "200", description = "Google OAuth authentication successful, JWT tokens provided",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = AuthResponseDTO.class),
                examples = @ExampleObject(value = """
                        {
                             "username": "rahul.s@laderatechnology.com",
                             "status": "SUCCESS",
                             "message": "Authentication successful",
                             "otp_required": false,
                             "token": {
                                 "id_token": "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiIzMjB...",
                                 "refresh_token": "6ecfcd95-0b9e-477f-b4bb...",
                                 "token_type": "Bearer",
                                 "expires_in": 3600,
                                 "refresh_token_expires_in": 604800
                             },
                             "issued_at": "2025-11-30T17:28:50.054280900Z",
                             "remember_me": true,
                             "client_id": "postman",
                             "device_id": "postman-test"
                         }
                    """))),
        @ApiResponse(responseCode = "202", description = "Google OAuth user created but needs to set password",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = OAuthRedirectDTO.class),
                examples = @ExampleObject(value = """
                    {
                      "status": "SET_PASSWORD_REQUIRED",
                      "username": "user@example.com",
                      "message": "Please set your password to complete registration"
                    }
                    """))),
        @ApiResponse(responseCode = "401", description = "Invalid or expired Google ID token",
            content = @Content(mediaType = "application/json", 
                examples = @ExampleObject(value = """
                    {
                      "status": "FAILED",
                      "message": "Invalid Google ID token"
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Invalid request format or missing idToken")
    })
    public ResponseEntity<?> googleOAuth(@Valid @RequestBody GoogleTokenDTO googleTokenDTO) {
        LOGGER.info("Google OAuth authentication attempt");

        if (!StringUtils.hasText(googleTokenDTO.getIdToken())) {
            return ResponseEntity.badRequest()
                    .body(AuthResponseDTO.failed(null, "Missing Google ID token"));
        }

        String rateLimitKey = "google-oauth:" + (googleTokenDTO.getClientId() != null ? googleTokenDTO.getClientId() : "default");
        
        OtpGenerationResult rateLimitCheck = otpRateLimiter.checkRateLimit(rateLimitKey);
        if (rateLimitCheck != null) {
            LOGGER.warn("Google OAuth rate limit exceeded for client: {}", googleTokenDTO.getClientId());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(AuthResponseDTO.failed(null, rateLimitCheck.getMessage()));
        }

        OtpGenerationResult attemptCheck = otpRateLimiter.checkAndIncrementAttempts(rateLimitKey);
        if (attemptCheck != null) {
            LOGGER.warn("Google OAuth max attempts exceeded for client: {}", googleTokenDTO.getClientId());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(AuthResponseDTO.failed(null, "Maximum authentication attempts exceeded. Please try again later"));
        }

        Map<String, Object> googleUserInfo = googleOAuthService.verifyAndExtractUserInfo(googleTokenDTO.getIdToken());
        if (MapUtils.isEmpty(googleUserInfo)) {
            LOGGER.warn("Google OAuth authentication failed: Invalid ID token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(AuthResponseDTO.failed(null, "Invalid Google ID token"));
        }

        try {
            User user = userService.findOrCreateGoogleOAuthUser(googleUserInfo);

            if (!Boolean.TRUE.equals(user.getPasswordSet())) {
                LOGGER.info("Google OAuth user {} needs to set password", user.getUsername());
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

            SecurityContextHolder.clearContext();

            JWTToken token = tokenProvider.createAccessTokenAfterVerifiedOtp(user.getUsername(), googleTokenDTO.getRememberMe());
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId(), tokenProvider.getRefreshTokenValidityInSeconds());

            JWTToken fullToken = new JWTToken(
                token.getIdToken(),
                refreshToken.getToken(),
                token.getTokenType(),
                token.getExpiresIn(),
                tokenProvider.getRefreshTokenValidityInSeconds()
            );

            otpRateLimiter.recordRateLimitTimestamp(rateLimitKey);
            otpRateLimiter.resetAttempts(rateLimitKey);
            otpAuditService.persistAuditEntry(user.getUsername());

            AuthResponseDTO response = AuthResponseDTO.success(user.getUsername(), fullToken, googleTokenDTO.getRememberMe())
                .withContext(googleTokenDTO.getRememberMe(), googleTokenDTO.getClientId(), googleTokenDTO.getDeviceId());

            LOGGER.info("Google OAuth authentication successful for user: {}", user.getUsername());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            LOGGER.error("Google OAuth authentication failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponseDTO.failed(null, "Google OAuth authentication failed"));
        }
    }
}
