package com.starter.springboot.service.impl;

import com.starter.springboot.service.IAuthCacheService;
import com.starter.springboot.service.IRefreshTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserTokenServiceTest {

    @Mock
    private IAuthCacheService authCacheService;

    @Mock
    private IRefreshTokenService refreshTokenService;

    @InjectMocks
    private UserTokenService userTokenService;

    private static final String TEST_USERNAME = "testuser";
    private static final Long TEST_USER_ID = 1L;

    @Test
    @DisplayName("Should successfully clear all user tokens and caches")
    void shouldSuccessfullyClearAllUserTokensAndCaches() {
        // Given
        doNothing().when(authCacheService).clearAllCachesForUser(TEST_USERNAME, TEST_USER_ID);
        doNothing().when(refreshTokenService).revokeAllUserRefreshTokens(TEST_USER_ID);

        // When
        userTokenService.clearAllUserTokensAndCaches(TEST_USERNAME, TEST_USER_ID);

        // Then
        verify(authCacheService).clearAllCachesForUser(TEST_USERNAME, TEST_USER_ID);
        verify(refreshTokenService).revokeAllUserRefreshTokens(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should propagate exception when clearing caches fails")
    void shouldPropagateExceptionWhenClearingCachesFails() {
        // Given
        doThrow(new RuntimeException("Cache clear failed")).when(authCacheService).clearAllCachesForUser(TEST_USERNAME, TEST_USER_ID);

        // When & Then
        assertThrows(RuntimeException.class, () -> userTokenService.clearAllUserTokensAndCaches(TEST_USERNAME, TEST_USER_ID));
        verify(authCacheService).clearAllCachesForUser(TEST_USERNAME, TEST_USER_ID);
        verify(refreshTokenService, never()).revokeAllUserRefreshTokens(anyLong());
    }

    @Test
    @DisplayName("Should propagate exception when revoking tokens fails")
    void shouldPropagateExceptionWhenRevokingTokensFails() {
        // Given
        doNothing().when(authCacheService).clearAllCachesForUser(TEST_USERNAME, TEST_USER_ID);
        doThrow(new RuntimeException("Token revoke failed")).when(refreshTokenService).revokeAllUserRefreshTokens(TEST_USER_ID);

        // When & Then
        assertThrows(RuntimeException.class, () -> userTokenService.clearAllUserTokensAndCaches(TEST_USERNAME, TEST_USER_ID));
        verify(authCacheService).clearAllCachesForUser(TEST_USERNAME, TEST_USER_ID);
        verify(refreshTokenService).revokeAllUserRefreshTokens(TEST_USER_ID);
    }
}
