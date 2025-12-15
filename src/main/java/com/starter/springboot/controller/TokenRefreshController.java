package com.starter.springboot.controller;

import com.starter.springboot.constants.ApplicationConstants;
import com.starter.springboot.dto.AuthResponseDTO;
import com.starter.springboot.dto.RefreshTokenRequestDTO;
import com.starter.springboot.entity.RefreshToken;
import com.starter.springboot.entity.User;
import com.starter.springboot.exception.InvalidRefreshTokenException;
import com.starter.springboot.security.jwt.ITokenProvider;
import com.starter.springboot.security.jwt.JWTToken;
import com.starter.springboot.service.IRefreshTokenService;
import com.starter.springboot.service.IUserService;
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
@Tag(name = "Token Management", description = "Token refresh and management endpoints")
public class TokenRefreshController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenRefreshController.class);

    private final IRefreshTokenService refreshTokenService;
    private final ITokenProvider tokenProvider;
    private final IUserService userService;
    private final LocalizationService localizationService;

    public TokenRefreshController(IRefreshTokenService refreshTokenService,
                                  ITokenProvider tokenProvider,
                                  IUserService userService,
                                  LocalizationService localizationService) {
        this.refreshTokenService = refreshTokenService;
        this.tokenProvider = tokenProvider;
        this.userService = userService;
        this.localizationService = localizationService;
    }

    @PostMapping(value = ApplicationConstants.REFRESH_ENDPOINT)
    @Operation(summary = "Refresh access token using refresh token",
        description = "Exchange a valid refresh token for a new access token and refresh token pair. " +
                     "The old refresh token is revoked and a new one is issued for security.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Refresh token request",
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
    public ResponseEntity<AuthResponseDTO> refreshToken(@Valid @RequestBody RefreshTokenRequestDTO refreshRequest) {
        try {
            RefreshToken refreshToken = refreshTokenService.validateRefreshToken(refreshRequest.refreshToken());

            User user = userService.findUserById(refreshToken.getUserId());

            RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(
                    refreshRequest.refreshToken(), user.getId(),
                    tokenProvider.getRefreshTokenValidityInSeconds()
            );

            JWTToken token = tokenProvider.createAccessTokenAfterVerifiedOtp(user.getUsername(), false);

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

        } catch (InvalidRefreshTokenException e) {
            LOGGER.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(AuthResponseDTO.failed(null, localizationService.getMessage("auth.invalid_refresh_token")));
        } catch (Exception e) {
            LOGGER.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(AuthResponseDTO.failed(null, localizationService.getMessage("auth.invalid_refresh_token")));
        }
    }
}
