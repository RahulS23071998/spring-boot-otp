package com.starter.springboot.service.impl;

import com.starter.springboot.dto.SetPasswordDTO;
import com.starter.springboot.dto.SetPasswordResponseDTO;
import com.starter.springboot.entity.User;
import com.starter.springboot.repository.UserRepository;
import com.starter.springboot.service.IEmailService;
import com.starter.springboot.service.IOtpRateLimiter;
import com.starter.springboot.service.ITemporaryPasswordTokenService;
import com.starter.springboot.service.PasswordValidationService;
import com.starter.springboot.dto.OtpGenerationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordSetupService Tests")
class PasswordSetupServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordValidationService passwordValidationService;

    @Mock
    private IEmailService emailService;

    @Mock
    private ITemporaryPasswordTokenService temporaryPasswordTokenService;

    @Mock
    private IOtpRateLimiter otpRateLimiter;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PasswordSetupService passwordSetupService;

    private static final String TEST_USERNAME = "testuser@example.com";
    private static final String TEST_PASSWORD = "NewPass123!";
    private static final String TEST_TOKEN = "test-token-123";
    private User testUser;
    private SetPasswordDTO validSetPasswordDTO;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        
        testUser = new User();
        testUser.setUsername(TEST_USERNAME);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setPasswordSet(Boolean.FALSE);
        testUser.setPassword("oldEncodedPassword");

        validSetPasswordDTO = new SetPasswordDTO();
        validSetPasswordDTO.setPassword(TEST_PASSWORD);
        validSetPasswordDTO.setConfirmPassword(TEST_PASSWORD);
        validSetPasswordDTO.setTemporaryToken(TEST_TOKEN);
    }

    @Test
    @DisplayName("Should set password with authenticated user successfully")
    void testHandlePasswordSetWithAuthentication_Success() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(TEST_USERNAME);
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(passwordValidationService.validate(TEST_PASSWORD))
                .thenReturn(new PasswordValidationService.PasswordValidationResult(true, null));
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn("encodedPassword");
        lenient().when(emailService.sendHtmlMessageAsync(any())).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(true));

        SetPasswordDTO request = new SetPasswordDTO();
        request.setPassword(TEST_PASSWORD);
        request.setConfirmPassword(TEST_PASSWORD);

        // Mock the security context
        var securityContext = mock(org.springframework.security.core.context.SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        ResponseEntity<SetPasswordResponseDTO> response = passwordSetupService
                .handlePasswordSetWithAuthentication(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SUCCESS", response.getBody().status());
        assertTrue(response.getBody().passwordSet());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should reject password set when user not authenticated")
    void testHandlePasswordSetWithAuthentication_NotAuthenticated() {
        var securityContext = mock(org.springframework.security.core.context.SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        SetPasswordDTO request = new SetPasswordDTO();
        request.setPassword(TEST_PASSWORD);

        ResponseEntity<SetPasswordResponseDTO> response = passwordSetupService
                .handlePasswordSetWithAuthentication(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("User not authenticated", response.getBody().message());
    }

    @Test
    @DisplayName("Should return 404 when authenticated user not found in database")
    void testHandlePasswordSetWithAuthentication_UserNotFound() {
        var securityContext = mock(org.springframework.security.core.context.SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(TEST_USERNAME);
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());
        SecurityContextHolder.setContext(securityContext);

        SetPasswordDTO request = new SetPasswordDTO();
        request.setPassword(TEST_PASSWORD);

        ResponseEntity<SetPasswordResponseDTO> response = passwordSetupService
                .handlePasswordSetWithAuthentication(request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("User not found", response.getBody().message());
    }

    @Test
    @DisplayName("Should set password with temporary token successfully")
    void testHandlePasswordSetWithTemporaryToken_Success() {
        when(temporaryPasswordTokenService.getUsernameFromToken(TEST_TOKEN))
                .thenReturn(TEST_USERNAME);
        when(otpRateLimiter.checkAndIncrementAttempts("password-set:" + TEST_USERNAME))
                .thenReturn(null);
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(temporaryPasswordTokenService.validateTemporaryToken(TEST_USERNAME, TEST_TOKEN))
                .thenReturn(true);
        when(passwordValidationService.validate(TEST_PASSWORD))
                .thenReturn(new PasswordValidationService.PasswordValidationResult(true, null));
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn("encodedPassword");
        lenient().when(emailService.sendHtmlMessageAsync(any())).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(true));

        ResponseEntity<SetPasswordResponseDTO> response = passwordSetupService
                .handlePasswordSetWithTemporaryToken(validSetPasswordDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SUCCESS", response.getBody().status());
        verify(temporaryPasswordTokenService).invalidateTemporaryToken(TEST_USERNAME);
        verify(otpRateLimiter).resetAttempts("password-set:" + TEST_USERNAME);
    }

    @Test
    @DisplayName("Should reject when temporary token not provided")
    void testHandlePasswordSetWithTemporaryToken_NoToken() {
        SetPasswordDTO request = new SetPasswordDTO();
        request.setPassword(TEST_PASSWORD);
        request.setTemporaryToken(null);

        ResponseEntity<SetPasswordResponseDTO> response = passwordSetupService
                .handlePasswordSetWithTemporaryToken(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Temporary token is required", response.getBody().message());
    }

    @Test
    @DisplayName("Should reject when temporary token invalid or expired")
    void testHandlePasswordSetWithTemporaryToken_InvalidToken() {
        when(temporaryPasswordTokenService.getUsernameFromToken(TEST_TOKEN))
                .thenReturn(null);

        ResponseEntity<SetPasswordResponseDTO> response = passwordSetupService
                .handlePasswordSetWithTemporaryToken(validSetPasswordDTO);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Temporary token expired or invalid", response.getBody().message());
    }

    @Test
    @DisplayName("Should return 429 when rate limit exceeded for password set")
    void testHandlePasswordSetWithTemporaryToken_RateLimitExceeded() {
        when(temporaryPasswordTokenService.getUsernameFromToken(TEST_TOKEN))
                .thenReturn(TEST_USERNAME);
        when(otpRateLimiter.checkAndIncrementAttempts("password-set:" + TEST_USERNAME))
                .thenReturn(OtpGenerationResult.maxAttemptsExceeded());

        ResponseEntity<SetPasswordResponseDTO> response = passwordSetupService
                .handlePasswordSetWithTemporaryToken(validSetPasswordDTO);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals("Too many password reset attempts. Please try again later", 
                response.getBody().message());
    }

    @Test
    @DisplayName("Should reject when token validation fails")
    void testHandlePasswordSetWithTemporaryToken_TokenValidationFails() {
        when(temporaryPasswordTokenService.getUsernameFromToken(TEST_TOKEN))
                .thenReturn(TEST_USERNAME);
        when(otpRateLimiter.checkAndIncrementAttempts("password-set:" + TEST_USERNAME))
                .thenReturn(null);
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(temporaryPasswordTokenService.validateTemporaryToken(TEST_USERNAME, TEST_TOKEN))
                .thenReturn(false);

        ResponseEntity<SetPasswordResponseDTO> response = passwordSetupService
                .handlePasswordSetWithTemporaryToken(validSetPasswordDTO);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Temporary token expired or invalid", response.getBody().message());
    }

    @Test
    @DisplayName("Should validate password before setting")
    void testUpdateUserPasswordWithValidation_InvalidPassword() {
        when(passwordValidationService.validate(TEST_PASSWORD))
                .thenReturn(new PasswordValidationService.PasswordValidationResult(false, 
                        "Password must contain uppercase letter"));

        ResponseEntity<SetPasswordResponseDTO> response = passwordSetupService
                .updateUserPasswordWithValidation(testUser, TEST_PASSWORD);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Password must contain uppercase letter", response.getBody().message());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject password set when password already set")
    void testUpdateUserPasswordWithValidation_PasswordAlreadySet() {
        testUser.setPasswordSet(Boolean.TRUE);

        ResponseEntity<SetPasswordResponseDTO> response = passwordSetupService
                .updateUserPasswordWithValidation(testUser, TEST_PASSWORD);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Password already set", response.getBody().message());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should send notification email after successful password set")
    void testUpdateUserPasswordWithValidation_SendsNotification() {
        when(passwordValidationService.validate(TEST_PASSWORD))
                .thenReturn(new PasswordValidationService.PasswordValidationResult(true, null));
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn("encodedPassword");
        when(emailService.sendHtmlMessageAsync(any())).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(true));

        ResponseEntity<SetPasswordResponseDTO> response = passwordSetupService
                .updateUserPasswordWithValidation(testUser, TEST_PASSWORD);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(emailService).sendHtmlMessageAsync(any());
    }

    @Test
    @DisplayName("Should continue operation even if email notification fails")
    void testUpdateUserPasswordWithValidation_EmailNotificationFailure() {
        when(passwordValidationService.validate(TEST_PASSWORD))
                .thenReturn(new PasswordValidationService.PasswordValidationResult(true, null));
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn("encodedPassword");
        lenient().doThrow(new RuntimeException("Email service error"))
                .when(emailService).sendHtmlMessageAsync(any());

        ResponseEntity<SetPasswordResponseDTO> response = passwordSetupService
                .updateUserPasswordWithValidation(testUser, TEST_PASSWORD);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should handle Redis exception gracefully")
    void testHandlePasswordSetWithTemporaryToken_RedisException() {
        when(temporaryPasswordTokenService.getUsernameFromToken(TEST_TOKEN))
                .thenThrow(new RuntimeException("Redis connection error"));

        ResponseEntity<SetPasswordResponseDTO> response = passwordSetupService
                .handlePasswordSetWithTemporaryToken(validSetPasswordDTO);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Failed to set password. Please try again.", response.getBody().message());
    }
}
