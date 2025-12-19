package com.starter.springboot.service.impl;

import com.starter.springboot.service.IRefreshTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenCleanupServiceTest {

    @Mock
    private IRefreshTokenService refreshTokenService;

    @InjectMocks
    private RefreshTokenCleanupService refreshTokenCleanupService;

    @Test
    @DisplayName("Should successfully cleanup expired refresh tokens")
    void shouldSuccessfullyCleanupExpiredRefreshTokens() {
        // Given
        doNothing().when(refreshTokenService).cleanupExpiredTokens();

        // When
        refreshTokenCleanupService.cleanupExpiredRefreshTokens();

        // Then
        verify(refreshTokenService).cleanupExpiredTokens();
    }

    @Test
    @DisplayName("Should handle exception during cleanup")
    void shouldHandleExceptionDuringCleanup() {
        // Given
        doThrow(new RuntimeException("Cleanup failed")).when(refreshTokenService).cleanupExpiredTokens();

        // When
        refreshTokenCleanupService.cleanupExpiredRefreshTokens();

        // Then
        verify(refreshTokenService).cleanupExpiredTokens();
        // Exception is caught and logged, so no exception is thrown
    }
}
