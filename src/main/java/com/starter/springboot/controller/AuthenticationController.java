package com.starter.springboot.controller;

import com.starter.springboot.constants.ApplicationConstants;
import com.starter.springboot.constants.OtpConstants;
import com.starter.springboot.constants.SecurityConstants;
import com.starter.springboot.dto.OtpValidationResult;
import com.starter.springboot.dto.OtpValidationStatus;
import com.starter.springboot.dto.AuthResponseDTO;
import com.starter.springboot.dto.LoginDTO;
import com.starter.springboot.dto.VerifyTokenRequestDTO;
import com.starter.springboot.exception.OtpRequiredException;
import com.starter.springboot.security.jwt.JWTToken;
import com.starter.springboot.security.jwt.TokenCreationResponse;
import com.starter.springboot.security.jwt.TokenProvider;
import com.starter.springboot.service.IOtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    private final AuthenticationManager authenticationManager;

    public AuthenticationController(TokenProvider tokenProvider,
                                    IOtpService otpService,
                                    AuthenticationManager authenticationManager) {
        this.tokenProvider = tokenProvider;
        this.otpService = otpService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping(value = ApplicationConstants.AUTHENTICATE_ENDPOINT)
    @Operation(summary = "Authenticate user with credentials", 
        description = "Authenticate a user with username and password. If OTP is enabled for the user, " +
                      "the response will indicate that OTP verification is required. An OTP will be sent to the user's email.")
    @RequestBody(description = "Login credentials with optional device/client information",
        content = @Content(schema = @Schema(implementation = LoginDTO.class),
            examples = @ExampleObject(value = """
                {
                  "username": "john.doe",
                  "password": "SecurePass123!",
                  "rememberMe": false,
                  "clientId": "mobile-app",
                  "deviceId": "device-001"
                }
                """)))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentication successful, JWT token provided or OTP required",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = AuthResponseDTO.class),
                examples = @ExampleObject(value = """
                    {
                      "username": "john.doe",
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
                      "username": "john.doe",
                      "success": false,
                      "message": "Invalid credentials"
                    }
                    """))),
        @ApiResponse(responseCode = "403", description = "Account locked or authentication failed",
            content = @Content(mediaType = "application/json", 
                examples = @ExampleObject(value = """
                    {
                      "username": "john.doe",
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
            AuthResponseDTO body = AuthResponseDTO.failed(loginDTO.getUsername(), SecurityConstants.INVALID_CREDENTIALS_MESSAGE).withContext(loginDTO.getRememberMe(), loginDTO.getClientId(), loginDTO.getDeviceId());
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
                  "username": "john.doe",
                  "otp": 123456,
                  "rememberMe": false,
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
                      "username": "john.doe",
                      "success": true,
                      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huLmRvZSIsImlhdCI6MTYzNDU2NzIwMH0...",
                      "rememberMe": false
                    }
                    """))),
        @ApiResponse(responseCode = "401", description = "Invalid OTP",
            content = @Content(mediaType = "application/json", 
                examples = @ExampleObject(value = """
                    {
                      "username": "john.doe",
                      "success": false,
                      "message": "Invalid OTP"
                    }
                    """))),
        @ApiResponse(responseCode = "423", description = "Account locked due to multiple failed OTP attempts",
            content = @Content(mediaType = "application/json", 
                examples = @ExampleObject(value = """
                    {
                      "username": "john.doe",
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
                ? OtpConstants.LOCKED_OTP_MESSAGE
                : OtpConstants.INVALID_OTP_MESSAGE;
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
}
