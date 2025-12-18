package com.starter.springboot.controller;

import com.starter.springboot.constants.ApplicationConstants;
import com.starter.springboot.dto.AuthResponseDTO;
import com.starter.springboot.entity.RefreshToken;
import com.starter.springboot.entity.User;
import com.starter.springboot.security.DomainUserDetails;
import com.starter.springboot.service.ILogoutService;
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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Objects;

/**
 * REST Controller for handling user logout operations.
 * Manages session termination, token revocation, and cache cleanup.
 */
@RestController
@RequestMapping(ApplicationConstants.AUTH_ENDPOINT)
@Validated
@Tag(name = "Authentication", description = "User authentication endpoints")
public class LogoutController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogoutController.class);

    private final ILogoutService logoutService;
    private final LocalizationService localizationService;
    private final IUserService userService;
    private final IRefreshTokenService refreshTokenService;

    public LogoutController(ILogoutService logoutService,
                           LocalizationService localizationService,
                           com.starter.springboot.service.IUserService userService,
                           IRefreshTokenService refreshTokenService) {
        this.logoutService = logoutService;
        this.localizationService = localizationService;
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Logout user",
        description = "Logout the authenticated user. Revokes refresh tokens, invalidates JWT, clears caches, " +
                      "and terminates the session. After logout, the user must authenticate again.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Optional refresh token to revoke")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logout successful, all tokens and sessions invalidated",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = AuthResponseDTO.class),
                examples = @ExampleObject(value = """
                    {
                      "status": "SUCCESS",
                      "message": "Logout successful. All tokens and sessions have been invalidated.",
                      "issued_at": "2025-11-11T18:35:26.000Z"
                    }
                    """))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - user not authenticated",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "status": "FAILED",
                      "message": "User not authenticated"
                    }
                    """))),
        @ApiResponse(responseCode = "500", description = "Server error during logout")
    })
    public ResponseEntity<AuthResponseDTO> logout(
            @RequestParam(value = "refreshToken", required = false) String refreshToken) {
        
        LOGGER.info("Logout request received");

        try {
            // If a refresh token is supplied, try to validate it first (explicit logout by refresh token)
            if (Objects.nonNull(refreshToken) && !refreshToken.isBlank()) {
                try {
                    RefreshToken rt = refreshTokenService.validateRefreshToken(refreshToken);
                    // Valid refresh token -> logout the owner and return success
                    User user = userService.findUserById(rt.getUserId());
                    if (user != null) {
                        logoutService.logout(user.getId(), user.getUsername(), refreshToken);
                        LOGGER.info("Logout by refresh token completed for user: {}", user.getUsername());
                        return ResponseEntity.ok(
                            new AuthResponseDTO(
                                user.getUsername(),
                                ApplicationConstants.SUCCESS_STATUS,
                                localizationService.getMessage("auth.logout_successful"),
                                Boolean.FALSE,
                                null,
                                Instant.now(),
                                null,
                                null,
                                null
                            )
                        );
                    }
                } catch (Exception e) {
                    LOGGER.warn("Invalid refresh token supplied for logout: {}", e.getMessage());
                    return ResponseEntity.status(401)
                        .body(AuthResponseDTO.failed(null, localizationService.getMessage("auth.invalid_refresh_token")));
                }
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // Treat anonymous authentication as unauthenticated
            if (Objects.isNull(authentication) || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
                LOGGER.warn("Logout attempt by unauthenticated or anonymous user");
                return ResponseEntity.status(401)
                    .body(AuthResponseDTO.failed(null, localizationService.getMessage("auth.not_authenticated")));
            }

            String username = extractUsername(authentication);
            Long userId = extractUserId(authentication);

            if (Objects.isNull(username)) {
                LOGGER.warn("Cannot extract username from authentication");
                return ResponseEntity.status(401)
                    .body(AuthResponseDTO.failed(null, localizationService.getMessage("auth.not_authenticated")));
            }

            if (Objects.isNull(userId)) {
                try {
                    User user = userService.findUserByUsername(username);
                    userId = user.getId();
                } catch (Exception e) {
                    LOGGER.warn("Could not find user by username: {}", username);
                }
            }

            LOGGER.info("Processing logout for user: {}", username);

            logoutService.logout(userId, username, refreshToken);

            LOGGER.info("Logout completed successfully for user: {}", username);

            return ResponseEntity.ok(
                new AuthResponseDTO(
                    username,
                    ApplicationConstants.SUCCESS_STATUS,
                    localizationService.getMessage("auth.logout_successful"),
                    Boolean.FALSE,
                    null,
                    Instant.now(),
                    null,
                    null,
                    null
                )
            );

        } catch (Exception e) {
            LOGGER.error("Error during logout: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(AuthResponseDTO.failed(null, localizationService.getMessage("auth.logout_error")));
        }
    }

    @PostMapping("/logout-all-sessions")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Logout from all sessions",
        description = "Logout the authenticated user from all sessions/devices. Revokes all refresh tokens, " +
                      "invalidates all JWTs, clears all caches, and terminates all sessions across all devices.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "All sessions logged out successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = AuthResponseDTO.class),
                examples = @ExampleObject(value = """
                    {
                      "status": "SUCCESS",
                      "message": "All sessions have been logged out successfully.",
                      "issued_at": "2025-11-11T18:35:26.000Z"
                    }
                    """))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - user not authenticated"),
        @ApiResponse(responseCode = "500", description = "Server error during logout")
    })
    public ResponseEntity<AuthResponseDTO> logoutAllSessions() {
        
        LOGGER.info("Logout all sessions request received");

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (Objects.isNull(authentication) || !authentication.isAuthenticated()) {
                LOGGER.warn("Logout attempt by unauthenticated user");
                return ResponseEntity.status(401)
                    .body(AuthResponseDTO.failed(null, localizationService.getMessage("auth.not_authenticated")));
            }

            String username = extractUsername(authentication);
            Long userId = extractUserId(authentication);

            if (Objects.isNull(username)) {
                LOGGER.warn("Cannot extract username from authentication");
                return ResponseEntity.status(401)
                    .body(AuthResponseDTO.failed(null, localizationService.getMessage("auth.not_authenticated")));
            }

            if (Objects.isNull(userId)) {
                try {
                    User user = userService.findUserByUsername(username);
                    userId = user.getId();
                } catch (Exception e) {
                    LOGGER.warn("Could not find user by username: {}", username);
                }
            }

            LOGGER.info("Revoking all sessions for user: {}", username);

            logoutService.revokeAllSessions(userId, username);

            LOGGER.info("All sessions revoked successfully for user: {}", username);

            return ResponseEntity.ok(
                new AuthResponseDTO(
                    username,
                    ApplicationConstants.SUCCESS_STATUS,
                    localizationService.getMessage("auth.logout_all_sessions_successful"),
                    Boolean.FALSE,
                    null,
                    Instant.now(),
                    null,
                    null,
                    null
                )
            );

        } catch (Exception e) {
            LOGGER.error("Error during logout all sessions: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(AuthResponseDTO.failed(null, localizationService.getMessage("auth.logout_error")));
        }
    }

    /**
     * Extract username from Authentication object.
     */
    private String extractUsername(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            return (String) principal;
        }
        return null;
    }

    /**
     * Extract userId from DomainUserDetails in Authentication principal.
     * Returns null if userId cannot be extracted.
     */
    private Long extractUserId(Authentication authentication) {
        try {
            Object principal = authentication.getPrincipal();
            if (principal instanceof DomainUserDetails) {
                Long userId = ((DomainUserDetails) principal).getUserId();
                if (Objects.nonNull(userId)) {
                    return userId;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not extract userId from DomainUserDetails: {}", e.getMessage());
        }
        return null;
    }
}
