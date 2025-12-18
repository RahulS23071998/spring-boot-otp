package com.starter.springboot.service.impl;

import com.starter.springboot.constants.OtpConstants;
import com.starter.springboot.service.IOtpProperties;
import com.starter.springboot.service.IOtpRateLimiter;
import com.starter.springboot.dto.OtpGenerationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of OTP rate limiter.
 */
@Service
public class OtpRateLimiterImpl implements IOtpRateLimiter {

    private final Logger LOGGER = LoggerFactory.getLogger(OtpRateLimiterImpl.class);

    private final StringRedisTemplate redisTemplate;
    private final IOtpProperties otpProperties;

    public OtpRateLimiterImpl(StringRedisTemplate redisTemplate, IOtpProperties otpProperties) {
        this.redisTemplate = redisTemplate;
        this.otpProperties = otpProperties;
    }

    @Override
    public OtpGenerationResult checkRateLimit(String key) {
        String rateLimitKey = OtpConstants.OTP_REDIS_KEY_PREFIX + key + OtpConstants.RATE_LIMIT_KEY_SUFFIX;
        String lastSentTime = redisTemplate.opsForValue().get(rateLimitKey);

        if (Objects.nonNull(lastSentTime)) {
            try {
                long lastSentMillis = Long.parseLong(lastSentTime);
                long currentTimeMillis = System.currentTimeMillis();
                long timeDiffSeconds = (currentTimeMillis - lastSentMillis) / 1000;

                if (timeDiffSeconds < OtpConstants.OTP_RATE_LIMIT_SECONDS) {
                    long remainingSeconds = OtpConstants.OTP_RATE_LIMIT_SECONDS - timeDiffSeconds;
                    LOGGER.warn("OTP rate limit exceeded for key: {}. Last sent {} seconds ago, {} seconds remaining", key, timeDiffSeconds, remainingSeconds);
                    // If this is a google oauth key, return an oauth-friendly message
                    if (key.startsWith("google-oauth:")) {
                        return OtpGenerationResult.googleRateLimited((int) remainingSeconds);
                    }
                    return OtpGenerationResult.rateLimited((int) remainingSeconds);
                }
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid timestamp format in Redis for key: {}", rateLimitKey);
                // Continue if timestamp is corrupted
            }
        }
        return null; // Allowed
    }

    @Override
    public OtpGenerationResult checkAndIncrementAttempts(String key) {
        String attemptsKey = OtpConstants.OTP_REDIS_KEY_PREFIX + key + OtpConstants.ATTEMPTS_KEY_SUFFIX;
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey, 1);
        if (attempts == 1) {
            redisTemplate.expire(attemptsKey, otpProperties.getAttemptWindowMinutes(), TimeUnit.MINUTES);
        }
        if (attempts > otpProperties.getMaxAttempts()) {
            LOGGER.warn("OTP request limit exceeded for key: {}", key);
            return OtpGenerationResult.maxAttemptsExceeded();
        }
        return null; // Allowed
    }

    @Override
    public void recordRateLimitTimestamp(String key) {
        String rateLimitKey = OtpConstants.OTP_REDIS_KEY_PREFIX + key + OtpConstants.RATE_LIMIT_KEY_SUFFIX;
        redisTemplate.opsForValue().set(rateLimitKey, String.valueOf(System.currentTimeMillis()));
        redisTemplate.expire(rateLimitKey, OtpConstants.OTP_RATE_LIMIT_SECONDS + 5, TimeUnit.SECONDS);
    }

    @Override
    public void resetAttempts(String key) {
        String attemptsKey = OtpConstants.OTP_REDIS_KEY_PREFIX + key + OtpConstants.ATTEMPTS_KEY_SUFFIX;
        redisTemplate.delete(attemptsKey);
        LOGGER.debug("Reset attempts counter for key: {}", key);
    }
}