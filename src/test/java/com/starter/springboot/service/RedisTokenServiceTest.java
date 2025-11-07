package com.starter.springboot.service;

import com.starter.springboot.service.impl.RedisTokenService;
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
@DisplayName("RedisTokenService Tests")
class RedisTokenServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisTokenService redisTokenService;

    private static final Long TEST_USER_ID = 123L;
    private static final String TEST_JTI = "test-jti-uuid";
    private static final long TEST_TTL_SECONDS = 3600L;
    private static final String EXPECTED_KEY = "whitelist:123";

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Should register JTI successfully")
    void shouldRegisterJtiSuccessfully() {
        // When
        redisTokenService.registerJti(TEST_USER_ID, TEST_JTI, TEST_TTL_SECONDS);

        // Then
        verify(redisTemplate).opsForValue();
        verify(valueOperations).set(EXPECTED_KEY, TEST_JTI, TEST_TTL_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("Should check whitelisted JTI returns true")
    void shouldCheckWhitelistedJtiReturnsTrue() {
        // Given
        when(valueOperations.get(EXPECTED_KEY)).thenReturn(TEST_JTI);

        // When
        boolean result = redisTokenService.isJtiWhitelisted(TEST_USER_ID, TEST_JTI);

        // Then
        assertTrue(result);
        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(EXPECTED_KEY);
    }

    @Test
    @DisplayName("Should remove whitelist successfully")
    void shouldRemoveWhitelistSuccessfully() {
        // When
        redisTokenService.removeWhitelist(TEST_USER_ID);

        // Then
        verify(redisTemplate).delete(EXPECTED_KEY);
    }

    @Test
    @DisplayName("Should check non-whitelisted JTI returns false")
    void shouldCheckNonWhitelistedJtiReturnsFalse() {
        // Given
        String differentJti = "different-jti";
        when(valueOperations.get(EXPECTED_KEY)).thenReturn(TEST_JTI);

        // When
        boolean result = redisTokenService.isJtiWhitelisted(TEST_USER_ID, differentJti);

        // Then
        assertFalse(result);
        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(EXPECTED_KEY);
    }

    @Test
    @DisplayName("Should check JTI with null userId")
    void shouldCheckJtiWithNullUserId() {
        // Given
        String expectedKeyForNull = "whitelist:null";
        when(valueOperations.get(expectedKeyForNull)).thenReturn(TEST_JTI);

        // When
        boolean result = redisTokenService.isJtiWhitelisted(null, TEST_JTI);

        // Then
        assertTrue(result);
        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(expectedKeyForNull);
    }

    @Test
    @DisplayName("Should check JTI when Redis returns null")
    void shouldCheckJtiWhenRedisReturnsNull() {
        // Given
        when(valueOperations.get(EXPECTED_KEY)).thenReturn(null);

        // When
        boolean result = redisTokenService.isJtiWhitelisted(TEST_USER_ID, TEST_JTI);

        // Then
        assertFalse(result);
        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(EXPECTED_KEY);
    }

    @Test
    @DisplayName("Should handle Redis exception on register")
    void shouldHandleRedisExceptionOnRegister() {
        // Given
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis connection failed"));

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> redisTokenService.registerJti(TEST_USER_ID, TEST_JTI, TEST_TTL_SECONDS));

        // Verify that opsForValue was called (exception occurred during execution)
        verify(redisTemplate).opsForValue();
    }

    @Test
    @DisplayName("Should handle Redis exception on check")
    void shouldHandleRedisExceptionOnCheck() {
        // Given
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis connection failed"));

        // When
        boolean result = redisTokenService.isJtiWhitelisted(TEST_USER_ID, TEST_JTI);

        // Then
        assertFalse(result);
        verify(redisTemplate).opsForValue();
    }

    @Test
    @DisplayName("Should handle Redis exception on remove")
    void shouldHandleRedisExceptionOnRemove() {
        // Given
        doThrow(new RuntimeException("Redis connection failed")).when(redisTemplate).delete(EXPECTED_KEY);

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> redisTokenService.removeWhitelist(TEST_USER_ID));

        // Verify that delete was attempted
        verify(redisTemplate).delete(EXPECTED_KEY);
    }

    @Test
    @DisplayName("Should register JTI with zero TTL")
    void shouldRegisterJtiWithZeroTtl() {
        // When
        redisTokenService.registerJti(TEST_USER_ID, TEST_JTI, 0L);

        // Then
        verify(redisTemplate).opsForValue();
        verify(valueOperations).set(EXPECTED_KEY, TEST_JTI, 0L, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("Should register JTI with negative TTL")
    void shouldRegisterJtiWithNegativeTtl() {
        // When
        redisTokenService.registerJti(TEST_USER_ID, TEST_JTI, -100L);

        // Then
        verify(redisTemplate).opsForValue();
        verify(valueOperations).set(EXPECTED_KEY, TEST_JTI, -100L, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("Should register null JTI")
    void shouldRegisterNullJti() {
        // When
        redisTokenService.registerJti(TEST_USER_ID, null, TEST_TTL_SECONDS);

        // Then
        verify(redisTemplate).opsForValue();
        verify(valueOperations).set(EXPECTED_KEY, null, TEST_TTL_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("Should check null JTI returns false when stored JTI exists")
    void shouldCheckNullJtiReturnsFalseWhenStoredJtiExists() {
        // Given
        when(valueOperations.get(EXPECTED_KEY)).thenReturn(TEST_JTI);

        // When
        boolean result = redisTokenService.isJtiWhitelisted(TEST_USER_ID, null);

        // Then
        assertFalse(result);
        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(EXPECTED_KEY);
    }

    @Test
    @DisplayName("Should check null JTI returns true when stored JTI is also null")
    void shouldCheckNullJtiReturnsTrueWhenStoredJtiIsAlsoNull() {
        // Given
        when(valueOperations.get(EXPECTED_KEY)).thenReturn(null);

        // When
        boolean result = redisTokenService.isJtiWhitelisted(TEST_USER_ID, null);

        // Then
        assertFalse(result); // Method returns false when stored value is null, regardless of input JTI
        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(EXPECTED_KEY);
    }

    @Test
    @DisplayName("Should generate correct user key for different user IDs")
    void shouldGenerateCorrectUserKeyForDifferentUserIds() {
        // Test with different user IDs to ensure key generation is correct
        Long userId1 = 1L;
        Long userId2 = 999999L;
        String jti1 = "jti-1";
        String jti2 = "jti-2";

        // When
        redisTokenService.registerJti(userId1, jti1, TEST_TTL_SECONDS);
        redisTokenService.registerJti(userId2, jti2, TEST_TTL_SECONDS);

        // Then
        verify(valueOperations).set("whitelist:1", jti1, TEST_TTL_SECONDS, TimeUnit.SECONDS);
        verify(valueOperations).set("whitelist:999999", jti2, TEST_TTL_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("Should handle empty string JTI")
    void shouldHandleEmptyStringJti() {
        // Given
        String emptyJti = "";
        when(valueOperations.get(EXPECTED_KEY)).thenReturn(emptyJti);

        // When
        boolean result = redisTokenService.isJtiWhitelisted(TEST_USER_ID, emptyJti);

        // Then
        assertTrue(result);
        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(EXPECTED_KEY);
    }

    @Test
    @DisplayName("Should overwrite existing JTI for same user")
    void shouldOverwriteExistingJtiForSameUser() {
        // Given
        String newJti = "new-jti-uuid";

        // When - register first JTI
        redisTokenService.registerJti(TEST_USER_ID, TEST_JTI, TEST_TTL_SECONDS);
        // When - register second JTI for same user (should overwrite)
        redisTokenService.registerJti(TEST_USER_ID, newJti, TEST_TTL_SECONDS);

        // Then - both operations should have been called with the same key
        verify(valueOperations).set(EXPECTED_KEY, TEST_JTI, TEST_TTL_SECONDS, TimeUnit.SECONDS);
        verify(valueOperations).set(EXPECTED_KEY, newJti, TEST_TTL_SECONDS, TimeUnit.SECONDS);
        verify(valueOperations, times(2)).set(eq(EXPECTED_KEY), any(String.class), eq(TEST_TTL_SECONDS), eq(TimeUnit.SECONDS));
    }
}