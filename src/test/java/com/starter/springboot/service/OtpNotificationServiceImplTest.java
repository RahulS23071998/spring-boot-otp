package com.starter.springboot.service;

import com.starter.springboot.constants.EmailConstants;
import com.starter.springboot.constants.OtpConstants;
import com.starter.springboot.dto.EmailDTO;
import com.starter.springboot.service.LocalizationService;
import com.starter.springboot.service.impl.OtpNotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OtpNotificationServiceImpl Tests")
class OtpNotificationServiceImplTest {

    @Mock
    private IEmailService emailService;

    @Mock
    private LocalizationService localizationService;

    @InjectMocks
    private OtpNotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        // Setup lenient mocks for async operations
        lenient().when(emailService.sendSimpleMessageAsync(any(EmailDTO.class)))
                .thenReturn(CompletableFuture.completedFuture(true));
        mockLocalizationMessages();
    }

    private void mockLocalizationMessages() {
        lenient().when(localizationService.getMessage(anyString()))
                .thenAnswer(invocation -> resolveMessage(invocation.getArgument(0)));
        lenient().when(localizationService.getMessage(anyString(), any()))
                .thenAnswer(invocation -> {
                    Object[] args = extractArgs(invocation.getArguments());
                    return resolveMessage(invocation.getArgument(0), args);
                });
    }

    private Object[] extractArgs(Object[] invocationArgs) {
        if (invocationArgs.length <= 1) {
            return new Object[0];
        }
        Object secondArg = invocationArgs[1];
        if (secondArg instanceof Object[] array) {
            return array;
        }
        Object[] args = new Object[invocationArgs.length - 1];
        System.arraycopy(invocationArgs, 1, args, 0, args.length);
        return args;
    }

    private String resolveMessage(String key, Object... args) {
        return switch (key) {
            case "auth.max_attempts_exceeded" -> OtpConstants.MAX_ATTEMPTS_EXCEEDED_MESSAGE;
            case "auth.otp_delivery_failure" -> format(OtpConstants.OTP_DELIVERY_FAILURE_MESSAGE_TEMPLATE, args);
            default -> key;
        };
    }

    private String format(String template, Object... args) {
        return (args == null || args.length == 0) ? template : String.format(template, args);
    }

    @Test
    @DisplayName("Should send OTP email successfully")
    void shouldSendOtpEmailSuccessfully() {
        // Given
        String key = "testUser";
        String userEmail = "test@example.com";
        Integer otpValue = 123456;

        // When
        CompletableFuture<Boolean> result = notificationService.sendOtpEmailAsync(key, userEmail, otpValue);

        // Then
        assertTrue(result.join());
        ArgumentCaptor<EmailDTO> emailCaptor = ArgumentCaptor.forClass(EmailDTO.class);
        verify(emailService).sendSimpleMessageAsync(emailCaptor.capture());

        EmailDTO sentEmail = emailCaptor.getValue();
        assertEquals(EmailConstants.OTP_EMAIL_SUBJECT, sentEmail.getSubject());
        assertEquals(EmailConstants.OTP_EMAIL_BODY_PREFIX + otpValue, sentEmail.getBody());
        assertEquals(List.of(userEmail), sentEmail.getRecipients());
    }

    @Test
    @DisplayName("Should return false for null email")
    void shouldReturnFalseForNullEmail() {
        // Given
        String key = "testUser";
        String userEmail = null;
        Integer otpValue = 123456;

        // When
        CompletableFuture<Boolean> result = notificationService.sendOtpEmailAsync(key, userEmail, otpValue);

        // Then
        assertFalse(result.join());
        verify(emailService, never()).sendSimpleMessageAsync(any());
    }

    @Test
    @DisplayName("Should return false for blank email")
    void shouldReturnFalseForBlankEmail() {
        // Given
        String key = "testUser";
        String userEmail = "   ";
        Integer otpValue = 123456;

        // When
        CompletableFuture<Boolean> result = notificationService.sendOtpEmailAsync(key, userEmail, otpValue);

        // Then
        assertFalse(result.join());
        verify(emailService, never()).sendSimpleMessageAsync(any());
    }

    @Test
    @DisplayName("Should handle email service exception")
    void shouldHandleEmailServiceException() {
        // Given
        String key = "testUser";
        String userEmail = "test@example.com";
        Integer otpValue = 123456;

        when(emailService.sendSimpleMessageAsync(any(EmailDTO.class)))
                .thenThrow(new RuntimeException("Email service error"));

        // When
        CompletableFuture<Boolean> result = notificationService.sendOtpEmailAsync(key, userEmail, otpValue);

        // Then
        assertFalse(result.join());
        verify(emailService).sendSimpleMessageAsync(any());
    }

    @Test
    @DisplayName("Should notify lockout successfully")
    void shouldNotifyLockoutSuccessfully() {
        // Given
        String key = "testUser";
        String userEmail = "test@example.com";

        // When
        notificationService.notifyLockout(key, userEmail);

        // Then
        ArgumentCaptor<EmailDTO> emailCaptor = ArgumentCaptor.forClass(EmailDTO.class);
        verify(emailService).sendSimpleMessageAsync(emailCaptor.capture());

        EmailDTO sentEmail = emailCaptor.getValue();
        assertEquals(EmailConstants.OTP_LOCKED_EMAIL_SUBJECT, sentEmail.getSubject());
        assertEquals(OtpConstants.MAX_ATTEMPTS_EXCEEDED_MESSAGE, sentEmail.getBody());
        assertEquals(List.of(userEmail), sentEmail.getRecipients());
    }

    @Test
    @DisplayName("Should skip lockout notification for null email")
    void shouldSkipLockoutNotificationForNullEmail() {
        // Given
        String key = "testUser";
        String userEmail = null;

        // When
        notificationService.notifyLockout(key, userEmail);

        // Then
        verify(emailService, never()).sendSimpleMessageAsync(any());
    }

    @Test
    @DisplayName("Should skip lockout notification for blank email")
    void shouldSkipLockoutNotificationForBlankEmail() {
        // Given
        String key = "testUser";
        String userEmail = "   ";

        // When
        notificationService.notifyLockout(key, userEmail);

        // Then
        verify(emailService, never()).sendSimpleMessageAsync(any());
    }

    @Test
    @DisplayName("Should notify delivery failure successfully")
    void shouldNotifyDeliveryFailureSuccessfully() {
        // Given
        String key = "testUser";
        String userEmail = "test@example.com";

        // When
        notificationService.notifyDeliveryFailure(userEmail, key);

        // Then
        ArgumentCaptor<EmailDTO> emailCaptor = ArgumentCaptor.forClass(EmailDTO.class);
        verify(emailService).sendSimpleMessageAsync(emailCaptor.capture());

        EmailDTO sentEmail = emailCaptor.getValue();
        assertEquals(EmailConstants.OTP_DELIVERY_FAILURE_SUBJECT, sentEmail.getSubject());
        assertEquals(String.format(OtpConstants.OTP_DELIVERY_FAILURE_MESSAGE_TEMPLATE, key), sentEmail.getBody());
        assertEquals(List.of(userEmail), sentEmail.getRecipients());
    }

    @Test
    @DisplayName("Should skip delivery failure notification for null email")
    void shouldSkipDeliveryFailureNotificationForNullEmail() {
        // Given
        String key = "testUser";
        String userEmail = null;

        // When
        notificationService.notifyDeliveryFailure(userEmail, key);

        // Then
        verify(emailService, never()).sendSimpleMessageAsync(any());
    }

    @Test
    @DisplayName("Should skip delivery failure notification for blank email")
    void shouldSkipDeliveryFailureNotificationForBlankEmail() {
        // Given
        String key = "testUser";
        String userEmail = "   ";

        // When
        notificationService.notifyDeliveryFailure(userEmail, key);

        // Then
        verify(emailService, never()).sendSimpleMessageAsync(any());
    }
}