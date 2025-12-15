package com.starter.springboot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.starter.springboot.dto.RefreshTokenRequestDTO;
import com.starter.springboot.entity.RefreshToken;
import com.starter.springboot.entity.User;
import com.starter.springboot.exception.InvalidRefreshTokenException;
import com.starter.springboot.security.jwt.ITokenProvider;
import com.starter.springboot.security.jwt.JWTToken;
import com.starter.springboot.service.IRefreshTokenService;
import com.starter.springboot.service.IUserService;
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
@DisplayName("TokenRefreshController Tests")
class TokenRefreshControllerTest {

    @Mock
    private IRefreshTokenService refreshTokenService;

    @Mock
    private ITokenProvider tokenProvider;

    @Mock
    private IUserService userService;

    @Mock
    private LocalizationService localizationService;

    @InjectMocks
    private TokenRefreshController tokenRefreshController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private RefreshTokenRequestDTO validRefreshRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(tokenRefreshController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        validRefreshRequest = new RefreshTokenRequestDTO("valid-refresh-token");

        mockLocalizationMessages();
    }

    private void mockLocalizationMessages() {
        lenient().when(localizationService.getMessage("auth.invalid_refresh_token"))
                .thenReturn("Invalid refresh token");
    }

    @Test
    @DisplayName("Should successfully refresh token")
    void testSuccessfulTokenRefresh() throws Exception {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setUsername("diona.smith");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(1L);
        refreshToken.setToken("valid-refresh-token");

        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setUserId(1L);
        newRefreshToken.setToken("new-refresh-token");

        JWTToken accessToken = JWTToken.bearerToken("new-access-token", 3600L);

        when(refreshTokenService.validateRefreshToken("valid-refresh-token")).thenReturn(refreshToken);
        when(userService.findUserById(1L)).thenReturn(user);
        when(tokenProvider.getRefreshTokenValidityInSeconds()).thenReturn(604800L);
        when(refreshTokenService.rotateRefreshToken("valid-refresh-token", 1L, 604800L))
                .thenReturn(newRefreshToken);
        when(tokenProvider.createAccessTokenAfterVerifiedOtp("diona.smith", false))
                .thenReturn(accessToken);

        // Act & Assert
        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRefreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("diona.smith")))
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.token.id_token", is("new-access-token")))
                .andExpect(jsonPath("$.token.refresh_token", is("new-refresh-token")))
                .andExpect(jsonPath("$.token.token_type", is("Bearer")))
                .andExpect(jsonPath("$.token.expires_in", is(3600)))
                .andExpect(jsonPath("$.token.refresh_token_expires_in", is(604800)));

        verify(refreshTokenService).validateRefreshToken("valid-refresh-token");
        verify(userService).findUserById(1L);
        verify(refreshTokenService).rotateRefreshToken("valid-refresh-token", 1L, 604800L);
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED for invalid refresh token")
    void testRefreshTokenWithInvalidToken() throws Exception {
        // Arrange
        when(refreshTokenService.validateRefreshToken("valid-refresh-token"))
                .thenThrow(new InvalidRefreshTokenException("Invalid refresh token"));

        // Act & Assert
        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRefreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is("FAILED")))
                .andExpect(jsonPath("$.message", is("Invalid refresh token")));

        verify(refreshTokenService).validateRefreshToken("valid-refresh-token");
    }
}
