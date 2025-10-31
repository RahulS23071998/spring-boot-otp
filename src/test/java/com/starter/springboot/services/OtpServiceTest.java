package com.starter.springboot.services;

import com.starter.springboot.constants.EmailConstants;
import com.starter.springboot.constants.OtpConstants;
import com.starter.springboot.otp.OtpAuditEntry;
import com.starter.springboot.repositories.OtpAuditEntryRepository;
import com.starter.springboot.rest.dto.EmailDTO;
import com.starter.springboot.services.dto.OtpValidationResult;
import com.starter.springboot.services.impl.OtpProperties;
import com.starter.springboot.services.impl.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OtpService Tests")
class OtpServiceTest {

    @Mock
    private IOtpGenerator otpGenerator;

    @Mock
    private IEmailService emailService;

    @Mock
    private OtpProperties otpProperties;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private OtpAuditEntryRepository otpAuditEntryRepository;

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
        lenient().when(emailService.sendSimpleMessageAsync(any(EmailDTO.class)))
            .thenReturn(CompletableFuture.completedFuture(true));
    }

    @Test
    @DisplayName("Should generate OTP successfully with valid user")
    void shouldGenerateOtpSuccessfullyWithValidUser() {
        // Given
        String attemptsKey = "otp:" + TEST_USERNAME + ":attempts";
        
        when(valueOperations.increment(attemptsKey, 1)).thenReturn(1L);
        when(otpGenerator.generateOTP(TEST_USERNAME)).thenReturn(TEST_OTP);
        when(emailService.sendSimpleMessageAsync(any(EmailDTO.class))).thenReturn(CompletableFuture.completedFuture(true));

        // When
        Boolean result = otpService.generateOtp(TEST_USERNAME, TEST_EMAIL);

        // Then
        assertTrue(result);
        
        // Verify Redis operations
        verify(valueOperations).increment(attemptsKey, 1);
        verify(redisTemplate).expire(attemptsKey, ATTEMPT_WINDOW_MINUTES, TimeUnit.MINUTES);
        
        // Verify OTP generation
        verify(otpGenerator).generateOTP(TEST_USERNAME);
        
        // Verify email sent
        ArgumentCaptor<EmailDTO> emailCaptor = ArgumentCaptor.forClass(EmailDTO.class);
        verify(emailService).sendSimpleMessageAsync(emailCaptor.capture());
        
        EmailDTO sentEmail = emailCaptor.getValue();
        assertEquals(EmailConstants.OTP_EMAIL_SUBJECT, sentEmail.getSubject());
        assertEquals(EmailConstants.OTP_EMAIL_BODY_PREFIX + TEST_OTP, sentEmail.getBody());
        assertEquals(1, sentEmail.getRecipients().size());
        assertEquals(TEST_EMAIL, sentEmail.getRecipients().get(0));
        
        // Verify audit entry created
        verify(otpAuditEntryRepository).save(any(OtpAuditEntry.class));
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
        String attemptsKey = "otp:" + TEST_USERNAME + ":attempts";
        when(valueOperations.increment(attemptsKey, 1)).thenReturn((long) MAX_ATTEMPTS + 1);

        // When
        Boolean result = otpService.generateOtp(TEST_USERNAME, TEST_EMAIL);

        // Then
        assertFalse(result);
        verify(valueOperations).increment(attemptsKey, 1);
        verify(otpGenerator, never()).generateOTP(anyString());
        verify(emailService, never()).sendSimpleMessageAsync(argThat(email ->
            EmailConstants.OTP_EMAIL_SUBJECT.equals(email.getSubject())
        ));
        verify(otpAuditEntryRepository, never()).save(any(OtpAuditEntry.class));
        verify(emailService).sendSimpleMessageAsync(argThat(email ->
            email.getRecipients().equals(List.of(TEST_EMAIL)) &&
                EmailConstants.OTP_LOCKED_EMAIL_SUBJECT.equals(email.getSubject()) &&
                OtpConstants.MAX_ATTEMPTS_EXCEEDED_MESSAGE.equals(email.getBody())
        ));
    }

    @Test
    @DisplayName("Should return false when OTP generation fails")
    void shouldReturnFalseWhenOtpGenerationFails() {
        // Given
        String attemptsKey = "otp:" + TEST_USERNAME + ":attempts";
        
        when(valueOperations.increment(attemptsKey, 1)).thenReturn(1L);
        when(otpGenerator.generateOTP(TEST_USERNAME)).thenReturn(-1);

        // When
        Boolean result = otpService.generateOtp(TEST_USERNAME, TEST_EMAIL);

        // Then
        assertFalse(result);
        verify(otpGenerator).generateOTP(TEST_USERNAME);
        verify(emailService, never()).sendSimpleMessageAsync(argThat(email ->
            EmailConstants.OTP_EMAIL_SUBJECT.equals(email.getSubject())
        ));
        verify(otpAuditEntryRepository, never()).save(any(OtpAuditEntry.class));
    }

    @Test
    @DisplayName("Should return false when user email not found")
    void shouldReturnFalseWhenUserEmailNotFound() {
        // Given
        String attemptsKey = "otp:" + TEST_USERNAME + ":attempts";
        
        when(valueOperations.increment(attemptsKey, 1)).thenReturn(1L);
        when(otpGenerator.generateOTP(TEST_USERNAME)).thenReturn(TEST_OTP);

        // When
        Boolean result = otpService.generateOtp(TEST_USERNAME, null);

        // Then
        assertFalse(result);
        verify(otpGenerator).generateOTP(TEST_USERNAME);
        verify(emailService, never()).sendSimpleMessageAsync(argThat(email ->
            EmailConstants.OTP_EMAIL_SUBJECT.equals(email.getSubject())
        ));
        verify(otpAuditEntryRepository, never()).save(any(OtpAuditEntry.class));
    }

    @Test
    @DisplayName("Should return false when email is blank")
    void shouldReturnFalseWhenEmailIsBlank() {
        // Given
        String attemptsKey = "otp:" + TEST_USERNAME + ":attempts";
        
        when(valueOperations.increment(attemptsKey, 1)).thenReturn(1L);
        when(otpGenerator.generateOTP(TEST_USERNAME)).thenReturn(TEST_OTP);

        // When
        Boolean result = otpService.generateOtp(TEST_USERNAME, "");

        // Then
        assertFalse(result);
        verify(otpGenerator).generateOTP(TEST_USERNAME);
        verify(emailService, never()).sendSimpleMessageAsync(any(EmailDTO.class));
        verify(otpAuditEntryRepository, never()).save(any(OtpAuditEntry.class));
    }

    @Test
    @DisplayName("Should return false when email sending fails")
    void shouldReturnFalseWhenEmailSendingFails() {
        // Given
        String attemptsKey = "otp:" + TEST_USERNAME + ":attempts";
        
        when(valueOperations.increment(attemptsKey, 1)).thenReturn(1L);
        when(otpGenerator.generateOTP(TEST_USERNAME)).thenReturn(TEST_OTP);
        when(emailService.sendSimpleMessageAsync(any(EmailDTO.class)))
            .thenReturn(CompletableFuture.completedFuture(false));

        // When
        Boolean result = otpService.generateOtp(TEST_USERNAME, TEST_EMAIL);

        // Then
        assertFalse(result);
        verify(otpGenerator).generateOTP(TEST_USERNAME);
        verify(emailService, times(2)).sendSimpleMessageAsync(argThat(email ->
            email.getRecipients().equals(List.of(TEST_EMAIL))
        ));
        verify(emailService).sendSimpleMessageAsync(argThat(email ->
            EmailConstants.OTP_DELIVERY_FAILURE_SUBJECT.equals(email.getSubject()) &&
                email.getBody().contains(TEST_USERNAME)
        ));
        verify(otpAuditEntryRepository, never()).save(any(OtpAuditEntry.class));
    }

    @Test
    @DisplayName("Should return false when validating incorrect OTP")
    void shouldReturnFalseWhenValidatingIncorrectOtp() {
        // Given
        Integer incorrectOtp = 999999;
        when(otpGenerator.validateOtpStatus(TEST_USERNAME, incorrectOtp)).thenReturn(OtpValidationResult.invalid());

        // When
        OtpValidationResult result = otpService.validateOTP(TEST_USERNAME, incorrectOtp);

        // Then
        assertFalse(result.isSuccess());
        verify(otpGenerator).validateOtpStatus(TEST_USERNAME, incorrectOtp);
    }

    @Test
    @DisplayName("Should not set expiry on subsequent attempts within window")
    void shouldNotSetExpiryOnSubsequentAttemptsWithinWindow() {
        // Given
        String attemptsKey = "otp:" + TEST_USERNAME + ":attempts";
        
        when(valueOperations.increment(attemptsKey, 1)).thenReturn(2L); // Second attempt
        when(otpGenerator.generateOTP(TEST_USERNAME)).thenReturn(TEST_OTP);
        when(emailService.sendSimpleMessageAsync(any(EmailDTO.class))).thenReturn(CompletableFuture.completedFuture(true));

        // When
        Boolean result = otpService.generateOtp(TEST_USERNAME, TEST_EMAIL);

        // Then
        assertTrue(result);
        verify(valueOperations).increment(attemptsKey, 1);
        verify(redisTemplate, never()).expire(eq(attemptsKey), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("Should create correct audit entry with proper dates")
    void shouldCreateCorrectAuditEntryWithProperDates() {
        // Given
        String attemptsKey = "otp:" + TEST_USERNAME + ":attempts";
        
        when(valueOperations.increment(attemptsKey, 1)).thenReturn(1L);
        when(otpGenerator.generateOTP(TEST_USERNAME)).thenReturn(TEST_OTP);
        when(emailService.sendSimpleMessageAsync(any(EmailDTO.class))).thenReturn(CompletableFuture.completedFuture(true));

        // When
        Boolean result = otpService.generateOtp(TEST_USERNAME, TEST_EMAIL);

        // Then
        assertTrue(result);
        
        ArgumentCaptor<OtpAuditEntry> auditCaptor = ArgumentCaptor.forClass(OtpAuditEntry.class);
        verify(otpAuditEntryRepository).save(auditCaptor.capture());
        
        OtpAuditEntry savedAudit = auditCaptor.getValue();
        assertEquals(TEST_USERNAME, savedAudit.getUsername());
        assertEquals(LocalDate.now(), savedAudit.getIssuedOn());
        
        LocalDate expectedExpiryDate = LocalDate.now().plusDays(EXPIRY_MINUTES / 1440);
        assertEquals(expectedExpiryDate, savedAudit.getExpiresOn());
        assertEquals(expectedExpiryDate.toString(), savedAudit.getPartnerExpiry());
    }

    @Test
    @DisplayName("Should handle whitespace email as blank")
    void shouldHandleWhitespaceEmailAsBlank() {
        // Given
        String attemptsKey = "otp:" + TEST_USERNAME + ":attempts";
        
        when(valueOperations.increment(attemptsKey, 1)).thenReturn(1L);
        when(otpGenerator.generateOTP(TEST_USERNAME)).thenReturn(TEST_OTP);

        // When
        Boolean result = otpService.generateOtp(TEST_USERNAME, "   ");

        // Then
        assertFalse(result);
        verify(otpGenerator).generateOTP(TEST_USERNAME);
        verify(emailService, never()).sendSimpleMessageAsync(argThat(email ->
            EmailConstants.OTP_EMAIL_SUBJECT.equals(email.getSubject())
        ));
        verify(otpAuditEntryRepository, never()).save(any(OtpAuditEntry.class));
    }

    @Test
    @DisplayName("Should handle exactly max attempts allowed")
    void shouldHandleExactlyMaxAttemptsAllowed() {
        // Given
        String attemptsKey = "otp:" + TEST_USERNAME + ":attempts";
        
        when(valueOperations.increment(attemptsKey, 1)).thenReturn((long) MAX_ATTEMPTS);
        when(otpGenerator.generateOTP(TEST_USERNAME)).thenReturn(TEST_OTP);
        when(emailService.sendSimpleMessageAsync(any(EmailDTO.class))).thenReturn(CompletableFuture.completedFuture(true));

        // When
        Boolean result = otpService.generateOtp(TEST_USERNAME, TEST_EMAIL);

        // Then
        assertTrue(result);
        verify(valueOperations).increment(attemptsKey, 1);
        verify(otpGenerator).generateOTP(TEST_USERNAME);
        verify(emailService).sendSimpleMessageAsync(any(EmailDTO.class));
        verify(otpAuditEntryRepository).save(any(OtpAuditEntry.class));
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