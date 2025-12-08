package com.starter.springboot.service.impl;

import com.starter.springboot.service.IAuthCacheService;
import com.starter.springboot.service.IRefreshTokenService;
import com.starter.springboot.service.IUserTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Facade service for managing user tokens and authentication caches.
 * Simplifies token and cache management by providing a single interface
 * to multiple token-related services.
 */
@Service
public class UserTokenService implements IUserTokenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserTokenService.class);

    private final IAuthCacheService authCacheService;
    private final IRefreshTokenService refreshTokenService;

    public UserTokenService(IAuthCacheService authCacheService,
                            IRefreshTokenService refreshTokenService) {
        this.authCacheService = authCacheService;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * Clears all authentication tokens and caches for a specific user.
     * This ensures the user must re-authenticate after password changes.
     *
     * @param username the username of the user
     * @param userId the unique identifier of the user
     */
    @Override
    @Transactional
    public void clearAllUserTokensAndCaches(String username, Long userId) {
        try {
            authCacheService.clearAllCachesForUser(username, userId);
            refreshTokenService.revokeAllUserRefreshTokens(userId);
            LOGGER.info("Cleared authentication cache and refresh tokens for user: {}", username);
        } catch (Exception e) {
            LOGGER.warn("Failed to clear tokens and caches for user {}: {}", username, e.getMessage());
            throw e;
        }
    }
}
