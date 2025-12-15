package com.starter.springboot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.starter.springboot.dto.OtpValidationResult;
import com.starter.springboot.dto.VerifyTokenRequestDTO;
import com.starter.springboot.security.jwt.ITokenProvider;
import com.starter.springboot.security.jwt.JWTToken;
import com.starter.springboot.service.IOtpService;
import com.starter.springboot.service.LocalizationService;
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

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("OtpVerificationController Tests")
class OtpVerificationControllerTest {

    @Mock
    private IOtpService otpService;

    @Mock
    private ITokenProvider tokenProvider;

    @Mock
    private LocalizationService localizationService;

    @InjectMocks
    private OtpVerificationController otpVerificationController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private VerifyTokenRequestDTO validVerifyRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(otpVerificationController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        // Setup valid VerifyTokenRequestDTO
        validVerifyRequest = new VerifyTokenRequestDTO();
        validVerifyRequest.setUsername("diona.smith");
        validVerifyRequest.setOtp(123456);
        validVerifyRequest.setRememberMe(false);
        validVerifyRequest.setClientId("web-app-client");
        validVerifyRequest.setDeviceId("mobile-device-001");

        mockLocalizationMessages();
    }

    private void mockLocalizationMessages() {
        lenient().when(localizationService.getMessage("auth.invalid_otp"))
                .thenReturn("Invalid OTP provided.");
        lenient().when(localizationService.getMessage("auth.locked_otp"))
                .thenReturn("Account locked");
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
    @DisplayName("Should return LOCKED status when account is locked")
    void testLockedAccountOtpVerification() throws Exception {
        // Arrange
        when(otpService.validateOTP("diona.smith", 123456)).thenReturn(OtpValidationResult.locked());

        // Act & Assert
        mockMvc.perform(post("/auth/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validVerifyRequest)))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.username", is("diona.smith")))
                .andExpect(jsonPath("$.status", is("FAILED")))
                .andExpect(jsonPath("$.message", is("Account locked")))
                .andExpect(jsonPath("$.token").doesNotExist());

        verify(otpService).validateOTP("diona.smith", 123456);
    }
}
