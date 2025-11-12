package com.starter.springboot.service.impl;

import com.starter.springboot.service.IRefreshTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduled service for cleaning up expired refresh tokens.
 */
@Service
public class RefreshTokenCleanupService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupService.class);

    private final IRefreshTokenService refreshTokenService;

    public RefreshTokenCleanupService(IRefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * Clean up expired refresh tokens daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredRefreshTokens() {
        log.info("Starting scheduled cleanup of expired refresh tokens");
        try {
            refreshTokenService.cleanupExpiredTokens();
            log.info("Completed scheduled cleanup of expired refresh tokens");
        } catch (Exception e) {
            log.error("Failed to cleanup expired refresh tokens: {}", e.getMessage(), e);
        }
    }
}