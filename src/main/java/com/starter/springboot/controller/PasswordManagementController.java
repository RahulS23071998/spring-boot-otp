package com.starter.springboot.controller;

import com.starter.springboot.dto.SetPasswordDTO;
import com.starter.springboot.dto.SetPasswordResponseDTO;
import com.starter.springboot.service.IPasswordSetupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@Validated
@Tag(name = "Password Management", description = "Password setup and management endpoints")
public class PasswordManagementController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordManagementController.class);

    private final IPasswordSetupService passwordSetupService;

    public PasswordManagementController(IPasswordSetupService passwordSetupService) {
        this.passwordSetupService = passwordSetupService;
    }

    @PostMapping("/set-password")
    @Operation(summary = "Set password for OAuth users",
        description = "Allows OAuth users to set their password after initial registration via Google OAuth. " +
                      "Validates password strength and sends confirmation email. Supports temporary token validation.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password set successfully",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = SetPasswordResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request or validation error",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "FAILED",
                      "message": "Password must contain at least one uppercase letter"
                    }
                    """))),
        @ApiResponse(responseCode = "401", description = "Invalid temporary token",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "FAILED",
                      "message": "Temporary token expired or invalid"
                    }
                    """))),
        @ApiResponse(responseCode = "404", description = "User not found",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "403", description = "Password already set or user not OAuth",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "429", description = "Too many password reset attempts",
            content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<SetPasswordResponseDTO> setPassword(@Valid @RequestBody SetPasswordDTO request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            LOGGER.warn("Password mismatch in set password request");
            return ResponseEntity.badRequest()
                .body(SetPasswordResponseDTO.failed("Passwords do not match"));
        }

        if (StringUtils.hasText(request.getTemporaryToken())) {
            return passwordSetupService.handlePasswordSetWithTemporaryToken(request);
        } else {
            return passwordSetupService.handlePasswordSetWithAuthentication(request);
        }
    }
}
