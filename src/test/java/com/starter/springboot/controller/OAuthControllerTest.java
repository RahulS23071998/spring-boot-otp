package com.starter.springboot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.starter.springboot.dto.GoogleTokenDTO;
import com.starter.springboot.dto.OtpGenerationResult;
import com.starter.springboot.entity.RefreshToken;
import com.starter.springboot.entity.User;
import com.starter.springboot.security.jwt.ITokenProvider;
import com.starter.springboot.security.jwt.JWTToken;
import com.starter.springboot.service.IGoogleOAuthService;
import com.starter.springboot.service.IOtpAuditService;
import com.starter.springboot.service.IOtpRateLimiter;
import com.starter.springboot.service.IPasswordSetupService;
import com.starter.springboot.service.IRefreshTokenService;
import com.starter.springboot.service.ITemporaryPasswordTokenService;
import com.starter.springboot.service.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuthController Tests")
class OAuthControllerTest {

    @Mock
    private IGoogleOAuthService googleOAuthService;

    @Mock
    private IUserService userService;

    @Mock
    private ITokenProvider tokenProvider;

    @Mock
    private IRefreshTokenService refreshTokenService;

    @Mock
    private IOtpRateLimiter otpRateLimiter;

    @Mock
    private IOtpAuditService otpAuditService;

    @Mock
    private ITemporaryPasswordTokenService temporaryPasswordTokenService;

    @Mock
    private IPasswordSetupService passwordSetupService;

    @InjectMocks
    private OAuthController oAuthController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private GoogleTokenDTO validGoogleTokenDTO;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(oAuthController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        validGoogleTokenDTO = new GoogleTokenDTO();
        validGoogleTokenDTO.setIdToken("valid-google-id-token");
        validGoogleTokenDTO.setClientId("web-client");
        validGoogleTokenDTO.setDeviceId("device-001");
        validGoogleTokenDTO.setRememberMe(false);

        lenient().when(otpRateLimiter.checkRateLimit(any())).thenReturn(null);
        lenient().when(otpRateLimiter.checkAndIncrementAttempts(any())).thenReturn(null);
    }

    @Test
    @DisplayName("Should successfully authenticate with Google OAuth")
    void testSuccessfulGoogleOAuthAuthentication() throws Exception {
        // Arrange
        Map<String, Object> googleUserInfo = new HashMap<>();
        googleUserInfo.put("email", "diona.smith@example.com");
        googleUserInfo.put("sub", "google-id-123");

        User user = new User();
        user.setId(1L);
        user.setUsername("diona.smith@example.com");
        user.setPasswordSet(true);

        JWTToken jwtToken = JWTToken.bearerToken("access-token", 3600L);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token");

        when(googleOAuthService.verifyAndExtractUserInfo("valid-google-id-token")).thenReturn(googleUserInfo);
        when(userService.findOrCreateGoogleOAuthUser(googleUserInfo)).thenReturn(user);
        when(tokenProvider.createAccessTokenAfterVerifiedOtp(user.getUsername(), false)).thenReturn(jwtToken);
        when(tokenProvider.getRefreshTokenValidityInSeconds()).thenReturn(604800L);
        when(refreshTokenService.createRefreshToken(user.getId(), 604800L)).thenReturn(refreshToken);

        // Act & Assert
        mockMvc.perform(post("/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validGoogleTokenDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("diona.smith@example.com")))
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.token.id_token", is("access-token")))
                .andExpect(jsonPath("$.token.refresh_token", is("refresh-token")));

        verify(googleOAuthService).verifyAndExtractUserInfo("valid-google-id-token");
        verify(userService).findOrCreateGoogleOAuthUser(googleUserInfo);
        verify(otpAuditService).persistAuditEntry(user.getUsername());
    }

    @Test
    @DisplayName("Should return ACCEPTED when password setup is required")
    void testGoogleOAuthPasswordSetupRequired() throws Exception {
        // Arrange
        Map<String, Object> googleUserInfo = new HashMap<>();
        googleUserInfo.put("email", "new.user@example.com");

        User user = new User();
        user.setUsername("new.user@example.com");
        user.setPasswordSet(false);

        when(googleOAuthService.verifyAndExtractUserInfo("valid-google-id-token")).thenReturn(googleUserInfo);
        when(userService.findOrCreateGoogleOAuthUser(googleUserInfo)).thenReturn(user);
        when(temporaryPasswordTokenService.generateTemporaryToken(user.getUsername())).thenReturn("temp-token");

        // Act & Assert
        mockMvc.perform(post("/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validGoogleTokenDTO)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status", is("SET_PASSWORD_REQUIRED")))
                .andExpect(jsonPath("$.username", is("new.user@example.com")))
                .andExpect(jsonPath("$.temporary_token", is("temp-token")));

        verify(passwordSetupService).sendPasswordActivationNotification(user, "temp-token");
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED for invalid Google token")
    void testGoogleOAuthWithInvalidToken() throws Exception {
        // Arrange
        when(googleOAuthService.verifyAndExtractUserInfo("valid-google-id-token")).thenReturn(null);

        // Act & Assert
        mockMvc.perform(post("/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validGoogleTokenDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is("FAILED")))
                .andExpect(jsonPath("$.message", is("Invalid Google ID token")));
    }

    @Test
    @DisplayName("Should return TOO_MANY_REQUESTS when rate limit exceeded")
    void testGoogleOAuthRateLimitExceeded() throws Exception {
        // Arrange
        when(otpRateLimiter.checkRateLimit(anyString()))
                .thenReturn(OtpGenerationResult.rateLimited(60));

        // Act & Assert
        mockMvc.perform(post("/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validGoogleTokenDTO)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status", is("FAILED")))
                .andExpect(jsonPath("$.message", is("Please wait 60 seconds before requesting a new OTP.")));
    }
}
