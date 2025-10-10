package com.starter.springboot.services;

import org.apache.commons.codec.digest.DigestUtils;
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
@DisplayName("OtpGenerator Tests")
class OtpGeneratorTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private OtpProperties otpProperties;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private OtpGenerator otpGenerator;

    private static final String TEST_KEY = "test@example.com";
    private static final String REDIS_KEY_PREFIX = "otp:";
    private static final int EXPIRY_MINUTES = 5;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(otpProperties.getExpiryMinutes()).thenReturn(EXPIRY_MINUTES);
    }

    @Test
    @DisplayName("Should generate valid 6-digit OTP and store in Redis")
    void shouldGenerateValidSixDigitOtpAndStoreInRedis() {
        // Given
        String redisKey = REDIS_KEY_PREFIX + TEST_KEY;

        // When
        Integer result = otpGenerator.generateOTP(TEST_KEY);

        // Then
        assertNotNull(result);
        assertTrue(result >= 100000 && result <= 999999, "OTP should be 6 digits");
        assertNotEquals(-1, result, "OTP generation should not fail");
        
        // Verify Redis interaction
        verify(valueOperations).set(eq(redisKey), anyString(), eq((long) EXPIRY_MINUTES), eq(TimeUnit.MINUTES));
        verify(otpProperties).getExpiryMinutes();
    }

    @Test
    @DisplayName("Should store hashed OTP in Redis with correct key and expiry")
    void shouldStoreHashedOtpInRedisWithCorrectKeyAndExpiry() {
        // Given
        String redisKey = REDIS_KEY_PREFIX + TEST_KEY;

        // When
        Integer otp = otpGenerator.generateOTP(TEST_KEY);

        // Then
        assertNotNull(otp);
        String expectedHash = DigestUtils.sha256Hex(String.valueOf(otp));
        verify(valueOperations).set(redisKey, expectedHash, (long) EXPIRY_MINUTES, TimeUnit.MINUTES);
    }

    @Test
    @DisplayName("Should return -1 when Redis storage fails")
    void shouldReturnMinusOneWhenRedisStorageFails() {
        // Given
        String redisKey = REDIS_KEY_PREFIX + TEST_KEY;
        doThrow(new RuntimeException("Redis connection failed"))
                .when(valueOperations).set(eq(redisKey), anyString(), eq((long) EXPIRY_MINUTES), eq(TimeUnit.MINUTES));

        // When
        Integer result = otpGenerator.generateOTP(TEST_KEY);

        // Then
        assertEquals(-1, result, "Should return -1 when Redis storage fails");
        verify(valueOperations).set(eq(redisKey), anyString(), eq((long) EXPIRY_MINUTES), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("Should generate different OTPs for multiple calls")
    void shouldGenerateDifferentOtpsForMultipleCalls() {
        // When
        Integer otp1 = otpGenerator.generateOTP(TEST_KEY);
        Integer otp2 = otpGenerator.generateOTP(TEST_KEY);

        // Then
        assertNotNull(otp1);
        assertNotNull(otp2);
        assertNotEquals(otp1, otp2, "Generated OTPs should be different");
    }

    @Test
    @DisplayName("Should validate correct OTP successfully")
    void shouldValidateCorrectOtpSuccessfully() {
        // Given
        int testOtp = 123456;
        String hashedOtp = DigestUtils.sha256Hex(String.valueOf(testOtp));
        String redisKey = REDIS_KEY_PREFIX + TEST_KEY;
        
        when(valueOperations.get(redisKey)).thenReturn(hashedOtp);

        // When
        boolean result = otpGenerator.validateOTPBasedOnKey(TEST_KEY, testOtp);

        // Then
        assertTrue(result, "Should validate correct OTP successfully");
        verify(valueOperations).get(redisKey);
        verify(redisTemplate).delete(redisKey);
    }

    @Test
    @DisplayName("Should reject incorrect OTP")
    void shouldRejectIncorrectOtp() {
        // Given
        int correctOtp = 123456;
        int incorrectOtp = 654321;
        String hashedCorrectOtp = DigestUtils.sha256Hex(String.valueOf(correctOtp));
        String redisKey = REDIS_KEY_PREFIX + TEST_KEY;
        
        when(valueOperations.get(redisKey)).thenReturn(hashedCorrectOtp);

        // When
        boolean result = otpGenerator.validateOTPBasedOnKey(TEST_KEY, incorrectOtp);

        // Then
        assertFalse(result, "Should reject incorrect OTP");
        verify(valueOperations).get(redisKey);
        verify(redisTemplate, never()).delete(redisKey);
    }

    @Test
    @DisplayName("Should return false when OTP not found in Redis")
    void shouldReturnFalseWhenOtpNotFoundInRedis() {
        // Given
        String redisKey = REDIS_KEY_PREFIX + TEST_KEY;
        when(valueOperations.get(redisKey)).thenReturn(null);

        // When
        boolean result = otpGenerator.validateOTPBasedOnKey(TEST_KEY, 123456);

        // Then
        assertFalse(result, "Should return false when OTP not found in Redis");
        verify(valueOperations).get(redisKey);
        verify(redisTemplate, never()).delete(redisKey);
    }

    @Test
    @DisplayName("Should delete OTP from cache after successful validation")
    void shouldDeleteOtpFromCacheAfterSuccessfulValidation() {
        // Given
        int testOtp = 123456;
        String hashedOtp = DigestUtils.sha256Hex(String.valueOf(testOtp));
        String redisKey = REDIS_KEY_PREFIX + TEST_KEY;
        
        when(valueOperations.get(redisKey)).thenReturn(hashedOtp);

        // When
        boolean result = otpGenerator.validateOTPBasedOnKey(TEST_KEY, testOtp);

        // Then
        assertTrue(result);
        verify(redisTemplate).delete(redisKey);
    }

    @Test
    @DisplayName("Should not delete OTP from cache after failed validation")
    void shouldNotDeleteOtpFromCacheAfterFailedValidation() {
        // Given
        int correctOtp = 123456;
        int incorrectOtp = 654321;
        String hashedCorrectOtp = DigestUtils.sha256Hex(String.valueOf(correctOtp));
        String redisKey = REDIS_KEY_PREFIX + TEST_KEY;
        
        when(valueOperations.get(redisKey)).thenReturn(hashedCorrectOtp);

        // When
        boolean result = otpGenerator.validateOTPBasedOnKey(TEST_KEY, incorrectOtp);

        // Then
        assertFalse(result);
        verify(redisTemplate, never()).delete(redisKey);
    }

    @Test
    @DisplayName("Should clear OTP from cache with correct Redis key")
    void shouldClearOtpFromCacheWithCorrectRedisKey() {
        // Given
        String redisKey = REDIS_KEY_PREFIX + TEST_KEY;

        // When
        otpGenerator.clearOTPFromCache(TEST_KEY);

        // Then
        verify(redisTemplate).delete(redisKey);
    }

    @Test
    @DisplayName("Should handle null key in generateOTP")
    void shouldHandleNullKeyInGenerateOtp() {
        // When
        Integer result = otpGenerator.generateOTP(null);

        // Then
        assertNotNull(result);
        assertTrue(result >= 100000 && result <= 999999 || result == -1);
        verify(valueOperations).set(eq("otp:null"), anyString(), eq((long) EXPIRY_MINUTES), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("Should handle null key in validateOTP")
    void shouldHandleNullKeyInValidateOtp() {
        // Given
        when(valueOperations.get("otp:null")).thenReturn(null);

        // When
        boolean result = otpGenerator.validateOTPBasedOnKey(null, 123456);

        // Then
        assertFalse(result);
        verify(valueOperations).get("otp:null");
    }

    @Test
    @DisplayName("Should handle null key in clearOTP")
    void shouldHandleNullKeyInClearOtp() {
        // When
        otpGenerator.clearOTPFromCache(null);

        // Then
        verify(redisTemplate).delete("otp:null");
    }

    @Test
    @DisplayName("Should handle empty string key")
    void shouldHandleEmptyStringKey() {
        // Given
        String emptyKey = "";
        String redisKey = REDIS_KEY_PREFIX + emptyKey;

        // When
        Integer result = otpGenerator.generateOTP(emptyKey);

        // Then
        assertNotNull(result);
        verify(valueOperations).set(eq(redisKey), anyString(), eq((long) EXPIRY_MINUTES), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("Should validate OTP with different key formats")
    void shouldValidateOtpWithDifferentKeyFormats() {
        // Given
        String emailKey = "user@example.com";
        String phoneKey = "+1234567890";
        String alphanumericKey = "user123";
        int testOtp = 123456;
        String hashedOtp = DigestUtils.sha256Hex(String.valueOf(testOtp));

        when(valueOperations.get(REDIS_KEY_PREFIX + emailKey)).thenReturn(hashedOtp);
        when(valueOperations.get(REDIS_KEY_PREFIX + phoneKey)).thenReturn(hashedOtp);
        when(valueOperations.get(REDIS_KEY_PREFIX + alphanumericKey)).thenReturn(hashedOtp);

        // When & Then
        assertTrue(otpGenerator.validateOTPBasedOnKey(emailKey, testOtp));
        assertTrue(otpGenerator.validateOTPBasedOnKey(phoneKey, testOtp));
        assertTrue(otpGenerator.validateOTPBasedOnKey(alphanumericKey, testOtp));

        verify(redisTemplate, times(3)).delete(anyString());
    }

    @Test
    @DisplayName("Should generate OTP within valid range consistently")
    void shouldGenerateOtpWithinValidRangeConsistently() {
        // When & Then
        for (int i = 0; i < 100; i++) {
            Integer otp = otpGenerator.generateOTP(TEST_KEY + i);
            assertNotNull(otp);
            if (otp != -1) { // Skip failed generations
                assertTrue(otp >= 100000 && otp <= 999999, 
                    "OTP " + otp + " should be between 100000 and 999999");
            }
        }
    }
}