package com.starter.springboot.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service for scheduled cleanup of expired tokens.
 * Handles removal of expired tokens from both database and Redis.
 */
@Service
public class TokenCleanupService {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupService.class);

    private final RefreshTokenService refreshTokenService;

    public TokenCleanupService(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * Scheduled job to cleanup expired refresh tokens from database.
     * Runs every hour (3600000 milliseconds).
     */
    @Scheduled(fixedRate = 3600000, initialDelay = 600000)
    public void cleanupExpiredRefreshTokens() {
        try {
            log.info("Starting scheduled cleanup of expired refresh tokens");
            refreshTokenService.cleanupExpiredTokens();
            log.info("Completed scheduled cleanup of expired refresh tokens");
        } catch (Exception e) {
            log.error("Error during token cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Scheduled job to cleanup expired refresh tokens from database.
     * Runs every 6 hours (21600000 milliseconds) - alternative schedule for less frequent cleanup.
     * Can be enabled by setting property: token.cleanup.frequency=6h
     */
    @Scheduled(fixedRate = 21600000, initialDelay = 300000)
    public void cleanupExpiredRefreshTokensLessFrequent() {
        try {
            log.debug("Running less frequent refresh token cleanup");
            refreshTokenService.cleanupExpiredTokens();
        } catch (Exception e) {
            log.error("Error during less frequent token cleanup: {}", e.getMessage(), e);
        }
    }
}
