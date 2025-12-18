package com.starter.springboot.service.impl;

import com.starter.springboot.service.ILoginRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class LoginRateLimiter implements ILoginRateLimiter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginRateLimiter.class);
    private static final String LOGIN_ATTEMPTS_PREFIX = "login:attempts:";

    private final StringRedisTemplate redisTemplate;
    
    @Value("${security.login.max-attempts:5}")
    private int maxAttempts;
    
    @Value("${security.login.window-seconds:60}")
    private int windowSeconds;

    public LoginRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isAllowed(String key) {
        String redisKey = LOGIN_ATTEMPTS_PREFIX + key;
        String attemptsStr = redisTemplate.opsForValue().get(redisKey);
        
        if (attemptsStr != null) {
            int attempts = Integer.parseInt(attemptsStr);
            if (attempts >= maxAttempts) {
                LOGGER.warn("Login rate limit exceeded for key: {}", key);
                return false;
            }
        }
        return true;
    }

    @Override
    public void recordAttempt(String key) {
        String redisKey = LOGIN_ATTEMPTS_PREFIX + key;
        Long attempts = redisTemplate.opsForValue().increment(redisKey);
        
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
        }
    }
}
