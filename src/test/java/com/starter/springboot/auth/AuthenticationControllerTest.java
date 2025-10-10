package com.starter.springboot.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starter.springboot.exceptions.OtpRequiredException;
import com.starter.springboot.rest.dto.LoginDTO;
import com.starter.springboot.rest.dto.VerifyTokenRequestDTO;
import com.starter.springboot.security.jwt.JWTToken;
import com.starter.springboot.security.jwt.TokenCreationResponse;
import com.starter.springboot.security.jwt.TokenProvider;
import com.starter.springboot.services.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.is;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationController Tests")
class AuthenticationControllerTest {

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private OtpService otpService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private Authentication authentication;

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
        validLoginDTO.setUsername("testuser");
        validLoginDTO.setPassword("password123");
        validLoginDTO.setRememberMe(false);
        validLoginDTO.setClientId("test-client");
        validLoginDTO.setDeviceId("test-device");
        
        // Setup valid VerifyTokenRequestDTO
        validVerifyRequest = new VerifyTokenRequestDTO();
        validVerifyRequest.setUsername("testuser");
        validVerifyRequest.setOtp(123456);
        validVerifyRequest.setRememberMe(false);
        validVerifyRequest.setClientId("test-client");
        validVerifyRequest.setDeviceId("test-device");
    }

    @Test
    @DisplayName("Should successfully authenticate user without OTP")
    void testSuccessfulAuthenticationWithoutOtp() throws Exception {
        // Arrange
        JWTToken jwtToken = JWTToken.bearerToken("test-jwt-token", 3600L);
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
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.token.id_token", is("test-jwt-token")))
                .andExpect(jsonPath("$.token.token_type", is("Bearer")))
                .andExpect(jsonPath("$.token.expires_in", is(3600)))
                .andExpect(jsonPath("$.otp_required", is(false)))
                .andExpect(jsonPath("$.remember_me", is(false)))
                .andExpect(jsonPath("$.client_id", is("test-client")))
                .andExpect(jsonPath("$.device_id", is("test-device")));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenProvider).createToken(eq(authentication), eq(false));
    }

    @Test
    @DisplayName("Should successfully verify OTP and return JWT token")
    void testSuccessfulOtpVerification() throws Exception {
        // Arrange
        JWTToken jwtToken = JWTToken.bearerToken("verified-jwt-token", 3600L);
        
        when(otpService.validateOTP("testuser", 123456)).thenReturn(true);
        when(tokenProvider.createTokenAfterVerifiedOtp("testuser", false)).thenReturn(jwtToken);

        // Act & Assert
        mockMvc.perform(post("/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validVerifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.message", is("Authentication successful")))
                .andExpect(jsonPath("$.token.id_token", is("verified-jwt-token")))
                .andExpect(jsonPath("$.token.token_type", is("Bearer")))
                .andExpect(jsonPath("$.token.expires_in", is(3600)))
                .andExpect(jsonPath("$.otp_required", is(false)))
                .andExpect(jsonPath("$.remember_me", is(false)))
                .andExpect(jsonPath("$.client_id", is("test-client")))
                .andExpect(jsonPath("$.device_id", is("test-device")));

        verify(otpService).validateOTP("testuser", 123456);
        verify(tokenProvider).createTokenAfterVerifiedOtp("testuser", false);
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
                .andExpect(jsonPath("$.username", is("testuser")))
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
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.status", is("FAILED")))
                .andExpect(jsonPath("$.message", is("Invalid username or password")))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.otp_required", is(false)))
                .andExpect(jsonPath("$.remember_me", is(false)))
                .andExpect(jsonPath("$.client_id", is("test-client")))
                .andExpect(jsonPath("$.device_id", is("test-device")));

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
                .andExpect(jsonPath("$.username", is("testuser")))
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
        when(otpService.validateOTP("testuser", 123456)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validVerifyRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.status", is("FAILED")))
                .andExpect(jsonPath("$.message", is("Invalid OTP provided.")))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.otp_required", is(false)))
                .andExpect(jsonPath("$.remember_me", is(false)))
                .andExpect(jsonPath("$.client_id", is("test-client")))
                .andExpect(jsonPath("$.device_id", is("test-device")));

        verify(otpService).validateOTP("testuser", 123456);
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
            org.junit.jupiter.api.Assertions.fail("Expected ServletException to be thrown");
        } catch (Exception e) {
            // Verify that the root cause is the OtpRequiredException
            org.junit.jupiter.api.Assertions.assertTrue(e.getCause() instanceof OtpRequiredException);
            org.junit.jupiter.api.Assertions.assertEquals("OTP verification required", e.getCause().getMessage());
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
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.token.id_token", is("remember-me-token")))
                .andExpect(jsonPath("$.token.expires_in", is(86400)))
                .andExpect(jsonPath("$.remember_me", is(true)));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenProvider).createToken(eq(authentication), eq(true));
    }

    @Test
    @DisplayName("Should handle OTP verification with remember me flag")
    void testOtpVerificationWithRememberMe() throws Exception {
        // Arrange
        validVerifyRequest.setRememberMe(true);
        JWTToken jwtToken = JWTToken.bearerToken("remember-me-verified-token", 86400L);
        
        when(otpService.validateOTP("testuser", 123456)).thenReturn(true);
        when(tokenProvider.createTokenAfterVerifiedOtp("testuser", true)).thenReturn(jwtToken);

        // Act & Assert
        mockMvc.perform(post("/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validVerifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.token.id_token", is("remember-me-verified-token")))
                .andExpect(jsonPath("$.token.expires_in", is(86400)))
                .andExpect(jsonPath("$.remember_me", is(true)));

        verify(otpService).validateOTP("testuser", 123456);
        verify(tokenProvider).createTokenAfterVerifiedOtp("testuser", true);
    }

    @Test
    @DisplayName("Should handle rate limited authentication")
    void testRateLimitedAuthentication() throws Exception {
        // Arrange
        TokenCreationResponse tokenResponse = TokenCreationResponse.rejected("Too many authentication attempts");
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(tokenProvider.createToken(eq(authentication), eq(false)))
            .thenReturn(tokenResponse);

        // Act & Assert
        mockMvc.perform(post("/auth/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginDTO)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.status", is("RATE_LIMITED")))
                .andExpect(jsonPath("$.message", is("Too many authentication attempts")))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.otp_required", is(true)));

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenProvider).createToken(eq(authentication), eq(false));
    }

    @Test
    @DisplayName("Should handle null values in OTP verification")
    void testOtpVerificationWithNullValues() throws Exception {
        // Arrange
        validVerifyRequest.setRememberMe(null);
        validVerifyRequest.setClientId(null);
        validVerifyRequest.setDeviceId(null);
        JWTToken jwtToken = JWTToken.bearerToken("null-context-token", 3600L);
        
        when(otpService.validateOTP("testuser", 123456)).thenReturn(true);
        when(tokenProvider.createTokenAfterVerifiedOtp("testuser", null)).thenReturn(jwtToken);

        // Act & Assert
        mockMvc.perform(post("/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validVerifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.token.id_token", is("null-context-token")))
                .andExpect(jsonPath("$.remember_me").doesNotExist())
                .andExpect(jsonPath("$.client_id").doesNotExist())
                .andExpect(jsonPath("$.device_id").doesNotExist());

        verify(otpService).validateOTP("testuser", 123456);
        verify(tokenProvider).createTokenAfterVerifiedOtp("testuser", null);
    }
}