package com.starter.springboot.controller;

import com.starter.springboot.constants.ApplicationConstants;
import com.starter.springboot.dto.AuthResponseDTO;
import com.starter.springboot.dto.OtpValidationResult;
import com.starter.springboot.dto.OtpValidationStatus;
import com.starter.springboot.dto.VerifyTokenRequestDTO;
import com.starter.springboot.security.jwt.ITokenProvider;
import com.starter.springboot.security.jwt.JWTToken;
import com.starter.springboot.service.IOtpService;
import com.starter.springboot.service.LocalizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping(ApplicationConstants.AUTH_ENDPOINT)
@Validated
@Tag(name = "OTP Verification", description = "OTP verification endpoints")
public class OtpVerificationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(OtpVerificationController.class);

    private final IOtpService otpService;
    private final ITokenProvider tokenProvider;
    private final LocalizationService localizationService;

    public OtpVerificationController(IOtpService otpService,
                                     ITokenProvider tokenProvider,
                                     LocalizationService localizationService) {
        this.otpService = otpService;
        this.tokenProvider = tokenProvider;
        this.localizationService = localizationService;
    }

    @PostMapping(value = ApplicationConstants.VERIFY_ENDPOINT)
    @Operation(summary = "Verify OTP and generate JWT token",
        description = "Verify the One-Time Password (OTP) sent to user's email. Upon successful verification, " +
                      "a JWT token will be generated for authenticated API requests. The OTP is valid for a limited time period.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "OTP verification request with username and OTP code",
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
    public ResponseEntity<AuthResponseDTO> verifyOtp(@Valid @RequestBody VerifyTokenRequestDTO verifyTokenRequest) {
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
}
