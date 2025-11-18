package com.starter.springboot.service;

import com.starter.springboot.entity.RefreshToken;
import com.starter.springboot.repository.RefreshTokenRepository;
import com.starter.springboot.service.impl.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService Tests")
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private static final Long TEST_USER_ID = 1L;
    private static final long EXPIRATION_SECONDS = 604800L;

    private RefreshToken testRefreshToken;

    @BeforeEach
    void setUp() {
        testRefreshToken = createTestRefreshToken();
    }

    private RefreshToken createTestRefreshToken() {
        RefreshToken token = new RefreshToken();
        token.setId(1L);
        token.setUserId(TEST_USER_ID);
        token.setToken("test-refresh-token-uuid-1234567890");
        token.setExpiresAt(Instant.now().plusSeconds(EXPIRATION_SECONDS));
        token.setCreatedAt(Instant.now());
        token.setRevokedAt(null);
        return token;
    }

    @Test
    @DisplayName("Should successfully create refresh token")
    void shouldSuccessfullyCreateRefreshToken() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);

        RefreshToken createdToken = refreshTokenService.createRefreshToken(TEST_USER_ID, EXPIRATION_SECONDS);

        assertNotNull(createdToken);
        assertEquals(TEST_USER_ID, createdToken.getUserId());
        assertEquals("test-refresh-token-uuid-1234567890", createdToken.getToken());
        assertNotNull(createdToken.getExpiresAt());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should generate unique token on creation")
    void shouldGenerateUniqueTokenOnCreation() {
        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            token.setId(1L);
            return token;
        });

        RefreshToken token1 = refreshTokenService.createRefreshToken(TEST_USER_ID, EXPIRATION_SECONDS);
        RefreshToken token2 = refreshTokenService.createRefreshToken(TEST_USER_ID, EXPIRATION_SECONDS);

        assertNotNull(token1.getToken());
        assertNotNull(token2.getToken());
        verify(refreshTokenRepository, times(2)).save(tokenCaptor.capture());
    }

    @Test
    @DisplayName("Should set correct expiration time on token creation")
    void shouldSetCorrectExpirationTimeOnTokenCreation() {
        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        Instant beforeCreation = Instant.now();
        
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            token.setId(1L);
            return token;
        });

        RefreshToken createdToken = refreshTokenService.createRefreshToken(TEST_USER_ID, EXPIRATION_SECONDS);
        Instant afterCreation = Instant.now();

        verify(refreshTokenRepository).save(tokenCaptor.capture());
        RefreshToken savedToken = tokenCaptor.getValue();
        
        assertTrue(savedToken.getExpiresAt().isAfter(beforeCreation.plusSeconds(EXPIRATION_SECONDS - 10)));
        assertTrue(savedToken.getExpiresAt().isBefore(afterCreation.plusSeconds(EXPIRATION_SECONDS + 10)));
    }

    @Test
    @DisplayName("Should successfully validate active refresh token")
    void shouldSuccessfullyValidateActiveRefreshToken() {
        String tokenValue = "test-refresh-token-uuid-1234567890";
        testRefreshToken.setRevokedAt(null);
        testRefreshToken.setExpiresAt(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByTokenAndRevokedAtIsNullAndExpiresAtAfter(
                eq(tokenValue), any(Instant.class)))
                .thenReturn(Optional.of(testRefreshToken));

        RefreshToken validatedToken = refreshTokenService.validateRefreshToken(tokenValue);

        assertNotNull(validatedToken);
        assertEquals(tokenValue, validatedToken.getToken());
        assertEquals(TEST_USER_ID, validatedToken.getUserId());
        verify(refreshTokenRepository).findByTokenAndRevokedAtIsNullAndExpiresAtAfter(
                eq(tokenValue), any(Instant.class));
    }

    @Test
    @DisplayName("Should throw exception for invalid refresh token")
    void shouldThrowExceptionForInvalidRefreshToken() {
        String invalidToken = "invalid-token";
        
        when(refreshTokenRepository.findByTokenAndRevokedAtIsNullAndExpiresAtAfter(
                eq(invalidToken), any(Instant.class)))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> refreshTokenService.validateRefreshToken(invalidToken));

        assertEquals("Invalid refresh token", exception.getMessage());
        verify(refreshTokenRepository).findByTokenAndRevokedAtIsNullAndExpiresAtAfter(
                eq(invalidToken), any(Instant.class));
    }

    @Test
    @DisplayName("Should throw exception for revoked refresh token")
    void shouldThrowExceptionForRevokedRefreshToken() {
        String tokenValue = "test-refresh-token";
        testRefreshToken.setRevokedAt(Instant.now());

        when(refreshTokenRepository.findByTokenAndRevokedAtIsNullAndExpiresAtAfter(
                eq(tokenValue), any(Instant.class)))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> refreshTokenService.validateRefreshToken(tokenValue));

        assertEquals("Invalid refresh token", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception for expired refresh token")
    void shouldThrowExceptionForExpiredRefreshToken() {
        String tokenValue = "expired-token";
        Instant pastTime = Instant.now().minusSeconds(3600);

        when(refreshTokenRepository.findByTokenAndRevokedAtIsNullAndExpiresAtAfter(
                eq(tokenValue), any(Instant.class)))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> refreshTokenService.validateRefreshToken(tokenValue));

        assertEquals("Invalid refresh token", exception.getMessage());
    }

    @Test
    @DisplayName("Should successfully revoke refresh token")
    void shouldSuccessfullyRevokeRefreshToken() {
        String tokenValue = "test-refresh-token-uuid-1234567890";
        testRefreshToken.setRevokedAt(null);
        testRefreshToken.setExpiresAt(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByTokenAndRevokedAtIsNullAndExpiresAtAfter(
                eq(tokenValue), any(Instant.class)))
                .thenReturn(Optional.of(testRefreshToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);

        refreshTokenService.revokeRefreshToken(tokenValue);

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        RefreshToken savedToken = tokenCaptor.getValue();
        
        assertNotNull(savedToken.getRevokedAt());
        assertTrue(savedToken.getRevokedAt().isBefore(Instant.now().plusSeconds(10)));
    }

    @Test
    @DisplayName("Should not fail when revoking non-existent token")
    void shouldNotFailWhenRevokingNonExistentToken() {
        String tokenValue = "non-existent-token";

        when(refreshTokenRepository.findByTokenAndRevokedAtIsNullAndExpiresAtAfter(
                eq(tokenValue), any(Instant.class)))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> refreshTokenService.revokeRefreshToken(tokenValue));
        verify(refreshTokenRepository).findByTokenAndRevokedAtIsNullAndExpiresAtAfter(
                eq(tokenValue), any(Instant.class));
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should successfully revoke all user refresh tokens")
    void shouldSuccessfullyRevokeAllUserRefreshTokens() {
        int revokedCount = 5;
        when(refreshTokenRepository.revokeAllByUserId(
                eq(TEST_USER_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(revokedCount);

        refreshTokenService.revokeAllUserRefreshTokens(TEST_USER_ID);

        verify(refreshTokenRepository).revokeAllByUserId(
                eq(TEST_USER_ID), any(Instant.class), any(Instant.class));
    }

    @Test
    @DisplayName("Should handle revoking all tokens when user has no tokens")
    void shouldHandleRevokingAllTokensWhenUserHasNoTokens() {
        when(refreshTokenRepository.revokeAllByUserId(
                eq(TEST_USER_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(0);

        refreshTokenService.revokeAllUserRefreshTokens(TEST_USER_ID);

        verify(refreshTokenRepository).revokeAllByUserId(
                eq(TEST_USER_ID), any(Instant.class), any(Instant.class));
    }

    @Test
    @DisplayName("Should successfully rotate refresh token")
    void shouldSuccessfullyRotateRefreshToken() {
        String oldToken = "old-refresh-token";
        String newToken = "new-refresh-token";
        
        testRefreshToken.setToken(oldToken);
        testRefreshToken.setRevokedAt(null);
        testRefreshToken.setExpiresAt(Instant.now().plusSeconds(3600));

        RefreshToken newRefreshToken = createTestRefreshToken();
        newRefreshToken.setToken(newToken);
        newRefreshToken.setId(2L);

        when(refreshTokenRepository.findByTokenAndRevokedAtIsNullAndExpiresAtAfter(
                eq(oldToken), any(Instant.class)))
                .thenReturn(Optional.of(testRefreshToken));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenReturn(testRefreshToken)
                .thenReturn(newRefreshToken);

        RefreshToken rotatedToken = refreshTokenService.rotateRefreshToken(
                oldToken, TEST_USER_ID, EXPIRATION_SECONDS);

        assertNotNull(rotatedToken);
        assertEquals(newToken, rotatedToken.getToken());
        
        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(2)).save(tokenCaptor.capture());
    }

    @Test
    @DisplayName("Should revoke old token during rotation")
    void shouldRevokeOldTokenDuringRotation() {
        String oldToken = "old-refresh-token";
        String newToken = "new-refresh-token";
        
        testRefreshToken.setToken(oldToken);
        testRefreshToken.setRevokedAt(null);
        testRefreshToken.setExpiresAt(Instant.now().plusSeconds(3600));

        RefreshToken newRefreshToken = createTestRefreshToken();
        newRefreshToken.setToken(newToken);

        when(refreshTokenRepository.findByTokenAndRevokedAtIsNullAndExpiresAtAfter(
                eq(oldToken), any(Instant.class)))
                .thenReturn(Optional.of(testRefreshToken));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenReturn(testRefreshToken)
                .thenReturn(newRefreshToken);

        refreshTokenService.rotateRefreshToken(oldToken, TEST_USER_ID, EXPIRATION_SECONDS);

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(2)).save(tokenCaptor.capture());
        
        RefreshToken revokedToken = tokenCaptor.getAllValues().get(0);
        assertNotNull(revokedToken.getRevokedAt());
    }

    @Test
    @DisplayName("Should create new token when rotating even if old token is invalid")
    void shouldCreateNewTokenWhenRotatingInvalidToken() {
        String invalidToken = "invalid-token";
        String newToken = "new-token";
        
        RefreshToken newRefreshToken = createTestRefreshToken();
        newRefreshToken.setToken(newToken);

        when(refreshTokenRepository.findByTokenAndRevokedAtIsNullAndExpiresAtAfter(
                eq(invalidToken), any(Instant.class)))
                .thenReturn(Optional.empty());
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenReturn(newRefreshToken);

        RefreshToken rotatedToken = refreshTokenService.rotateRefreshToken(invalidToken, TEST_USER_ID, EXPIRATION_SECONDS);

        assertNotNull(rotatedToken);
        assertEquals(newToken, rotatedToken.getToken());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Should successfully cleanup expired tokens")
    void shouldSuccessfullyCleanupExpiredTokens() {
        int deletedCount = 10;
        when(refreshTokenRepository.deleteExpiredTokens(any(Instant.class))).thenReturn(deletedCount);

        refreshTokenService.cleanupExpiredTokens();

        verify(refreshTokenRepository).deleteExpiredTokens(any(Instant.class));
    }

    @Test
    @DisplayName("Should handle cleanup when no expired tokens exist")
    void shouldHandleCleanupWhenNoExpiredTokensExist() {
        when(refreshTokenRepository.deleteExpiredTokens(any(Instant.class))).thenReturn(0);

        refreshTokenService.cleanupExpiredTokens();

        verify(refreshTokenRepository).deleteExpiredTokens(any(Instant.class));
    }

    @Test
    @DisplayName("Should create refresh token with correct user ID")
    void shouldCreateRefreshTokenWithCorrectUserId() {
        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);

        refreshTokenService.createRefreshToken(TEST_USER_ID, EXPIRATION_SECONDS);

        verify(refreshTokenRepository).save(tokenCaptor.capture());
        RefreshToken savedToken = tokenCaptor.getValue();
        assertEquals(TEST_USER_ID, savedToken.getUserId());
    }

    @Test
    @DisplayName("Should validate only non-revoked and non-expired tokens")
    void shouldValidateOnlyNonRevokedAndNonExpiredTokens() {
        String tokenValue = "valid-token";

        when(refreshTokenRepository.findByTokenAndRevokedAtIsNullAndExpiresAtAfter(
                eq(tokenValue), any(Instant.class)))
                .thenReturn(Optional.of(testRefreshToken));

        RefreshToken result = refreshTokenService.validateRefreshToken(tokenValue);

        assertNotNull(result);
        assertNull(result.getRevokedAt());
        assertTrue(result.getExpiresAt().isAfter(Instant.now()));
    }

    @Test
    @DisplayName("Should save revoked token with timestamp")
    void shouldSaveRevokedTokenWithTimestamp() {
        String tokenValue = "token-to-revoke";
        testRefreshToken.setToken(tokenValue);
        testRefreshToken.setRevokedAt(null);
        testRefreshToken.setExpiresAt(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByTokenAndRevokedAtIsNullAndExpiresAtAfter(
                eq(tokenValue), any(Instant.class)))
                .thenReturn(Optional.of(testRefreshToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);

        Instant beforeRevoke = Instant.now();
        refreshTokenService.revokeRefreshToken(tokenValue);
        Instant afterRevoke = Instant.now();

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        RefreshToken revokedToken = tokenCaptor.getValue();

        assertNotNull(revokedToken.getRevokedAt());
        assertTrue(revokedToken.getRevokedAt().isAfter(beforeRevoke.minusSeconds(5)));
        assertTrue(revokedToken.getRevokedAt().isBefore(afterRevoke.plusSeconds(5)));
    }
}
