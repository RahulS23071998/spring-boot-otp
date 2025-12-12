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

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TemporaryPasswordTokenService Tests")
class TemporaryPasswordTokenServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TemporaryPasswordTokenService temporaryPasswordTokenService;

    private static final String TEST_USERNAME = "testuser@example.com";
    private static final String TEST_TOKEN = "test-token-uuid-123";
    private static final long TOKEN_VALIDITY_MINUTES = 15;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Should generate temporary token successfully")
    void testGenerateTemporaryToken_Success() {
        doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        String token = temporaryPasswordTokenService.generateTemporaryToken(TEST_USERNAME);

        assertNotNull(token);
        assertFalse(token.isBlank());
        verify(valueOperations, times(2)).set(anyString(), anyString(), eq(TOKEN_VALIDITY_MINUTES), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("Should store both username and token mappings")
    void testGenerateTemporaryToken_StoreBothMappings() {
        doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        String token = temporaryPasswordTokenService.generateTemporaryToken(TEST_USERNAME);

        verify(valueOperations).set(
                eq("password-reset:" + TEST_USERNAME),
                eq(token),
                eq(TOKEN_VALIDITY_MINUTES),
                eq(TimeUnit.MINUTES)
        );
        verify(valueOperations).set(
                contains("password-reset-token:"),
                eq(TEST_USERNAME),
                eq(TOKEN_VALIDITY_MINUTES),
                eq(TimeUnit.MINUTES)
        );
    }

    @Test
    @DisplayName("Should validate correct temporary token")
    void testValidateTemporaryToken_ValidToken() {
        when(valueOperations.get("password-reset:" + TEST_USERNAME))
                .thenReturn(TEST_TOKEN);

        boolean isValid = temporaryPasswordTokenService.validateTemporaryToken(TEST_USERNAME, TEST_TOKEN);

        assertTrue(isValid);
        verify(valueOperations).get("password-reset:" + TEST_USERNAME);
    }

    @Test
    @DisplayName("Should reject invalid temporary token")
    void testValidateTemporaryToken_InvalidToken() {
        when(valueOperations.get("password-reset:" + TEST_USERNAME))
                .thenReturn("different-token");

        boolean isValid = temporaryPasswordTokenService.validateTemporaryToken(TEST_USERNAME, TEST_TOKEN);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should reject expired temporary token")
    void testValidateTemporaryToken_ExpiredToken() {
        when(valueOperations.get("password-reset:" + TEST_USERNAME))
                .thenReturn(null);

        boolean isValid = temporaryPasswordTokenService.validateTemporaryToken(TEST_USERNAME, TEST_TOKEN);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should handle Redis exception during validation")
    void testValidateTemporaryToken_RedisException() {
        when(valueOperations.get(anyString()))
                .thenThrow(new RuntimeException("Redis connection error"));

        boolean isValid = temporaryPasswordTokenService.validateTemporaryToken(TEST_USERNAME, TEST_TOKEN);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should invalidate temporary token successfully")
    void testInvalidateTemporaryToken_Success() {
        when(valueOperations.get("password-reset:" + TEST_USERNAME))
                .thenReturn(TEST_TOKEN);
        when(redisTemplate.delete(anyString()))
                .thenReturn(true);

        temporaryPasswordTokenService.invalidateTemporaryToken(TEST_USERNAME);

        verify(redisTemplate, times(2)).delete(anyString());
    }

    @Test
    @DisplayName("Should handle invalidation when token not found")
    void testInvalidateTemporaryToken_TokenNotFound() {
        when(valueOperations.get("password-reset:" + TEST_USERNAME))
                .thenReturn(null);
        when(redisTemplate.delete(anyString()))
                .thenReturn(false);

        assertDoesNotThrow(() -> 
            temporaryPasswordTokenService.invalidateTemporaryToken(TEST_USERNAME)
        );
        verify(redisTemplate, times(1)).delete("password-reset:" + TEST_USERNAME);
    }

    @Test
    @DisplayName("Should handle Redis exception during invalidation")
    void testInvalidateTemporaryToken_RedisException() {
        when(valueOperations.get(anyString()))
                .thenThrow(new RuntimeException("Redis error"));

        assertDoesNotThrow(() -> 
            temporaryPasswordTokenService.invalidateTemporaryToken(TEST_USERNAME)
        );
    }

    @Test
    @DisplayName("Should retrieve username from token successfully")
    void testGetUsernameFromToken_Success() {
        when(valueOperations.get("password-reset-token:" + TEST_TOKEN))
                .thenReturn(TEST_USERNAME);

        String username = temporaryPasswordTokenService.getUsernameFromToken(TEST_TOKEN);

        assertEquals(TEST_USERNAME, username);
        verify(valueOperations).get("password-reset-token:" + TEST_TOKEN);
    }

    @Test
    @DisplayName("Should return null when token not found")
    void testGetUsernameFromToken_TokenNotFound() {
        when(valueOperations.get("password-reset-token:" + TEST_TOKEN))
                .thenReturn(null);

        String username = temporaryPasswordTokenService.getUsernameFromToken(TEST_TOKEN);

        assertNull(username);
    }

    @Test
    @DisplayName("Should handle Redis exception when retrieving username")
    void testGetUsernameFromToken_RedisException() {
        when(valueOperations.get(anyString()))
                .thenThrow(new RuntimeException("Redis error"));

        String username = temporaryPasswordTokenService.getUsernameFromToken(TEST_TOKEN);

        assertNull(username);
    }

    @Test
    @DisplayName("Should generate unique tokens for multiple invocations")
    void testGenerateTemporaryToken_UniqueTokens() {
        doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        String token1 = temporaryPasswordTokenService.generateTemporaryToken(TEST_USERNAME);
        String token2 = temporaryPasswordTokenService.generateTemporaryToken(TEST_USERNAME);

        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
    }
}
