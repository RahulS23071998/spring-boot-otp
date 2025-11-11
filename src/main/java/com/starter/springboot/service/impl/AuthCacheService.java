package com.starter.springboot.service.impl;

import com.starter.springboot.service.IAuthCacheService;
import com.starter.springboot.service.IRedisTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;

/**
 * Implementation of authentication cache management service.
 * Handles clearing of cached authentication data and token whitelists.
 */
@Service
public class AuthCacheService implements IAuthCacheService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthCacheService.class);

    private final StringRedisTemplate redisTemplate;
    private final IRedisTokenService redisTokenService;

    // Cache key prefix used in OtpAwareAuthenticationProvider
    private static final String AUTH_CACHE_KEY_PREFIX = "auth:user:";

    public AuthCacheService(StringRedisTemplate redisTemplate, IRedisTokenService redisTokenService) {
        this.redisTemplate = redisTemplate;
        this.redisTokenService = redisTokenService;
    }

    @Override
    public void clearAuthCacheForUser(String username) {
        if (Objects.isNull(username) || username.isEmpty()) {
            LOGGER.warn("Cannot clear auth cache for empty username");
            return;
        }

        try {
            String cacheKey = AUTH_CACHE_KEY_PREFIX + username.toLowerCase();
            Boolean deleted = redisTemplate.delete(cacheKey);
            if (deleted) {
                LOGGER.info("Cleared authentication cache for user: {}", username);
            } else {
                LOGGER.debug("No authentication cache found for user: {}", username);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to clear authentication cache for user {}: {}", username, e.getMessage());
        }
    }

    @Override
    public void clearAllAuthCache() {
        try {
            // Get all keys matching the auth cache pattern
            Set<String> keys = redisTemplate.keys(AUTH_CACHE_KEY_PREFIX + "*");
            if (Objects.nonNull(keys) && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                LOGGER.info("Cleared authentication cache for {} users", keys.size());
            } else {
                LOGGER.debug("No authentication cache found to clear");
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to clear all authentication caches: {}", e.getMessage());
        }
    }

    @Override
    public void clearAllCachesForUser(String username, Long userId) {
        // Clear authentication cache
        clearAuthCacheForUser(username);

        // Clear token whitelist
        if (Objects.nonNull(userId)) {
            try {
                redisTokenService.removeWhitelist(userId);
                LOGGER.info("Cleared token whitelist for user: {} (ID: {})", username, userId);
            } catch (Exception e) {
                LOGGER.warn("Failed to remove token whitelist for user {} (ID: {}): {}",
                        username, userId, e.getMessage());
            }
        }
    }
}