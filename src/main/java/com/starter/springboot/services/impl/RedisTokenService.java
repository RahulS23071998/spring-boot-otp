package com.starter.springboot.services.impl;

import com.starter.springboot.services.IRedisTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
public class RedisTokenService implements IRedisTokenService {
    private static final Logger log = LoggerFactory.getLogger(RedisTokenService.class);

    private final StringRedisTemplate redisTemplate;

    public RedisTokenService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String userKey(Long userId) {
        return "whitelist:" + userId;
    }

    /**
     * Register a token jti for a user in Redis whitelist with given ttlSeconds.
     * This will overwrite any existing jti for the user (single active jti per user).
     */
    @Override
    public void registerJti(Long userId, String jti, long ttlSeconds) {
        try {
            String key = userKey(userId);
            redisTemplate.opsForValue().set(key, jti, ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Failed to register jti for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Check if the provided jti matches the jti stored in Redis whitelist for the user.
     */
    @Override
    public boolean isJtiWhitelisted(Long userId, String jti) {
        try {
            String key = userKey(userId);
            String stored = redisTemplate.opsForValue().get(key);
            if (Objects.isNull(stored)) return false;
            return stored.equals(jti);
        } catch (Exception e) {
            log.error("Failed to check jti whitelist for user {}: {}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * Remove whitelist entry for a user (useful on password change or logout).
     */
    @Override
    public void removeWhitelist(Long userId) {
        try {
            redisTemplate.delete(userKey(userId));
        } catch (Exception e) {
            log.error("Failed to remove whitelist for user {}: {}", userId, e.getMessage());
        }
    }
}
