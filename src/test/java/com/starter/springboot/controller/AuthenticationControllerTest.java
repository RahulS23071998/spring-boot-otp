package com.starter.springboot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.starter.springboot.dto.LoginDTO;
import com.starter.springboot.exception.OtpRequiredException;
import com.starter.springboot.security.jwt.ITokenProvider;
import com.starter.springboot.security.jwt.JWTToken;
import com.starter.springboot.security.jwt.TokenCreationResponse;
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
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationController Tests")
class AuthenticationControllerTest {

    @Mock
    private ITokenProvider tokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private LocalizationService localizationService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthenticationController authenticationController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private LoginDTO validLoginDTO;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(authenticationController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
        
        // Setup valid LoginDTO
        validLoginDTO = new LoginDTO();
        validLoginDTO.setUsername("diona.smith");
        validLoginDTO.setPassword("SecurePass123");
        validLoginDTO.setRememberMe(false);
        validLoginDTO.setClientId("web-app-client");
        validLoginDTO.setDeviceId("mobile-device-001");
        
        mockLocalizationMessages();
    }

    private void mockLocalizationMessages() {
        lenient().when(localizationService.getMessage("auth.invalid_credentials"))
                .thenReturn("Invalid credentials");
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
                .andExpect(jsonPath("$.remember_me", is(true)));

        verify(tokenProvider).createToken(eq(authentication), eq(true));
    }
}
