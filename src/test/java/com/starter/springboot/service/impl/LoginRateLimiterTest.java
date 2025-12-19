package com.starter.springboot.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginRateLimiterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private LoginRateLimiter loginRateLimiter;

    private static final String TEST_KEY = "testUser";
    private static final String REDIS_KEY = "login:attempts:" + TEST_KEY;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(loginRateLimiter, "maxAttempts", 3);
        ReflectionTestUtils.setField(loginRateLimiter, "windowSeconds", 60);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Should allow login when attempts are below max")
    void shouldAllowLoginWhenAttemptsBelowMax() {
        // Given
        when(valueOperations.get(REDIS_KEY)).thenReturn("2");

        // When
        boolean allowed = loginRateLimiter.isAllowed(TEST_KEY);

        // Then
        assertTrue(allowed);
        verify(valueOperations).get(REDIS_KEY);
    }

    @Test
    @DisplayName("Should allow login when no attempts recorded")
    void shouldAllowLoginWhenNoAttemptsRecorded() {
        // Given
        when(valueOperations.get(REDIS_KEY)).thenReturn(null);

        // When
        boolean allowed = loginRateLimiter.isAllowed(TEST_KEY);

        // Then
        assertTrue(allowed);
        verify(valueOperations).get(REDIS_KEY);
    }

    @Test
    @DisplayName("Should block login when attempts reach max")
    void shouldBlockLoginWhenAttemptsReachMax() {
        // Given
        when(valueOperations.get(REDIS_KEY)).thenReturn("3");

        // When
        boolean allowed = loginRateLimiter.isAllowed(TEST_KEY);

        // Then
        assertFalse(allowed);
        verify(valueOperations).get(REDIS_KEY);
    }

    @Test
    @DisplayName("Should block login when attempts exceed max")
    void shouldBlockLoginWhenAttemptsExceedMax() {
        // Given
        when(valueOperations.get(REDIS_KEY)).thenReturn("4");

        // When
        boolean allowed = loginRateLimiter.isAllowed(TEST_KEY);

        // Then
        assertFalse(allowed);
        verify(valueOperations).get(REDIS_KEY);
    }

    @Test
    @DisplayName("Should record attempt and set expiration on first attempt")
    void shouldRecordAttemptAndSetExpirationOnFirstAttempt() {
        // Given
        when(valueOperations.increment(REDIS_KEY)).thenReturn(1L);

        // When
        loginRateLimiter.recordAttempt(TEST_KEY);

        // Then
        verify(valueOperations).increment(REDIS_KEY);
        verify(redisTemplate).expire(REDIS_KEY, 60, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("Should record attempt without setting expiration on subsequent attempts")
    void shouldRecordAttemptWithoutSettingExpirationOnSubsequentAttempts() {
        // Given
        when(valueOperations.increment(REDIS_KEY)).thenReturn(2L);

        // When
        loginRateLimiter.recordAttempt(TEST_KEY);

        // Then
        verify(valueOperations).increment(REDIS_KEY);
        verify(redisTemplate, never()).expire(anyString(), anyLong(), any(TimeUnit.class));
    }
}
