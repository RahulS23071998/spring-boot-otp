package com.starter.springboot.service;

import com.starter.springboot.service.impl.LogoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LogoutService Tests")
class LogoutServiceTest {

    @Mock
    private IRefreshTokenService refreshTokenService;

    @Mock
    private IRedisTokenService redisTokenService;

    @Mock
    private IAuthCacheService authCacheService;

    @InjectMocks
    private LogoutService logoutService;

    private static final Long USER_ID = 1L;
    private static final String USERNAME = "testuser";
    private static final String REFRESH_TOKEN = "test-refresh-token";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should successfully logout with refresh token")
    void shouldSuccessfullyLogoutWithRefreshToken() {
        logoutService.logout(USER_ID, USERNAME, REFRESH_TOKEN);

        verify(refreshTokenService).revokeRefreshToken(REFRESH_TOKEN);
        verify(redisTokenService).removeWhitelist(USER_ID);
        verify(authCacheService).clearAllCachesForUser(USERNAME, USER_ID);
    }

    @Test
    @DisplayName("Should successfully logout without refresh token")
    void shouldSuccessfullyLogoutWithoutRefreshToken() {
        logoutService.logout(USER_ID, USERNAME);

        verify(refreshTokenService, never()).revokeRefreshToken(anyString());
        verify(redisTokenService).removeWhitelist(USER_ID);
        verify(authCacheService).clearAllCachesForUser(USERNAME, USER_ID);
    }

    @Test
    @DisplayName("Should successfully revoke all sessions")
    void shouldSuccessfullyRevokeAllSessions() {
        logoutService.revokeAllSessions(USER_ID, USERNAME);

        verify(refreshTokenService).revokeAllUserRefreshTokens(USER_ID);
        verify(redisTokenService).removeWhitelist(USER_ID);
        verify(authCacheService).clearAllCachesForUser(USERNAME, USER_ID);
    }

    @Test
    @DisplayName("Should handle null inputs gracefully for logout")
    void shouldHandleNullInputsGracefullyForLogout() {
        logoutService.logout(null, null, null);

        verifyNoInteractions(refreshTokenService);
        verifyNoInteractions(redisTokenService);
        verifyNoInteractions(authCacheService);
    }

    @Test
    @DisplayName("Should handle null inputs gracefully for revoke all sessions")
    void shouldHandleNullInputsGracefullyForRevokeAllSessions() {
        logoutService.revokeAllSessions(null, null);

        verifyNoInteractions(refreshTokenService);
        verifyNoInteractions(redisTokenService);
        verifyNoInteractions(authCacheService);
    }

    @Test
    @DisplayName("Should handle exceptions during logout components")
    void shouldHandleExceptionsDuringLogoutComponents() {
        doThrow(new RuntimeException("DB Error")).when(refreshTokenService).revokeRefreshToken(anyString());
        doThrow(new RuntimeException("Redis Error")).when(redisTokenService).removeWhitelist(anyLong());
        doThrow(new RuntimeException("Cache Error")).when(authCacheService).clearAllCachesForUser(anyString(), anyLong());

        logoutService.logout(USER_ID, USERNAME, REFRESH_TOKEN);

        verify(refreshTokenService).revokeRefreshToken(REFRESH_TOKEN);
        verify(redisTokenService).removeWhitelist(USER_ID);
        verify(authCacheService).clearAllCachesForUser(USERNAME, USER_ID);
    }
}
