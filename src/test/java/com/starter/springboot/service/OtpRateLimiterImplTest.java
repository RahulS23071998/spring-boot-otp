package com.starter.springboot.service;

import com.starter.springboot.constants.OtpConstants;
import com.starter.springboot.dto.OtpGenerationResult;
import com.starter.springboot.service.impl.OtpRateLimiterImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OtpRateLimiterImpl Tests")
class OtpRateLimiterImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private IOtpProperties otpProperties;

    @InjectMocks
    private OtpRateLimiterImpl rateLimiter;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(otpProperties.getMaxAttempts()).thenReturn(3);
        lenient().when(otpProperties.getAttemptWindowMinutes()).thenReturn(5);
    }

    @Test
    @DisplayName("Should allow when no rate limit exists")
    void shouldAllowWhenNoRateLimitExists() {
        // Given
        String key = "testUser";
        when(valueOperations.get(anyString())).thenReturn(null);

        // When
        OtpGenerationResult result = rateLimiter.checkRateLimit(key);

        // Then
        assertNull(result);
        verify(valueOperations).get(OtpConstants.OTP_REDIS_KEY_PREFIX + key + OtpConstants.RATE_LIMIT_KEY_SUFFIX);
    }

    @Test
    @DisplayName("Should allow when rate limit window has passed")
    void shouldAllowWhenRateLimitWindowHasPassed() {
        // Given
        String key = "testUser";
        long pastTime = System.currentTimeMillis() - (OtpConstants.OTP_RATE_LIMIT_SECONDS + 1) * 1000;
        when(valueOperations.get(anyString())).thenReturn(String.valueOf(pastTime));

        // When
        OtpGenerationResult result = rateLimiter.checkRateLimit(key);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("Should return rate limited when within window")
    void shouldReturnRateLimitedWhenWithinWindow() {
        // Given
        String key = "testUser";
        long recentTime = System.currentTimeMillis() - (OtpConstants.OTP_RATE_LIMIT_SECONDS - 10) * 1000;
        when(valueOperations.get(anyString())).thenReturn(String.valueOf(recentTime));

        // When
        OtpGenerationResult result = rateLimiter.checkRateLimit(key);

        // Then
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("Please wait 10 seconds before requesting a new OTP.", result.getMessage());
    }

    @Test
    @DisplayName("Should handle invalid timestamp in Redis")
    void shouldHandleInvalidTimestampInRedis() {
        // Given
        String key = "testUser";
        when(valueOperations.get(anyString())).thenReturn("invalid-timestamp");

        // When
        OtpGenerationResult result = rateLimiter.checkRateLimit(key);

        // Then
        assertNull(result); // Continues despite invalid timestamp
    }

    @Test
    @DisplayName("Should allow first attempt")
    void shouldAllowFirstAttempt() {
        // Given
        String key = "testUser";
        when(valueOperations.increment(anyString(), eq(1L))).thenReturn(1L);

        // When
        OtpGenerationResult result = rateLimiter.checkAndIncrementAttempts(key);

        // Then
        assertNull(result);
        verify(valueOperations).increment(OtpConstants.OTP_REDIS_KEY_PREFIX + key + OtpConstants.ATTEMPTS_KEY_SUFFIX, 1);
        verify(redisTemplate).expire(anyString(), eq(5L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("Should allow attempts within limit")
    void shouldAllowAttemptsWithinLimit() {
        // Given
        String key = "testUser";
        when(valueOperations.increment(anyString(), eq(1L))).thenReturn(2L);

        // When
        OtpGenerationResult result = rateLimiter.checkAndIncrementAttempts(key);

        // Then
        assertNull(result);
        verify(valueOperations).increment(OtpConstants.OTP_REDIS_KEY_PREFIX + key + OtpConstants.ATTEMPTS_KEY_SUFFIX, 1);
        verify(redisTemplate, never()).expire(anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("Should return max attempts exceeded when limit reached")
    void shouldReturnMaxAttemptsExceededWhenLimitReached() {
        // Given
        String key = "testUser";
        when(valueOperations.increment(anyString(), eq(1L))).thenReturn(4L); // Exceeds max attempts of 3

        // When
        OtpGenerationResult result = rateLimiter.checkAndIncrementAttempts(key);

        // Then
        assertNotNull(result);
        assertTrue(result.isMaxAttemptsExceeded());
    }

    @Test
    @DisplayName("Should record rate limit timestamp")
    void shouldRecordRateLimitTimestamp() {
        // Given
        String key = "testUser";

        // When
        rateLimiter.recordRateLimitTimestamp(key);

        // Then
        verify(valueOperations).set(
            eq(OtpConstants.OTP_REDIS_KEY_PREFIX + key + OtpConstants.RATE_LIMIT_KEY_SUFFIX),
            anyString()
        );
        verify(redisTemplate).expire(
            eq(OtpConstants.OTP_REDIS_KEY_PREFIX + key + OtpConstants.RATE_LIMIT_KEY_SUFFIX),
            eq((long) OtpConstants.OTP_RATE_LIMIT_SECONDS + 5),
            eq(TimeUnit.SECONDS)
        );
    }

    @Test
    @DisplayName("Should reset attempts")
    void shouldResetAttempts() {
        // Given
        String key = "testUser";

        // When
        rateLimiter.resetAttempts(key);

        // Then
        verify(redisTemplate).delete(OtpConstants.OTP_REDIS_KEY_PREFIX + key + OtpConstants.ATTEMPTS_KEY_SUFFIX);
    }
}