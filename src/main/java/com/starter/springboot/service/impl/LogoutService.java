package com.starter.springboot.service.impl;

import com.starter.springboot.service.IAuthCacheService;
import com.starter.springboot.service.ILogoutService;
import com.starter.springboot.service.IRedisTokenService;
import com.starter.springboot.service.IRefreshTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Implementation of logout service handling complete user session termination.
 * Manages JWT token invalidation, refresh token revocation, and cache cleanup.
 */
@Service
public class LogoutService implements ILogoutService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogoutService.class);

    private final IRefreshTokenService refreshTokenService;
    private final IRedisTokenService redisTokenService;
    private final IAuthCacheService authCacheService;

    public LogoutService(IRefreshTokenService refreshTokenService,
                        IRedisTokenService redisTokenService,
                        IAuthCacheService authCacheService) {
        this.refreshTokenService = refreshTokenService;
        this.redisTokenService = redisTokenService;
        this.authCacheService = authCacheService;
    }

    @Override
    @Transactional
    public void logout(Long userId, String username, String refreshToken) {
        if (Objects.isNull(userId) || Objects.isNull(username)) {
            LOGGER.warn("Cannot logout with null userId or username");
            return;
        }

        LOGGER.info("Initiating logout for user: {} (ID: {})", username, userId);

        revokeRefreshToken(refreshToken, userId, username);
        revokeJwtWhitelist(userId, username);
        clearAllCaches(userId, username);
        clearSecurityContext();

        LOGGER.info("Logout completed for user: {} (ID: {})", username, userId);
    }

    @Override
    @Transactional
    public void logout(Long userId, String username) {
        logout(userId, username, null);
    }

    @Override
    @Transactional
    public void revokeAllSessions(Long userId, String username) {
        if (Objects.isNull(userId) || Objects.isNull(username)) {
            LOGGER.warn("Cannot revoke all sessions with null userId or username");
            return;
        }

        LOGGER.info("Revoking all sessions for user: {} (ID: {})", username, userId);

        revokeAllRefreshTokens(userId, username);
        revokeJwtWhitelist(userId, username);
        clearAllCaches(userId, username);
        clearSecurityContext();

        LOGGER.info("All sessions revoked for user: {} (ID: {})", username, userId);
    }

    /**
     * Revoke a specific refresh token.
     */
    private void revokeRefreshToken(String refreshToken, Long userId, String username) {
        if (Objects.isNull(refreshToken) || refreshToken.isEmpty()) {
            LOGGER.debug("No refresh token provided for revocation");
            return;
        }

        try {
            refreshTokenService.revokeRefreshToken(refreshToken);
            LOGGER.debug("Revoked refresh token for user: {} (ID: {})", username, userId);
        } catch (Exception e) {
            LOGGER.warn("Failed to revoke refresh token for user {} (ID: {}): {}",
                    username, userId, e.getMessage());
        }
    }

    /**
     * Revoke all refresh tokens for a user.
     */
    private void revokeAllRefreshTokens(Long userId, String username) {
        try {
            refreshTokenService.revokeAllUserRefreshTokens(userId);
            LOGGER.debug("Revoked all refresh tokens for user: {} (ID: {})", username, userId);
        } catch (Exception e) {
            LOGGER.warn("Failed to revoke all refresh tokens for user {} (ID: {}): {}",
                    username, userId, e.getMessage());
        }
    }

    /**
     * Remove JWT token from Redis whitelist.
     * This invalidates the current JWT token for the user.
     */
    private void revokeJwtWhitelist(Long userId, String username) {
        try {
            redisTokenService.removeWhitelist(userId);
            LOGGER.debug("Removed JWT whitelist for user: {} (ID: {})", username, userId);
        } catch (Exception e) {
            LOGGER.warn("Failed to remove JWT whitelist for user {} (ID: {}): {}",
                    username, userId, e.getMessage());
        }
    }

    /**
     * Clear all authentication and token caches for the user.
     */
    private void clearAllCaches(Long userId, String username) {
        try {
            authCacheService.clearAllCachesForUser(username, userId);
            LOGGER.debug("Cleared all caches for user: {} (ID: {})", username, userId);
        } catch (Exception e) {
            LOGGER.warn("Failed to clear caches for user {} (ID: {}): {}",
                    username, userId, e.getMessage());
        }
    }

    /**
     * Clear Spring Security context to remove authentication from the current thread.
     */
    private void clearSecurityContext() {
        try {
            SecurityContextHolder.clearContext();
            LOGGER.debug("Cleared Spring Security context");
        } catch (Exception e) {
            LOGGER.warn("Failed to clear Spring Security context: {}", e.getMessage());
        }
    }
}
