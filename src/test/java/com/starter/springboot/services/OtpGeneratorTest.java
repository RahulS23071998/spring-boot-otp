package com.starter.springboot.services;

import com.starter.springboot.constants.OtpConstants;
import com.starter.springboot.services.dto.OtpValidationResult;
import com.starter.springboot.services.impl.OtpGenerator;
import com.starter.springboot.services.impl.OtpProperties;
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
        lenient().when(otpProperties.getMaxAttempts()).thenReturn(3);
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
        verify(valueOperations).set(eq(redisKey + OtpConstants.STATUS_KEY_SUFFIX), eq(OtpConstants.STATUS_ACTIVE), eq((long) EXPIRY_MINUTES), eq(TimeUnit.MINUTES));
        verify(valueOperations).set(eq(redisKey + OtpConstants.FAILURE_KEY_SUFFIX), eq("0"), eq((long) EXPIRY_MINUTES), eq(TimeUnit.MINUTES));
        verify(otpProperties, times(3)).getExpiryMinutes();
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
        verify(valueOperations).set(redisKey + OtpConstants.STATUS_KEY_SUFFIX, OtpConstants.STATUS_ACTIVE, (long) EXPIRY_MINUTES, TimeUnit.MINUTES);
        verify(valueOperations).set(redisKey + OtpConstants.FAILURE_KEY_SUFFIX, "0", (long) EXPIRY_MINUTES, TimeUnit.MINUTES);
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
        String statusKey = redisKey + OtpConstants.STATUS_KEY_SUFFIX;
        
        when(valueOperations.get(statusKey)).thenReturn(OtpConstants.STATUS_ACTIVE);
        when(valueOperations.get(redisKey)).thenReturn(hashedOtp);

        // When
        OtpValidationResult result = otpGenerator.validateOtpStatus(TEST_KEY, testOtp);

        // Then
        assertTrue(result.isSuccess(), "Should validate correct OTP successfully");
        verify(valueOperations).get(statusKey);
        verify(valueOperations).get(redisKey);
        verify(redisTemplate).delete(redisKey);
        verify(redisTemplate).delete(redisKey + ":status");
        verify(redisTemplate).delete(redisKey + ":failures");
    }

    @Test
    @DisplayName("Should reject incorrect OTP")
    void shouldRejectIncorrectOtp() {
        // Given
        int correctOtp = 123456;
        int incorrectOtp = 654321;
        String hashedCorrectOtp = DigestUtils.sha256Hex(String.valueOf(correctOtp));
        String redisKey = REDIS_KEY_PREFIX + TEST_KEY;
        String statusKey = redisKey + OtpConstants.STATUS_KEY_SUFFIX;
        String failureKey = redisKey + OtpConstants.FAILURE_KEY_SUFFIX;
        
        when(valueOperations.get(statusKey)).thenReturn(OtpConstants.STATUS_ACTIVE);
        when(valueOperations.get(redisKey)).thenReturn(hashedCorrectOtp);
        when(valueOperations.increment(failureKey, 1)).thenReturn(1L);

        // When
        OtpValidationResult result = otpGenerator.validateOtpStatus(TEST_KEY, incorrectOtp);

        // Then
        assertTrue(result.isInvalid(), "Should reject incorrect OTP");
        verify(valueOperations).get(statusKey);
        verify(valueOperations).get(redisKey);
        verify(valueOperations).increment(failureKey, 1);
        verify(redisTemplate).expire(failureKey, EXPIRY_MINUTES, TimeUnit.MINUTES);
        verify(redisTemplate, never()).delete(redisKey);
        verify(redisTemplate, never()).delete(redisKey + ":status");
        verify(redisTemplate, never()).delete(redisKey + ":failures");
    }

    @Test
    @DisplayName("Should return invalid result when OTP not found in Redis")
    void shouldReturnInvalidResultWhenOtpNotFoundInRedis() {
        // Given
        String redisKey = REDIS_KEY_PREFIX + TEST_KEY;
        String statusKey = redisKey + OtpConstants.STATUS_KEY_SUFFIX;
        String failureKey = redisKey + OtpConstants.FAILURE_KEY_SUFFIX;
        when(valueOperations.get(statusKey)).thenReturn(OtpConstants.STATUS_ACTIVE);
        when(valueOperations.get(redisKey)).thenReturn(null);

        // When
        OtpValidationResult result = otpGenerator.validateOtpStatus(TEST_KEY, 123456);

        // Then
        assertTrue(result.isInvalid(), "Should return invalid result when OTP not found in Redis");
        verify(valueOperations).get(statusKey);
        verify(valueOperations).get(redisKey);
        verify(redisTemplate, never()).delete(redisKey);
        verify(redisTemplate, never()).delete(statusKey);
        verify(redisTemplate, never()).delete(failureKey);
    }

    @Test
    @DisplayName("Should delete OTP and metadata after successful validation")
    void shouldDeleteOtpAndMetadataAfterSuccessfulValidation() {
        // Given
        int testOtp = 123456;
        String hashedOtp = DigestUtils.sha256Hex(String.valueOf(testOtp));
        String redisKey = REDIS_KEY_PREFIX + TEST_KEY;
        String statusKey = redisKey + OtpConstants.STATUS_KEY_SUFFIX;
        String failureKey = redisKey + OtpConstants.FAILURE_KEY_SUFFIX;
        
        when(valueOperations.get(statusKey)).thenReturn(OtpConstants.STATUS_ACTIVE);
        when(valueOperations.get(redisKey)).thenReturn(hashedOtp);

        // When
        OtpValidationResult result = otpGenerator.validateOtpStatus(TEST_KEY, testOtp);

        // Then
        assertTrue(result.isSuccess());
        verify(valueOperations).get(statusKey);
        verify(valueOperations).get(redisKey);
        verify(redisTemplate).delete(redisKey);
        verify(redisTemplate).delete(statusKey);
        verify(redisTemplate).delete(failureKey);
    }

    @Test
    @DisplayName("Should not delete OTP from cache after failed validation")
    void shouldNotDeleteOtpFromCacheAfterFailedValidation() {
        // Given
        int correctOtp = 123456;
        int incorrectOtp = 654321;
        String hashedCorrectOtp = DigestUtils.sha256Hex(String.valueOf(correctOtp));
        String redisKey = REDIS_KEY_PREFIX + TEST_KEY;
        String statusKey = redisKey + OtpConstants.STATUS_KEY_SUFFIX;
        String failureKey = redisKey + OtpConstants.FAILURE_KEY_SUFFIX;
        
        when(valueOperations.get(statusKey)).thenReturn(OtpConstants.STATUS_ACTIVE);
        when(valueOperations.get(redisKey)).thenReturn(hashedCorrectOtp);
        when(valueOperations.increment(failureKey, 1)).thenReturn(1L);

        // When
        OtpValidationResult result = otpGenerator.validateOtpStatus(TEST_KEY, incorrectOtp);

        // Then
        assertTrue(result.isInvalid());
        verify(valueOperations).get(statusKey);
        verify(valueOperations).get(redisKey);
        verify(valueOperations).increment(failureKey, 1);
        verify(redisTemplate).expire(failureKey, EXPIRY_MINUTES, TimeUnit.MINUTES);
        verify(redisTemplate, never()).delete(redisKey);
        verify(redisTemplate, never()).delete(redisKey + ":status");
        verify(redisTemplate, never()).delete(redisKey + ":failures");
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
        verify(redisTemplate).delete(redisKey + ":status");
        verify(redisTemplate).delete(redisKey + ":failures");
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
        String redisKey = REDIS_KEY_PREFIX + "null";
        String statusKey = redisKey + OtpConstants.STATUS_KEY_SUFFIX;
        String failureKey = redisKey + OtpConstants.FAILURE_KEY_SUFFIX;
        when(valueOperations.get(statusKey)).thenReturn(OtpConstants.STATUS_ACTIVE);
        when(valueOperations.get(redisKey)).thenReturn(null);

        // When
        OtpValidationResult result = otpGenerator.validateOtpStatus(null, 123456);

        // Then
        assertTrue(result.isInvalid());
        verify(valueOperations).get(statusKey);
        verify(valueOperations).get(redisKey);
        verify(redisTemplate, never()).delete(redisKey);
        verify(redisTemplate, never()).delete(statusKey);
        verify(redisTemplate, never()).delete(failureKey);
    }

    @Test
    @DisplayName("Should handle null key in clearOTP")
    void shouldHandleNullKeyInClearOtp() {
        // When
        otpGenerator.clearOTPFromCache(null);

        // Then
        verify(redisTemplate).delete("otp:null");
        verify(redisTemplate).delete("otp:null:status");
        verify(redisTemplate).delete("otp:null:failures");
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

        when(valueOperations.get(REDIS_KEY_PREFIX + emailKey + ":status")).thenReturn("ACTIVE");
        when(valueOperations.get(REDIS_KEY_PREFIX + phoneKey + ":status")).thenReturn("ACTIVE");
        when(valueOperations.get(REDIS_KEY_PREFIX + alphanumericKey + ":status")).thenReturn("ACTIVE");
        when(valueOperations.get(REDIS_KEY_PREFIX + emailKey)).thenReturn(hashedOtp);
        when(valueOperations.get(REDIS_KEY_PREFIX + phoneKey)).thenReturn(hashedOtp);
        when(valueOperations.get(REDIS_KEY_PREFIX + alphanumericKey)).thenReturn(hashedOtp);

        // When & Then
        assertTrue(otpGenerator.validateOtpStatus(emailKey, testOtp).isSuccess());
        assertTrue(otpGenerator.validateOtpStatus(phoneKey, testOtp).isSuccess());
        assertTrue(otpGenerator.validateOtpStatus(alphanumericKey, testOtp).isSuccess());

        verify(redisTemplate).delete(REDIS_KEY_PREFIX + emailKey);
        verify(redisTemplate).delete(REDIS_KEY_PREFIX + emailKey + ":status");
        verify(redisTemplate).delete(REDIS_KEY_PREFIX + emailKey + ":failures");
        verify(redisTemplate).delete(REDIS_KEY_PREFIX + phoneKey);
        verify(redisTemplate).delete(REDIS_KEY_PREFIX + phoneKey + ":status");
        verify(redisTemplate).delete(REDIS_KEY_PREFIX + phoneKey + ":failures");
        verify(redisTemplate).delete(REDIS_KEY_PREFIX + alphanumericKey);
        verify(redisTemplate).delete(REDIS_KEY_PREFIX + alphanumericKey + ":status");
        verify(redisTemplate).delete(REDIS_KEY_PREFIX + alphanumericKey + ":failures");
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