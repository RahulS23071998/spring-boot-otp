package com.starter.springboot.controller;

import com.starter.springboot.constants.ApplicationConstants;
import com.starter.springboot.constants.OtpConstants;
import com.starter.springboot.constants.SecurityConstants;
import com.starter.springboot.entity.RefreshToken;
import com.starter.springboot.entity.User;
import com.starter.springboot.repository.UserRepository;
import com.starter.springboot.dto.OtpValidationResult;
import com.starter.springboot.dto.OtpValidationStatus;
import com.starter.springboot.dto.AuthResponseDTO;
import com.starter.springboot.dto.LoginDTO;
import com.starter.springboot.dto.RefreshTokenRequestDTO;
import com.starter.springboot.dto.VerifyTokenRequestDTO;
import com.starter.springboot.exception.OtpRequiredException;
import com.starter.springboot.security.jwt.JWTToken;
import com.starter.springboot.security.jwt.TokenCreationResponse;
import com.starter.springboot.security.jwt.TokenProvider;
import com.starter.springboot.service.IOtpService;
import com.starter.springboot.service.IRefreshTokenService;
import com.starter.springboot.service.LocalizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * REST Controller for handling authentication and OTP verification.
 * Provides endpoints for user login with OTP-based authentication.
 */
@RestController
@RequestMapping(ApplicationConstants.AUTH_ENDPOINT)
@Validated
@Tag(name = "Authentication", description = "Authentication and OTP verification endpoints")
public class AuthenticationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationController.class);

    private final TokenProvider tokenProvider;

    private final IOtpService otpService;

    private final IRefreshTokenService refreshTokenService;

    private final UserRepository userRepository;

    private final AuthenticationManager authenticationManager;

    private final LocalizationService localizationService;

    public AuthenticationController(TokenProvider tokenProvider,
                                    IOtpService otpService,
                                    IRefreshTokenService refreshTokenService,
                                    UserRepository userRepository,
                                    AuthenticationManager authenticationManager,
                                    LocalizationService localizationService) {
        this.tokenProvider = tokenProvider;
        this.otpService = otpService;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.localizationService = localizationService;
    }

    @PostMapping(value = ApplicationConstants.AUTHENTICATE_ENDPOINT)
    @Operation(summary = "Authenticate user with credentials", 
        description = "Authenticate a user with username and password. If OTP is enabled for the user, " +
                      "the response will indicate that OTP verification is required. An OTP will be sent to the user's email.")
    @RequestBody(description = "Login credentials with optional device/client information",
        content = @Content(schema = @Schema(implementation = LoginDTO.class),
            examples = @ExampleObject(value = """
                {
                  "username": "admin",
                  "password": "nimda",
                  "rememberMe": true,
                  "clientId": "web-portal",
                  "deviceId": "device-1234"
                }
                """)))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentication successful, JWT token provided or OTP required",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = AuthResponseDTO.class),
                examples = @ExampleObject(value = """
                    {
                      "username": "admin",
                      "success": true,
                      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                      "rememberMe": false,
                      "clientId": "mobile-app",
                      "deviceId": "device-001"
                    }
                    """))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials or OTP not verified",
            content = @Content(mediaType = "application/json", 
                examples = @ExampleObject(value = """
                    {
                      "username": "admin",
                      "success": false,
                      "message": "Invalid credentials"
                    }
                    """))),
        @ApiResponse(responseCode = "403", description = "Account locked or authentication failed",
            content = @Content(mediaType = "application/json", 
                examples = @ExampleObject(value = """
                    {
                      "username": "admin",
                      "success": false,
                      "message": "Account locked"
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Invalid request format or validation error")
    })
    public ResponseEntity<AuthResponseDTO> authorize(
        @Valid @org.springframework.web.bind.annotation.RequestBody LoginDTO loginDTO) {
        LOGGER.info("Authentication attempt for user: {}", loginDTO.getUsername());

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
            loginDTO.getUsername(), loginDTO.getPassword()
        );
        try {
            Authentication authentication = this.authenticationManager.authenticate(authenticationToken);
            TokenCreationResponse createResponse = tokenProvider.createToken(authentication, loginDTO.getRememberMe());

            SecurityContextHolder.getContext().setAuthentication(authentication);

            LOGGER.info("Authentication completed for user: {}", loginDTO.getUsername());
            AuthResponseDTO response = AuthResponseDTO.fromTokenCreation(loginDTO.getUsername(), createResponse)
                .withContext(loginDTO.getRememberMe(), loginDTO.getClientId(), loginDTO.getDeviceId());
            return ResponseEntity
                .status(createResponse.status())
                .body(response);
        } catch (OtpRequiredException ex) {
            LOGGER.info("OTP required for user: {}", loginDTO.getUsername());
            throw ex;
        } catch (BadCredentialsException badCredentialsException) {
            LOGGER.warn("Authentication failed for user: {} due to bad credentials", loginDTO.getUsername());
            AuthResponseDTO body = AuthResponseDTO.failed(loginDTO.getUsername(), localizationService.getMessage("auth.invalid_credentials")).withContext(loginDTO.getRememberMe(), loginDTO.getClientId(), loginDTO.getDeviceId());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        } catch (AuthenticationException exception) {
            LOGGER.warn("Authentication failed for user: {}: {}", loginDTO.getUsername(), exception.getMessage());
            AuthResponseDTO body = AuthResponseDTO.failed(loginDTO.getUsername(), exception.getMessage()).withContext(loginDTO.getRememberMe(), loginDTO.getClientId(), loginDTO.getDeviceId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
        }
    }

    @PostMapping(value = ApplicationConstants.VERIFY_ENDPOINT)
    @Operation(summary = "Verify OTP and generate JWT token",
        description = "Verify the One-Time Password (OTP) sent to user's email. Upon successful verification, " +
                      "a JWT token will be generated for authenticated API requests. The OTP is valid for a limited time period.")
    @RequestBody(description = "OTP verification request with username and OTP code",
        content = @Content(schema = @Schema(implementation = VerifyTokenRequestDTO.class),
            examples = @ExampleObject(value = """
                {
                  "username": "admin",
                  "otp": 123456,
                  "rememberMe": true,
                  "clientId": "mobile-app",
                  "deviceId": "device-001"
                }
                """)))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OTP verified successfully, JWT token provided",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = AuthResponseDTO.class),
                examples = @ExampleObject(value = """
                    {
                      "username": "admin",
                      "success": true,
                      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huLmRvZSIsImlhdCI6MTYzNDU2NzIwMH0...",
                      "rememberMe": false
                    }
                    """))),
        @ApiResponse(responseCode = "401", description = "Invalid OTP",
            content = @Content(mediaType = "application/json", 
                examples = @ExampleObject(value = """
                    {
                      "username": "admin",
                      "success": false,
                      "message": "Invalid OTP"
                    }
                    """))),
        @ApiResponse(responseCode = "423", description = "Account locked due to multiple failed OTP attempts",
            content = @Content(mediaType = "application/json", 
                examples = @ExampleObject(value = """
                    {
                      "username": "admin",
                      "success": false,
                      "message": "Account locked due to multiple failed OTP attempts"
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Invalid request format or missing required fields")
    })
    public ResponseEntity<AuthResponseDTO> verifyOtp(
        @Valid @org.springframework.web.bind.annotation.RequestBody VerifyTokenRequestDTO verifyTokenRequest) {
        String username = verifyTokenRequest.getUsername();
        Integer otp = verifyTokenRequest.getOtp();
        Boolean rememberMe = verifyTokenRequest.getRememberMe();

        OtpValidationResult validationResult = otpService.validateOTP(username, otp);
        if (!validationResult.isSuccess()) {
            LOGGER.warn("OTP validation failed for user: {} with status {}", username, validationResult.getStatus());
            HttpStatus status = validationResult.getStatus() == OtpValidationStatus.LOCKED ? HttpStatus.LOCKED : HttpStatus.UNAUTHORIZED;
            String message = validationResult.getStatus() == OtpValidationStatus.LOCKED
                ? localizationService.getMessage("auth.locked_otp")
                : localizationService.getMessage("auth.invalid_otp");
            return ResponseEntity.status(status)
                .body(AuthResponseDTO.failed(username, message)
                    .withContext(rememberMe, verifyTokenRequest.getClientId(), verifyTokenRequest.getDeviceId()));
        }

        JWTToken token = tokenProvider.createTokenAfterVerifiedOtp(username, rememberMe);
        AuthResponseDTO response = AuthResponseDTO.success(username, token, rememberMe)
            .withContext(rememberMe, verifyTokenRequest.getClientId(), verifyTokenRequest.getDeviceId());

        LOGGER.info("OTP verified successfully for user: {}", username);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = ApplicationConstants.REFRESH_ENDPOINT)
    @Operation(summary = "Refresh access token using refresh token",
        description = "Exchange a valid refresh token for a new access token and refresh token pair. " +
                     "The old refresh token is revoked and a new one is issued for security.")
    @RequestBody(description = "Refresh token request",
        content = @Content(schema = @Schema(implementation = RefreshTokenRequestDTO.class),
            examples = @ExampleObject(value = """
                {
                  "refreshToken": "abc123-def456-ghi789-jkl012"
                }
                """)))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token refreshed successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = AuthResponseDTO.class),
                examples = @ExampleObject(value = """
                    {
                      "username": "admin",
                      "success": true,
                      "token": {
                        "id_token": "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiIyMjA4NTdkMS1kNjM5LTRlYWMtOWU...",
                        "refresh_token": "aed31a06-dc69-4075-a3e8-156c90188c65-db4ba010...",
                        "token_type": "Bearer",
                        "expires_in": 3600,
                        "refresh_token_expires_in": 604800
                      },
                      "issued_at": "2025-11-11T18:35:26.000Z"
                    }
                    """))),
        @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "username": null,
                      "success": false,
                      "message": "Invalid refresh token"
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Invalid request format")
    })
    public ResponseEntity<AuthResponseDTO> refreshToken(
        @Valid @org.springframework.web.bind.annotation.RequestBody RefreshTokenRequestDTO refreshRequest) {

        try {
            // Validate refresh token
            RefreshToken refreshToken = refreshTokenService.validateRefreshToken(refreshRequest.refreshToken());

            // Get user details
            User user = userRepository.findById(refreshToken.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Create new tokens (this will rotate the refresh token)
            RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(
                    refreshRequest.refreshToken(), user.getId(),
                    tokenProvider.getRefreshTokenValidityInSeconds()
            );

            // Create new access token (bypass OTP check for refresh)
            JWTToken token = tokenProvider.createAccessTokenAfterVerifiedOtp(user.getUsername(), false);

            // Create the final token with access token and rotated refresh token
            JWTToken updatedToken = new JWTToken(
                    token.getIdToken(),
                    newRefreshToken.getToken(),
                    token.getTokenType(),
                    token.getExpiresIn(),
                    tokenProvider.getRefreshTokenValidityInSeconds()
            );

            AuthResponseDTO response = AuthResponseDTO.success(user.getUsername(), updatedToken, false);
            LOGGER.info("Token refreshed successfully for user: {}", user.getUsername());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            LOGGER.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(AuthResponseDTO.failed(null, localizationService.getMessage("auth.invalid_refresh_token")));
        }
    }
}
