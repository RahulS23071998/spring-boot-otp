package com.starter.springboot.service;

import com.starter.springboot.dto.OtpGenerationResult;
import com.starter.springboot.dto.OtpValidationResult;
import com.starter.springboot.service.impl.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OtpService Tests")
class OtpServiceTest {

    @Mock
    private IOtpGenerator otpGenerator;

    @Mock
    private IOtpNotificationService notificationService;

    @Mock
    private IOtpRateLimiter rateLimiter;

    @Mock
    private IOtpAuditService auditService;

    @Mock
    private IOtpProperties otpProperties;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private OtpService otpService;

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_EMAIL = "test@example.com";

    private static final Integer TEST_OTP = 123456;
    private static final int MAX_ATTEMPTS = 3;
    private static final int ATTEMPT_WINDOW_MINUTES = 15;
    private static final int EXPIRY_MINUTES = 5;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(otpProperties.getMaxAttempts()).thenReturn(MAX_ATTEMPTS);
        lenient().when(otpProperties.getAttemptWindowMinutes()).thenReturn(ATTEMPT_WINDOW_MINUTES);
        lenient().when(otpProperties.getExpiryMinutes()).thenReturn(EXPIRY_MINUTES);
        lenient().when(rateLimiter.checkRateLimit(anyString())).thenReturn(null);
        lenient().when(rateLimiter.checkAndIncrementAttempts(anyString())).thenReturn(null);
        lenient().doNothing().when(rateLimiter).recordRateLimitTimestamp(anyString());
        lenient().doNothing().when(rateLimiter).resetAttempts(anyString());
        lenient().doNothing().when(auditService).persistAuditEntry(anyString());
        lenient().when(notificationService.sendOtpEmailAsync(anyString(), anyString(), anyInt()))
            .thenReturn(CompletableFuture.completedFuture(true));
        lenient().doNothing().when(notificationService).notifyLockout(anyString(), anyString());
        lenient().doNothing().when(notificationService).notifyDeliveryFailure(anyString(), anyString());
    }

    @Test
    @DisplayName("Should generate OTP successfully with valid user")
    void shouldGenerateOtpSuccessfullyWithValidUser() {
        // Given
        when(otpGenerator.generateOTP(TEST_USERNAME)).thenReturn(TEST_OTP);

        // When
        OtpGenerationResult result = otpService.generateOtp(TEST_USERNAME, TEST_EMAIL);

        // Then
        assertTrue(result.isSuccess());

        // Verify rate limiter
        verify(rateLimiter).checkRateLimit(TEST_USERNAME);
        verify(rateLimiter).checkAndIncrementAttempts(TEST_USERNAME);
        verify(rateLimiter).recordRateLimitTimestamp(TEST_USERNAME);

        // Verify OTP generation
        verify(otpGenerator).generateOTP(TEST_USERNAME);

        // Verify notification
        verify(notificationService).sendOtpEmailAsync(TEST_USERNAME, TEST_EMAIL, TEST_OTP);

        // Verify audit
        verify(auditService).persistAuditEntry(TEST_USERNAME);
    }

    @Test
    @DisplayName("Should validate correct OTP successfully")
    void shouldValidateCorrectOtpSuccessfully() {
        // Given
        when(otpGenerator.validateOtpStatus(TEST_USERNAME, TEST_OTP)).thenReturn(OtpValidationResult.success());

        // When
        OtpValidationResult result = otpService.validateOTP(TEST_USERNAME, TEST_OTP);

        // Then
        assertTrue(result.isSuccess());
        verify(otpGenerator).validateOtpStatus(TEST_USERNAME, TEST_OTP);
        verify(rateLimiter).resetAttempts(TEST_USERNAME);
    }

    @Test
    @DisplayName("Should return false when OTP is null")
    void shouldReturnFalseWhenOtpIsNull() {
        // When
        OtpValidationResult result = otpService.validateOTP(TEST_USERNAME, null);

        // Then
        assertFalse(result.isSuccess());
        verifyNoInteractions(otpGenerator);
    }

    @Test
    @DisplayName("Should return false when max attempts exceeded and trigger lockout notification")
    void shouldReturnFalseWhenMaxAttemptsExceededAndTriggerLockoutNotification() {
        // Given
        when(rateLimiter.checkAndIncrementAttempts(TEST_USERNAME)).thenReturn(OtpGenerationResult.maxAttemptsExceeded());

        // When
        OtpGenerationResult result = otpService.generateOtp(TEST_USERNAME, TEST_EMAIL);

        // Then
        assertTrue(result.isMaxAttemptsExceeded());
        verify(rateLimiter).checkRateLimit(TEST_USERNAME);
        verify(rateLimiter).checkAndIncrementAttempts(TEST_USERNAME);
        verify(notificationService).notifyLockout(TEST_USERNAME, TEST_EMAIL);
        verify(otpGenerator, never()).generateOTP(anyString());
        verify(notificationService, never()).sendOtpEmailAsync(anyString(), anyString(), anyInt());
        verify(auditService, never()).persistAuditEntry(anyString());
    }

    @Test
    @DisplayName("Should return false when OTP generation fails")
    void shouldReturnFalseWhenOtpGenerationFails() {
        // Given
        when(otpGenerator.generateOTP(TEST_USERNAME)).thenReturn(-1);

        // When
        OtpGenerationResult result = otpService.generateOtp(TEST_USERNAME, TEST_EMAIL);

        // Then
        assertFalse(result.isSuccess());
        verify(rateLimiter).checkRateLimit(TEST_USERNAME);
        verify(rateLimiter).checkAndIncrementAttempts(TEST_USERNAME);
        verify(otpGenerator).generateOTP(TEST_USERNAME);
        verify(notificationService).notifyDeliveryFailure(TEST_EMAIL, TEST_USERNAME);
        verify(notificationService, never()).sendOtpEmailAsync(anyString(), anyString(), anyInt());
        verify(auditService, never()).persistAuditEntry(anyString());
    }

    @Test
    @DisplayName("Should return false when user email not found")
    void shouldReturnFalseWhenUserEmailNotFound() {
        // When
        OtpGenerationResult result = otpService.generateOtp(TEST_USERNAME, null);

        // Then
        assertFalse(result.isSuccess());
        verifyNoInteractions(rateLimiter, otpGenerator, notificationService, auditService);
    }

    @Test
    @DisplayName("Should return false when email is blank")
    void shouldReturnFalseWhenEmailIsBlank() {
        // When
        OtpGenerationResult result = otpService.generateOtp(TEST_USERNAME, "");

        // Then
        assertFalse(result.isSuccess());
        verifyNoInteractions(rateLimiter, otpGenerator, notificationService, auditService);
    }

    @Test
    @DisplayName("Should return success but trigger delivery failure notification when email sending fails asynchronously")
    void shouldReturnSuccessAndTriggerDeliveryFailureNotificationWhenEmailSendingFails() {
        // Given
        when(otpGenerator.generateOTP(TEST_USERNAME)).thenReturn(TEST_OTP);
        when(notificationService.sendOtpEmailAsync(anyString(), anyString(), anyInt()))
            .thenReturn(CompletableFuture.completedFuture(false));

        // When
        OtpGenerationResult result = otpService.generateOtp(TEST_USERNAME, TEST_EMAIL);

        // Then
        assertTrue(result.isSuccess());
        verify(rateLimiter).checkRateLimit(TEST_USERNAME);
        verify(rateLimiter).checkAndIncrementAttempts(TEST_USERNAME);
        verify(rateLimiter).recordRateLimitTimestamp(TEST_USERNAME);
        verify(otpGenerator).generateOTP(TEST_USERNAME);
        verify(notificationService).sendOtpEmailAsync(TEST_USERNAME, TEST_EMAIL, TEST_OTP);
        verify(notificationService).notifyDeliveryFailure(TEST_EMAIL, TEST_USERNAME);
        verify(auditService).persistAuditEntry(TEST_USERNAME);
    }

    @Test
    @DisplayName("Should return false when validating incorrect OTP")
    void shouldReturnFalseWhenValidatingIncorrectOtp() {
        // Given
        int incorrectOtp = 999999;
        when(otpGenerator.validateOtpStatus(TEST_USERNAME, incorrectOtp)).thenReturn(OtpValidationResult.invalid());

        // When
        OtpValidationResult result = otpService.validateOTP(TEST_USERNAME, incorrectOtp);

        // Then
        assertFalse(result.isSuccess());
        verify(otpGenerator).validateOtpStatus(TEST_USERNAME, incorrectOtp);
        verify(rateLimiter, never()).resetAttempts(anyString());
    }

    @Test
    @DisplayName("Should not set expiry on subsequent attempts within window")
    void shouldNotSetExpiryOnSubsequentAttemptsWithinWindow() {
        // Given
        when(otpGenerator.generateOTP(TEST_USERNAME)).thenReturn(TEST_OTP);

        // When
        OtpGenerationResult result = otpService.generateOtp(TEST_USERNAME, TEST_EMAIL);

        // Then
        assertTrue(result.isSuccess());
        verify(rateLimiter).checkRateLimit(TEST_USERNAME);
        verify(rateLimiter).checkAndIncrementAttempts(TEST_USERNAME);
        verify(rateLimiter).recordRateLimitTimestamp(TEST_USERNAME);
        verify(otpGenerator).generateOTP(TEST_USERNAME);
        verify(notificationService).sendOtpEmailAsync(TEST_USERNAME, TEST_EMAIL, TEST_OTP);
        verify(auditService).persistAuditEntry(TEST_USERNAME);
    }

    @Test
    @DisplayName("Should create correct audit entry with proper dates")
    void shouldCreateCorrectAuditEntryWithProperDates() {
        // Given
        when(otpGenerator.generateOTP(TEST_USERNAME)).thenReturn(TEST_OTP);

        // When
        OtpGenerationResult result = otpService.generateOtp(TEST_USERNAME, TEST_EMAIL);

        // Then
        assertTrue(result.isSuccess());
        verify(rateLimiter).checkRateLimit(TEST_USERNAME);
        verify(rateLimiter).checkAndIncrementAttempts(TEST_USERNAME);
        verify(rateLimiter).recordRateLimitTimestamp(TEST_USERNAME);
        verify(otpGenerator).generateOTP(TEST_USERNAME);
        verify(notificationService).sendOtpEmailAsync(TEST_USERNAME, TEST_EMAIL, TEST_OTP);
        verify(auditService).persistAuditEntry(TEST_USERNAME);
    }

    @Test
    @DisplayName("Should handle whitespace email as blank")
    void shouldHandleWhitespaceEmailAsBlank() {
        // When
        OtpGenerationResult result = otpService.generateOtp(TEST_USERNAME, "   ");

        // Then
        assertFalse(result.isSuccess());
        verifyNoInteractions(rateLimiter, otpGenerator, notificationService, auditService);
    }

    @Test
    @DisplayName("Should handle exactly max attempts allowed")
    void shouldHandleExactlyMaxAttemptsAllowed() {
        // Given
        when(otpGenerator.generateOTP(TEST_USERNAME)).thenReturn(TEST_OTP);

        // When
        OtpGenerationResult result = otpService.generateOtp(TEST_USERNAME, TEST_EMAIL);

        // Then
        assertTrue(result.isSuccess());
        verify(rateLimiter).checkRateLimit(TEST_USERNAME);
        verify(rateLimiter).checkAndIncrementAttempts(TEST_USERNAME);
        verify(rateLimiter).recordRateLimitTimestamp(TEST_USERNAME);
        verify(otpGenerator).generateOTP(TEST_USERNAME);
        verify(notificationService).sendOtpEmailAsync(TEST_USERNAME, TEST_EMAIL, TEST_OTP);
        verify(auditService).persistAuditEntry(TEST_USERNAME);
    }

    @Test
    @DisplayName("Should validate OTP with empty string key")
    void shouldValidateOtpWithEmptyStringKey() {
        // Given
        String emptyKey = "";
        when(otpGenerator.validateOtpStatus(emptyKey, TEST_OTP)).thenReturn(OtpValidationResult.success());

        // When
        OtpValidationResult result = otpService.validateOTP(emptyKey, TEST_OTP);

        // Then
        assertTrue(result.isSuccess());
        verify(otpGenerator).validateOtpStatus(emptyKey, TEST_OTP);
        verify(rateLimiter).resetAttempts(emptyKey);
    }

    @Test
    @DisplayName("Should validate OTP with null key")
    void shouldValidateOtpWithNullKey() {
        // Given
        when(otpGenerator.validateOtpStatus(null, TEST_OTP)).thenReturn(OtpValidationResult.invalid());

        // When
        OtpValidationResult result = otpService.validateOTP(null, TEST_OTP);

        // Then
        assertFalse(result.isSuccess());
        verify(otpGenerator).validateOtpStatus(null, TEST_OTP);
    }
}