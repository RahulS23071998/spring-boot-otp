package com.starter.springboot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starter.springboot.dto.GoogleTokenDTO;
import com.starter.springboot.dto.OtpValidationResult;
import com.starter.springboot.dto.RefreshTokenRequestDTO;
import com.starter.springboot.entity.AuthType;
import com.starter.springboot.entity.RefreshToken;
import com.starter.springboot.entity.User;
import com.starter.springboot.entity.UserStatus;
import com.starter.springboot.exception.OtpRequiredException;
import com.starter.springboot.dto.LoginDTO;
import com.starter.springboot.dto.VerifyTokenRequestDTO;
import com.starter.springboot.repository.UserRepository;
import com.starter.springboot.security.jwt.JWTToken;
import com.starter.springboot.security.jwt.TokenCreationResponse;
import com.starter.springboot.security.jwt.ITokenProvider;
import com.starter.springboot.service.IGoogleOAuthService;
import com.starter.springboot.service.IOtpService;
import com.starter.springboot.service.IOtpRateLimiter;
import com.starter.springboot.service.IOtpAuditService;
import com.starter.springboot.service.IRefreshTokenService;
import com.starter.springboot.service.IUserService;
import com.starter.springboot.service.LocalizationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Optional;

import static org.hamcrest.Matchers.is;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationController Tests")
class AuthenticationControllerTest {

    @Mock
    private ITokenProvider tokenProvider;

    @Mock
    private IOtpService otpService;

    @Mock
    private IRefreshTokenService refreshTokenService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private LocalizationService localizationService;

    @Mock
    private Authentication authentication;

    @Mock
    private IGoogleOAuthService googleOAuthService;

    @Mock
    private IUserService userService;

    @Mock
    private IOtpRateLimiter otpRateLimiter;

    @Mock
    private IOtpAuditService otpAuditService;

    @InjectMocks
    private AuthenticationController authenticationController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private LoginDTO validLoginDTO;
    private VerifyTokenRequestDTO validVerifyRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authenticationController).build();
        objectMapper = new ObjectMapper();
        
        // Setup valid LoginDTO
        validLoginDTO = new LoginDTO();
        validLoginDTO.setUsername("diona.smith");
        validLoginDTO.setPassword("SecurePass123");
        validLoginDTO.setRememberMe(false);
        validLoginDTO.setClientId("web-app-client");
        validLoginDTO.setDeviceId("mobile-device-001");

        // Setup valid VerifyTokenRequestDTO
        validVerifyRequest = new VerifyTokenRequestDTO();
        validVerifyRequest.setUsername("diona.smith");
        validVerifyRequest.setOtp(123456);
        validVerifyRequest.setRememberMe(false);
        validVerifyRequest.setClientId("web-app-client");
        validVerifyRequest.setDeviceId("mobile-device-001");
        
        lenient().when(otpRateLimiter.checkRateLimit(any())).thenReturn(null);
        lenient().when(otpRateLimiter.checkAndIncrementAttempts(any())).thenReturn(null);
        
        mockLocalizationMessages();
    }

    private void mockLocalizationMessages() {
        lenient().when(localizationService.getMessage("auth.invalid_credentials"))
                .thenReturn("Invalid credentials");
        lenient().when(localizationService.getMessage("auth.invalid_otp"))
                .thenReturn("Invalid OTP provided.");
        lenient().when(localizationService.getMessage("auth.locked_otp"))
                .thenReturn("Account locked");
        lenient().when(localizationService.getMessage("auth.invalid_refresh_token"))
                .thenReturn("Invalid refresh token");
    }

    @Test
    @DisplayName("Should successfully authenticate user without OTP")
    void testSuccessfulAuthenticationWithoutOtp() throws Exception {
        // Arrange
        JWTToken jwtToken = JWTToken.bearerToken("valid-jwt-token", 3600L);
        TokenCreationResponse tokenResponse = TokenCreationResponse.accepted(jwtToken);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(tokenProvider.createToken(eq(authentication), eq(false)))
            .thenReturn(tokenResponse);

        // Act & Assert
        mockMvc.perform(post("/auth/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("diona.smith")))
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.token.id_token", is("valid-jwt-token")))
                .andExpect(jsonPath("$.token.token_type", is("Bearer")))
                .andExpect(jsonPath("$.token.expires_in", is(3600)))
                .andExpect(jsonPath("$.otp_required", is(false)))
                .andExpect(jsonPath("$.remember_me", is(false)))
                .andExpect(jsonPath("$.client_id", is("web-app-client")))
                .andExpect(jsonPath("$.device_id", is("mobile-device-001")));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenProvider).createToken(eq(authentication), eq(false));
    }

    @Test
    @DisplayName("Should successfully verify OTP and return JWT token")
    void testSuccessfulOtpVerification() throws Exception {
        // Arrange
        JWTToken jwtToken = JWTToken.bearerToken("verified-jwt-token", 3600L);

        when(otpService.validateOTP("diona.smith", 123456)).thenReturn(OtpValidationResult.success());
        when(tokenProvider.createTokenAfterVerifiedOtp("diona.smith", false)).thenReturn(jwtToken);

        // Act & Assert
        mockMvc.perform(post("/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validVerifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("diona.smith")))
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.message", is("Authentication successful")))
                .andExpect(jsonPath("$.token.id_token", is("verified-jwt-token")))
                .andExpect(jsonPath("$.token.token_type", is("Bearer")))
                .andExpect(jsonPath("$.token.expires_in", is(3600)))
                .andExpect(jsonPath("$.otp_required", is(false)))
                .andExpect(jsonPath("$.remember_me", is(false)))
                .andExpect(jsonPath("$.client_id", is("web-app-client")))
                .andExpect(jsonPath("$.device_id", is("mobile-device-001")));

        verify(otpService).validateOTP("diona.smith", 123456);
        verify(tokenProvider).createTokenAfterVerifiedOtp("diona.smith", false);
    }

    @Test
    @DisplayName("Should return OTP_PENDING status when OTP is required")
    void testAuthenticationRequiresOtp() throws Exception {
        // Arrange
        TokenCreationResponse tokenResponse = TokenCreationResponse.pendingOtp("OTP required for authentication");
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(tokenProvider.createToken(eq(authentication), eq(false)))
            .thenReturn(tokenResponse);

        // Act & Assert
        mockMvc.perform(post("/auth/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginDTO)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.username", is("diona.smith")))
                .andExpect(jsonPath("$.status", is("OTP_PENDING")))
                .andExpect(jsonPath("$.message", is("OTP required for authentication")))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.otp_required", is(true)));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenProvider).createToken(eq(authentication), eq(false));
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED for invalid username or password")
    void testInvalidUsernameOrPassword() throws Exception {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        mockMvc.perform(post("/auth/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.username", is("diona.smith")))
                .andExpect(jsonPath("$.status", is("FAILED")))
                .andExpect(jsonPath("$.message", is("Invalid credentials")))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.otp_required", is(false)))
                .andExpect(jsonPath("$.remember_me", is(false)))
                .andExpect(jsonPath("$.client_id", is("web-app-client")))
                .andExpect(jsonPath("$.device_id", is("mobile-device-001")));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Should return FORBIDDEN for authentication exceptions")
    void testAuthenticationExceptionHandling() throws Exception {
        // Arrange
        AuthenticationException authException = new AuthenticationException("Account locked") {};
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(authException);

        // Act & Assert
        mockMvc.perform(post("/auth/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginDTO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.username", is("diona.smith")))
                .andExpect(jsonPath("$.status", is("FAILED")))
                .andExpect(jsonPath("$.message", is("Account locked")))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.otp_required", is(false)));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED for invalid OTP")
    void testInvalidOtpVerification() throws Exception {
        // Arrange
        when(otpService.validateOTP("diona.smith", 123456)).thenReturn(OtpValidationResult.invalid());

        // Act & Assert
        mockMvc.perform(post("/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validVerifyRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.username", is("diona.smith")))
                .andExpect(jsonPath("$.status", is("FAILED")))
                .andExpect(jsonPath("$.message", is("Invalid OTP provided.")))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.otp_required", is(false)))
                .andExpect(jsonPath("$.remember_me", is(false)))
                .andExpect(jsonPath("$.client_id", is("web-app-client")))
                .andExpect(jsonPath("$.device_id", is("mobile-device-001")));

        verify(otpService).validateOTP("diona.smith", 123456);
    }

    @Test
    @DisplayName("Should propagate OtpRequiredException as ServletException")
    void testOtpRequiredExceptionHandling() throws Exception {
        // Arrange
        OtpRequiredException otpException = new OtpRequiredException("OTP verification required");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(tokenProvider.createToken(eq(authentication), eq(false)))
            .thenThrow(otpException);

        // Act & Assert - The exception should bubble up as ServletException in MockMvc
        try {
            mockMvc.perform(post("/auth/authenticate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validLoginDTO)));
            // Should not reach here - expecting exception
            Assertions.fail("Expected ServletException to be thrown");
        } catch (Exception e) {
            // Verify that the root cause is the OtpRequiredException
            Assertions.assertInstanceOf(OtpRequiredException.class, e.getCause());
            Assertions.assertEquals("OTP verification required", e.getCause().getMessage());
        }

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenProvider).createToken(eq(authentication), eq(false));
    }

    @Test
    @DisplayName("Should handle authentication with remember me flag")
    void testTokenCreationWithRememberMe() throws Exception {
        // Arrange
        validLoginDTO.setRememberMe(true);
        JWTToken jwtToken = JWTToken.bearerToken("remember-me-token", 86400L); // 24 hours
        TokenCreationResponse tokenResponse = TokenCreationResponse.accepted(jwtToken);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(tokenProvider.createToken(eq(authentication), eq(true)))
            .thenReturn(tokenResponse);

        // Act & Assert
        mockMvc.perform(post("/auth/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("diona.smith")))
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.token.id_token", is("remember-me-token")))
                .andExpect(jsonPath("$.token.expires_in", is(86400)))
                .andExpect(jsonPath("$.remember_me", is(true)));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenProvider).createToken(eq(authentication), eq(true));
    }

    @Test
    @DisplayName("Should successfully refresh token with valid refresh token")
    void testSuccessfulTokenRefresh() throws Exception {
        // Arrange
        String oldRefreshToken = "old-refresh-token-value";
        String newRefreshToken = "new-refresh-token-value";
        String newAccessToken = "new-access-token";
        Long userId = 1L;
        
        RefreshTokenRequestDTO refreshRequest = new RefreshTokenRequestDTO(oldRefreshToken);
        
        User user = new User();
        user.setId(userId);
        user.setUsername("diona.smith");
        
        RefreshToken oldToken = new RefreshToken();
        oldToken.setId(1L);
        oldToken.setUserId(userId);
        oldToken.setToken(oldRefreshToken);
        oldToken.setExpiresAt(Instant.now().plusSeconds(604800));
        oldToken.setRevokedAt(null);
        
        RefreshToken newToken = new RefreshToken();
        newToken.setId(2L);
        newToken.setUserId(userId);
        newToken.setToken(newRefreshToken);
        newToken.setExpiresAt(Instant.now().plusSeconds(604800));
        newToken.setRevokedAt(null);
        
        JWTToken accessToken = JWTToken.bearerToken(newAccessToken, 3600L);
        JWTToken finalToken = new JWTToken(newAccessToken, newRefreshToken, "Bearer", 3600L, 604800L);
        
        when(refreshTokenService.validateRefreshToken(oldRefreshToken)).thenReturn(oldToken);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(refreshTokenService.rotateRefreshToken(oldRefreshToken, userId, 604800L)).thenReturn(newToken);
        when(tokenProvider.createAccessTokenAfterVerifiedOtp("diona.smith", false)).thenReturn(accessToken);
        when(tokenProvider.getRefreshTokenValidityInSeconds()).thenReturn(604800L);
        
        // Act & Assert
        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("diona.smith")))
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.token.id_token", is(newAccessToken)))
                .andExpect(jsonPath("$.token.refresh_token", is(newRefreshToken)))
                .andExpect(jsonPath("$.token.token_type", is("Bearer")))
                .andExpect(jsonPath("$.token.expires_in", is(3600)))
                .andExpect(jsonPath("$.token.refresh_token_expires_in", is(604800)));
        
        verify(refreshTokenService).validateRefreshToken(oldRefreshToken);
        verify(userRepository).findById(userId);
        verify(refreshTokenService).rotateRefreshToken(oldRefreshToken, userId, 604800L);
        verify(tokenProvider).createAccessTokenAfterVerifiedOtp("diona.smith", false);
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED for invalid refresh token")
    void testRefreshTokenWithInvalidToken() throws Exception {
        // Arrange
        String invalidRefreshToken = "invalid-refresh-token";
        RefreshTokenRequestDTO refreshRequest = new RefreshTokenRequestDTO(invalidRefreshToken);
        
        when(refreshTokenService.validateRefreshToken(invalidRefreshToken))
                .thenThrow(new RuntimeException("Invalid refresh token"));
        
        // Act & Assert
        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is("FAILED")))
                .andExpect(jsonPath("$.message", is("Invalid refresh token")));
        
        verify(refreshTokenService).validateRefreshToken(invalidRefreshToken);
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED for expired refresh token")
    void testRefreshTokenWithExpiredToken() throws Exception {
        // Arrange
        String expiredRefreshToken = "expired-refresh-token";
        RefreshTokenRequestDTO refreshRequest = new RefreshTokenRequestDTO(expiredRefreshToken);
        
        when(refreshTokenService.validateRefreshToken(expiredRefreshToken))
                .thenThrow(new RuntimeException("Invalid refresh token"));
        
        // Act & Assert
        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is("FAILED")))
                .andExpect(jsonPath("$.message", is("Invalid refresh token")));
        
        verify(refreshTokenService).validateRefreshToken(expiredRefreshToken);
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED when user not found during refresh")
    void testRefreshTokenWhenUserNotFound() throws Exception {
        // Arrange
        String refreshTokenValue = "valid-refresh-token";
        Long userId = 999L;
        
        RefreshTokenRequestDTO refreshRequest = new RefreshTokenRequestDTO(refreshTokenValue);
        
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(1L);
        refreshToken.setUserId(userId);
        refreshToken.setToken(refreshTokenValue);
        refreshToken.setExpiresAt(Instant.now().plusSeconds(604800));
        refreshToken.setRevokedAt(null);
        
        when(refreshTokenService.validateRefreshToken(refreshTokenValue)).thenReturn(refreshToken);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        
        // Act & Assert
        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is("FAILED")))
                .andExpect(jsonPath("$.message", is("Invalid refresh token")));
        
        verify(refreshTokenService).validateRefreshToken(refreshTokenValue);
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("Should perform token rotation during refresh")
    void testTokenRotationDuringRefresh() throws Exception {
        // Arrange
        String oldRefreshToken = "old-refresh-token-value";
        String newRefreshToken = "new-refresh-token-value";
        String newAccessToken = "new-access-token";
        Long userId = 1L;
        
        RefreshTokenRequestDTO refreshRequest = new RefreshTokenRequestDTO(oldRefreshToken);
        
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        
        RefreshToken oldToken = new RefreshToken();
        oldToken.setId(1L);
        oldToken.setUserId(userId);
        oldToken.setToken(oldRefreshToken);
        oldToken.setExpiresAt(Instant.now().plusSeconds(604800));
        oldToken.setRevokedAt(null);
        
        RefreshToken newToken = new RefreshToken();
        newToken.setId(2L);
        newToken.setUserId(userId);
        newToken.setToken(newRefreshToken);
        newToken.setExpiresAt(Instant.now().plusSeconds(604800));
        newToken.setRevokedAt(null);
        
        JWTToken accessToken = JWTToken.bearerToken(newAccessToken, 3600L);
        JWTToken finalToken = new JWTToken(newAccessToken, newRefreshToken, "Bearer", 3600L, 604800L);
        
        when(refreshTokenService.validateRefreshToken(oldRefreshToken)).thenReturn(oldToken);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(refreshTokenService.rotateRefreshToken(oldRefreshToken, userId, 604800L)).thenReturn(newToken);
        when(tokenProvider.createAccessTokenAfterVerifiedOtp("testuser", false)).thenReturn(accessToken);
        when(tokenProvider.getRefreshTokenValidityInSeconds()).thenReturn(604800L);
        
        // Act & Assert
        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token.refresh_token", is(newRefreshToken)));
        
        verify(refreshTokenService).rotateRefreshToken(oldRefreshToken, userId, 604800L);
    }

    @Test
    @DisplayName("Should return BAD_REQUEST for missing refresh token")
    void testRefreshTokenWithMissingToken() throws Exception {
        // Arrange
        String jsonPayload = "{}";
        
        // Act & Assert
        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle refresh token service exception gracefully")
    void testRefreshTokenWithServiceException() throws Exception {
        // Arrange
        String refreshTokenValue = "problematic-token";
        RefreshTokenRequestDTO refreshRequest = new RefreshTokenRequestDTO(refreshTokenValue);
        
        when(refreshTokenService.validateRefreshToken(refreshTokenValue))
                .thenThrow(new RuntimeException("Database error"));
        
        // Act & Assert
        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is("FAILED")))
                .andExpect(jsonPath("$.message", is("Invalid refresh token")));
    }

    @Test
    @DisplayName("Should successfully authenticate with valid Google OAuth token")
    void testSuccessfulGoogleOAuthAuthentication() throws Exception {
        // Arrange
        String validGoogleToken = "valid-google-id-token";
        GoogleTokenDTO googleTokenDTO = new GoogleTokenDTO(validGoogleToken);
        googleTokenDTO.setRememberMe(true);
        googleTokenDTO.setClientId("postman");
        googleTokenDTO.setDeviceId("postman-test");

        User googleUser = createGoogleUser(1L, "user@gmail.com", "Google", "User");
        JWTToken accessToken = JWTToken.bearerToken("access-token-123", 3600L);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(1L);
        refreshToken.setUserId(1L);
        refreshToken.setToken("refresh-token-123");

        java.util.Map<String, Object> googleUserInfo = createGoogleUserInfo("google-sub-123", "user@gmail.com", "Google", "User", "Google User");

        when(googleOAuthService.verifyAndExtractUserInfo(validGoogleToken)).thenReturn(googleUserInfo);
        when(userService.findOrCreateGoogleOAuthUser(googleUserInfo)).thenReturn(googleUser);
        when(tokenProvider.createAccessTokenAfterVerifiedOtp("user@gmail.com", true)).thenReturn(accessToken);
        when(refreshTokenService.createRefreshToken(1L, 604800L)).thenReturn(refreshToken);
        when(tokenProvider.getRefreshTokenValidityInSeconds()).thenReturn(604800L);

        // Act & Assert
        mockMvc.perform(post("/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(googleTokenDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("user@gmail.com")))
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.message", is("Authentication successful")))
                .andExpect(jsonPath("$.token.id_token", is("access-token-123")))
                .andExpect(jsonPath("$.token.token_type", is("Bearer")))
                .andExpect(jsonPath("$.token.expires_in", is(3600)))
                .andExpect(jsonPath("$.remember_me", is(true)))
                .andExpect(jsonPath("$.client_id", is("postman")))
                .andExpect(jsonPath("$.device_id", is("postman-test")))
                .andExpect(jsonPath("$.otp_required", is(false)));

        verify(googleOAuthService).verifyAndExtractUserInfo(validGoogleToken);
        verify(userService).findOrCreateGoogleOAuthUser(googleUserInfo);
        verify(tokenProvider).createAccessTokenAfterVerifiedOtp("user@gmail.com", true);
        verify(refreshTokenService).createRefreshToken(1L, 604800L);
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED for invalid Google ID token")
    void testGoogleOAuthWithInvalidToken() throws Exception {
        // Arrange
        String invalidGoogleToken = "invalid-google-id-token";
        GoogleTokenDTO googleTokenDTO = new GoogleTokenDTO(invalidGoogleToken);

        when(googleOAuthService.verifyAndExtractUserInfo(invalidGoogleToken)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(post("/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(googleTokenDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is("FAILED")))
                .andExpect(jsonPath("$.message", is("Invalid Google ID token")));

        verify(googleOAuthService).verifyAndExtractUserInfo(invalidGoogleToken);
        verify(userService, never()).findOrCreateGoogleOAuthUser(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Should return BAD_REQUEST for missing Google ID token")
    void testGoogleOAuthWithMissingToken() throws Exception {
        // Arrange
        String jsonPayload = "{}";

        // Act & Assert
        mockMvc.perform(post("/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isBadRequest());

        verify(googleOAuthService, never()).verifyAndExtractUserInfo(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("Should successfully link Google OAuth to existing user")
    void testGoogleOAuthLinkingExistingUser() throws Exception {
        // Arrange
        String validGoogleToken = "valid-google-id-token";
        GoogleTokenDTO googleTokenDTO = new GoogleTokenDTO(validGoogleToken);
        googleTokenDTO.setRememberMe(false);

        User existingUser = createGoogleUser(2L, "existing@gmail.com", "Existing", "User");
        existingUser.setAuthType(AuthType.GOOGLE_OAUTH);
        existingUser.setGoogleId("google-sub-456");

        JWTToken accessToken = JWTToken.bearerToken("access-token-456", 3600L);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(2L);
        refreshToken.setUserId(2L);
        refreshToken.setToken("refresh-token-456");

        java.util.Map<String, Object> googleUserInfo = createGoogleUserInfo("google-sub-456", "existing@gmail.com", "Existing", "User", "Existing User");

        when(googleOAuthService.verifyAndExtractUserInfo(validGoogleToken)).thenReturn(googleUserInfo);
        when(userService.findOrCreateGoogleOAuthUser(googleUserInfo)).thenReturn(existingUser);
        when(tokenProvider.createAccessTokenAfterVerifiedOtp("existing@gmail.com", false)).thenReturn(accessToken);
        when(refreshTokenService.createRefreshToken(2L, 604800L)).thenReturn(refreshToken);
        when(tokenProvider.getRefreshTokenValidityInSeconds()).thenReturn(604800L);

        // Act & Assert
        mockMvc.perform(post("/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(googleTokenDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("existing@gmail.com")))
                .andExpect(jsonPath("$.token.id_token", is("access-token-456")))
                .andExpect(jsonPath("$.remember_me", is(false)));

        verify(userService).findOrCreateGoogleOAuthUser(googleUserInfo);
    }

    @Test
    @DisplayName("Should handle empty Google user info")
    void testGoogleOAuthWithEmptyUserInfo() throws Exception {
        // Arrange
        String validGoogleToken = "valid-google-id-token";
        GoogleTokenDTO googleTokenDTO = new GoogleTokenDTO(validGoogleToken);

        when(googleOAuthService.verifyAndExtractUserInfo(validGoogleToken)).thenReturn(java.util.Collections.emptyMap());

        // Act & Assert
        mockMvc.perform(post("/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(googleTokenDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is("FAILED")))
                .andExpect(jsonPath("$.message", is("Invalid Google ID token")));

        verify(googleOAuthService).verifyAndExtractUserInfo(validGoogleToken);
    }

    @Test
    @DisplayName("Should create new Google OAuth user without remember me flag")
    void testCreateNewGoogleOAuthUserWithoutRememberMe() throws Exception {
        // Arrange
        String validGoogleToken = "valid-google-id-token";
        GoogleTokenDTO googleTokenDTO = new GoogleTokenDTO(validGoogleToken);

        User newGoogleUser = createGoogleUser(3L, "newuser@gmail.com", "New", "User");
        JWTToken accessToken = JWTToken.bearerToken("access-token-789", 3600L);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(3L);
        refreshToken.setUserId(3L);
        refreshToken.setToken("refresh-token-789");

        java.util.Map<String, Object> googleUserInfo = createGoogleUserInfo("google-sub-789", "newuser@gmail.com", "New", "User", "New User");

        when(googleOAuthService.verifyAndExtractUserInfo(validGoogleToken)).thenReturn(googleUserInfo);
        when(userService.findOrCreateGoogleOAuthUser(googleUserInfo)).thenReturn(newGoogleUser);
        when(tokenProvider.createAccessTokenAfterVerifiedOtp(org.mockito.ArgumentMatchers.eq("newuser@gmail.com"), org.mockito.ArgumentMatchers.nullable(Boolean.class))).thenReturn(accessToken);
        when(refreshTokenService.createRefreshToken(3L, 604800L)).thenReturn(refreshToken);
        when(tokenProvider.getRefreshTokenValidityInSeconds()).thenReturn(604800L);

        // Act & Assert
        mockMvc.perform(post("/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(googleTokenDTO)))
                .andExpect(status().isOk());

        verify(refreshTokenService).createRefreshToken(3L, 604800L);
    }

    private User createGoogleUser(Long id, String email, String firstName, String lastName) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setUsername(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPassword("random-password");
        user.setEnabled(true);
        user.setStatus(UserStatus.ACTIVE);
        user.setAuthType(AuthType.GOOGLE_OAUTH);
        user.setGoogleId("google-id-" + id);
        user.setIsOtpRequired(false);
        return user;
    }

    private java.util.Map<String, Object> createGoogleUserInfo(String googleId, String email, String givenName, String familyName, String name) {
        java.util.Map<String, Object> googleUserInfo = new java.util.HashMap<>();
        googleUserInfo.put("sub", googleId);
        googleUserInfo.put("email", email);
        googleUserInfo.put("given_name", givenName);
        googleUserInfo.put("family_name", familyName);
        googleUserInfo.put("name", name);
        googleUserInfo.put("picture", "https://example.com/picture.jpg");
        return googleUserInfo;
    }

}