package com.starter.springboot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.starter.springboot.entity.RefreshToken;
import com.starter.springboot.entity.User;
import com.starter.springboot.security.DomainUserDetails;
import com.starter.springboot.service.ILogoutService;
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
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("LogoutController Tests")
class LogoutControllerTest {

    @Mock
    private ILogoutService logoutService;

    @Mock
    private LocalizationService localizationService;

    @Mock
    private IUserService userService;

    @Mock
    private IRefreshTokenService refreshTokenService;

    @InjectMocks
    private LogoutController logoutController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        mockMvc = MockMvcBuilders.standaloneSetup(logoutController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should successfully logout authenticated user")
    void shouldSuccessfullyLogoutAuthenticatedUser() throws Exception {
        // Mock authentication
        DomainUserDetails userDetails = mock(DomainUserDetails.class);
        when(userDetails.getUsername()).thenReturn("testuser");
        when(userDetails.getUserId()).thenReturn(1L);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, "password", java.util.Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(localizationService.getMessage("auth.logout_successful")).thenReturn("Logout successful");

        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Logout successful"));

        verify(logoutService).logout(1L, "testuser", null);
    }

    @Test
    @DisplayName("Should fail logout when not authenticated")
    void shouldFailLogoutWhenNotAuthenticated() throws Exception {
        when(localizationService.getMessage("auth.not_authenticated")).thenReturn("User not authenticated");

        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.message").value("User not authenticated"));

        verifyNoInteractions(logoutService);
    }

    @Test
    @DisplayName("Should successfully logout with refresh token")
    void shouldSuccessfullyLogoutWithRefreshToken() throws Exception {
        String refreshToken = "valid-refresh-token";
        RefreshToken rt = new RefreshToken();
        rt.setUserId(1L);
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        when(refreshTokenService.validateRefreshToken(refreshToken)).thenReturn(rt);
        when(userService.findUserById(1L)).thenReturn(user);
        when(localizationService.getMessage("auth.logout_successful")).thenReturn("Logout successful");

        mockMvc.perform(post("/auth/logout")
                .param("refreshToken", refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(logoutService).logout(1L, "testuser", refreshToken);
    }

    @Test
    @DisplayName("Should fail logout with invalid refresh token")
    void shouldFailLogoutWithInvalidRefreshToken() throws Exception {
        String refreshToken = "invalid-token";
        when(refreshTokenService.validateRefreshToken(refreshToken)).thenThrow(new RuntimeException("Invalid token"));
        when(localizationService.getMessage("auth.invalid_refresh_token")).thenReturn("Invalid refresh token");

        mockMvc.perform(post("/auth/logout")
                .param("refreshToken", refreshToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid refresh token"));

        verifyNoInteractions(logoutService);
    }

    @Test
    @DisplayName("Should successfully logout all sessions")
    void shouldSuccessfullyLogoutAllSessions() throws Exception {
        // Mock authentication
        DomainUserDetails userDetails = mock(DomainUserDetails.class);
        when(userDetails.getUsername()).thenReturn("testuser");
        when(userDetails.getUserId()).thenReturn(1L);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, "password", java.util.Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(localizationService.getMessage("auth.logout_all_sessions_successful")).thenReturn("All sessions logged out");

        mockMvc.perform(post("/auth/logout-all-sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("All sessions logged out"));

        verify(logoutService).revokeAllSessions(1L, "testuser");
    }

    @Test
    @DisplayName("Should fail logout all sessions when not authenticated")
    void shouldFailLogoutAllSessionsWhenNotAuthenticated() throws Exception {
        when(localizationService.getMessage("auth.not_authenticated")).thenReturn("User not authenticated");

        mockMvc.perform(post("/auth/logout-all-sessions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("FAILED"));

        verifyNoInteractions(logoutService);
    }

    @Test
    @DisplayName("Should handle internal server error during logout")
    void shouldHandleInternalServerErrorDuringLogout() throws Exception {
        // Mock authentication
        DomainUserDetails userDetails = mock(DomainUserDetails.class);
        when(userDetails.getUsername()).thenReturn("testuser");
        when(userDetails.getUserId()).thenReturn(1L);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, "password", java.util.Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        doThrow(new RuntimeException("Internal error")).when(logoutService).logout(anyLong(), anyString(), any());
        when(localizationService.getMessage("auth.logout_error")).thenReturn("Logout error");

        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Logout error"));
    }
}
