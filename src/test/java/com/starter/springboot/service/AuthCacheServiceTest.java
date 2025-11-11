package com.starter.springboot.service;

import com.starter.springboot.service.impl.AuthCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthCacheService Tests")
class AuthCacheServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private IRedisTokenService redisTokenService;

    @InjectMocks
    private AuthCacheService authCacheService;

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_USERNAME_UPPER = "TESTUSER";
    private static final Long TEST_USER_ID = 1L;
    private static final String EXPECTED_CACHE_KEY = "auth:user:testuser";

    @Test
    @DisplayName("Should clear authentication cache for user successfully")
    void shouldClearAuthenticationCacheForUserSuccessfully() {
        // Given
        when(redisTemplate.delete(EXPECTED_CACHE_KEY)).thenReturn(true);

        // When
        authCacheService.clearAuthCacheForUser(TEST_USERNAME);

        // Then
        verify(redisTemplate).delete(EXPECTED_CACHE_KEY);
    }

    @Test
    @DisplayName("Should handle case insensitive username for cache clearing")
    void shouldHandleCaseInsensitiveUsernameForCacheClearing() {
        // Given
        String expectedKey = "auth:user:testuser";
        when(redisTemplate.delete(expectedKey)).thenReturn(true);

        // When
        authCacheService.clearAuthCacheForUser(TEST_USERNAME_UPPER);

        // Then
        verify(redisTemplate).delete(expectedKey);
    }

    @Test
    @DisplayName("Should not attempt cache clearing when username is null")
    void shouldNotAttemptCacheClearingWhenUsernameIsNull() {
        // When
        authCacheService.clearAuthCacheForUser(null);

        // Then
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("Should not attempt cache clearing when username is empty")
    void shouldNotAttemptCacheClearingWhenUsernameIsEmpty() {
        // When
        authCacheService.clearAuthCacheForUser("");

        // Then
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("Should handle Redis exception during cache clearing gracefully")
    void shouldHandleRedisExceptionDuringCacheClearingGracefully() {
        // Given
        when(redisTemplate.delete(EXPECTED_CACHE_KEY)).thenThrow(new RuntimeException("Redis connection failed"));

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> authCacheService.clearAuthCacheForUser(TEST_USERNAME));
        verify(redisTemplate).delete(EXPECTED_CACHE_KEY);
    }

    @Test
    @DisplayName("Should clear all authentication caches successfully")
    void shouldClearAllAuthenticationCachesSuccessfully() {
        // Given
        Set<String> cacheKeys = new HashSet<>();
        cacheKeys.add("auth:user:user1");
        cacheKeys.add("auth:user:user2");
        cacheKeys.add("auth:user:user3");

        when(redisTemplate.keys("auth:user:*")).thenReturn(cacheKeys);

        // When
        authCacheService.clearAllAuthCache();

        // Then
        verify(redisTemplate).keys("auth:user:*");
        verify(redisTemplate).delete(cacheKeys);
    }

    @Test
    @DisplayName("Should handle empty cache keys set gracefully")
    void shouldHandleEmptyCacheKeysSetGracefully() {
        // Given
        when(redisTemplate.keys("auth:user:*")).thenReturn(new HashSet<>());

        // When
        authCacheService.clearAllAuthCache();

        // Then
        verify(redisTemplate).keys("auth:user:*");
        verify(redisTemplate, never()).delete(anySet());
    }

    @Test
    @DisplayName("Should handle null cache keys set gracefully")
    void shouldHandleNullCacheKeysSetGracefully() {
        // Given
        when(redisTemplate.keys("auth:user:*")).thenReturn(null);

        // When
        authCacheService.clearAllAuthCache();

        // Then
        verify(redisTemplate).keys("auth:user:*");
        verify(redisTemplate, never()).delete(anySet());
    }

    @Test
    @DisplayName("Should handle Redis exception during all cache clearing gracefully")
    void shouldHandleRedisExceptionDuringAllCacheClearingGracefully() {
        // Given
        when(redisTemplate.keys("auth:user:*")).thenThrow(new RuntimeException("Redis connection failed"));

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> authCacheService.clearAllAuthCache());
        verify(redisTemplate).keys("auth:user:*");
    }

    @Test
    @DisplayName("Should clear all caches for user successfully")
    void shouldClearAllCachesForUserSuccessfully() {
        // Given
        when(redisTemplate.delete(EXPECTED_CACHE_KEY)).thenReturn(true);
        doNothing().when(redisTokenService).removeWhitelist(TEST_USER_ID);

        // When
        authCacheService.clearAllCachesForUser(TEST_USERNAME, TEST_USER_ID);

        // Then
        verify(redisTemplate).delete(EXPECTED_CACHE_KEY);
        verify(redisTokenService).removeWhitelist(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should clear auth cache even when token removal fails")
    void shouldClearAuthCacheEvenWhenTokenRemovalFails() {
        // Given
        when(redisTemplate.delete(EXPECTED_CACHE_KEY)).thenReturn(true);
        doThrow(new RuntimeException("Token removal failed")).when(redisTokenService).removeWhitelist(TEST_USER_ID);

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> authCacheService.clearAllCachesForUser(TEST_USERNAME, TEST_USER_ID));
        verify(redisTemplate).delete(EXPECTED_CACHE_KEY);
        verify(redisTokenService).removeWhitelist(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should skip token whitelist removal when userId is null")
    void shouldSkipTokenWhitelistRemovalWhenUserIdIsNull() {
        // Given
        when(redisTemplate.delete(EXPECTED_CACHE_KEY)).thenReturn(true);

        // When
        authCacheService.clearAllCachesForUser(TEST_USERNAME, null);

        // Then
        verify(redisTemplate).delete(EXPECTED_CACHE_KEY);
        verify(redisTokenService, never()).removeWhitelist(anyLong());
    }

    @Test
    @DisplayName("Should handle case insensitive username in comprehensive cache clearing")
    void shouldHandleCaseInsensitiveUsernameInComprehensiveCacheClearing() {
        // Given
        String expectedKey = "auth:user:testuser";
        when(redisTemplate.delete(expectedKey)).thenReturn(true);
        doNothing().when(redisTokenService).removeWhitelist(TEST_USER_ID);

        // When
        authCacheService.clearAllCachesForUser(TEST_USERNAME_UPPER, TEST_USER_ID);

        // Then
        verify(redisTemplate).delete(expectedKey);
        verify(redisTokenService).removeWhitelist(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should handle Redis exception in comprehensive cache clearing gracefully")
    void shouldHandleRedisExceptionInComprehensiveCacheClearingGracefully() {
        // Given
        when(redisTemplate.delete(EXPECTED_CACHE_KEY)).thenThrow(new RuntimeException("Redis connection failed"));
        doNothing().when(redisTokenService).removeWhitelist(TEST_USER_ID);

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> authCacheService.clearAllCachesForUser(TEST_USERNAME, TEST_USER_ID));
        verify(redisTemplate).delete(EXPECTED_CACHE_KEY);
        verify(redisTokenService).removeWhitelist(TEST_USER_ID);
    }
}