package com.starter.springboot.service.impl;

import com.starter.springboot.service.ITemporaryPasswordTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class TemporaryPasswordTokenService implements ITemporaryPasswordTokenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemporaryPasswordTokenService.class);
    private static final long TOKEN_VALIDITY_MINUTES = 15;
    private static final String TOKEN_PREFIX = "password-reset:";
    private static final String TOKEN_REVERSE_PREFIX = "password-reset-token:";

    private final StringRedisTemplate redisTemplate;

    public TemporaryPasswordTokenService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String generateTemporaryToken(String username) {
        String token = UUID.randomUUID().toString();
        String usernameKey = TOKEN_PREFIX + username;
        String tokenKey = TOKEN_REVERSE_PREFIX + token;
        
        try {
            redisTemplate.opsForValue().set(
                usernameKey,
                token,
                TOKEN_VALIDITY_MINUTES,
                TimeUnit.MINUTES
            );
            redisTemplate.opsForValue().set(
                tokenKey,
                username,
                TOKEN_VALIDITY_MINUTES,
                TimeUnit.MINUTES
            );
            LOGGER.info("Generated temporary password token for user: {}", username);
            return token;
        } catch (Exception e) {
            LOGGER.error("Failed to generate temporary token for user {}: {}", username, e.getMessage());
            throw new RuntimeException("Failed to generate temporary token", e);
        }
    }

    @Override
    public boolean validateTemporaryToken(String username, String token) {
        try {
            String key = TOKEN_PREFIX + username;
            String storedToken = redisTemplate.opsForValue().get(key);
            
            if (storedToken == null) {
                LOGGER.warn("No temporary token found for user: {}", username);
                return false;
            }
            
            boolean isValid = storedToken.equals(token);
            if (!isValid) {
                LOGGER.warn("Invalid temporary token for user: {}", username);
            }
            return isValid;
        } catch (Exception e) {
            LOGGER.error("Error validating temporary token for user {}: {}", username, e.getMessage());
            return false;
        }
    }

    @Override
    public void invalidateTemporaryToken(String username) {
        try {
            String usernameKey = TOKEN_PREFIX + username;
            String token = redisTemplate.opsForValue().get(usernameKey);
            
            redisTemplate.delete(usernameKey);
            if (token != null) {
                String tokenKey = TOKEN_REVERSE_PREFIX + token;
                redisTemplate.delete(tokenKey);
            }
            LOGGER.info("Invalidated temporary token for user: {}", username);
        } catch (Exception e) {
            LOGGER.warn("Failed to invalidate temporary token for user {}: {}", username, e.getMessage());
        }
    }

    @Override
    public String getUsernameFromToken(String token) {
        try {
            String tokenKey = TOKEN_REVERSE_PREFIX + token;
            String username = redisTemplate.opsForValue().get(tokenKey);
            
            if (username == null) {
                LOGGER.warn("No username found for token or token expired");
                return null;
            }
            
            LOGGER.info("Retrieved username from temporary token");
            return username;
        } catch (Exception e) {
            LOGGER.error("Error retrieving username from temporary token: {}", e.getMessage());
            return null;
        }
    }
}
