package com.starter.springboot.repository;

import com.starter.springboot.entity.RefreshToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.sql.init.mode=never",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false"
})
@DisplayName("RefreshTokenRepository Tests")
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private TestEntityManager entityManager;

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_USER_ID_2 = 2L;

    private RefreshToken activeToken;
    private RefreshToken expiredToken;
    private RefreshToken revokedToken;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        refreshTokenRepository.deleteAll();

        // Create test tokens
        activeToken = createRefreshToken(TEST_USER_ID, "active-token", Instant.now().plusSeconds(3600), null);
        expiredToken = createRefreshToken(TEST_USER_ID, "expired-token", Instant.now().minusSeconds(3600), null);
        revokedToken = createRefreshToken(TEST_USER_ID, "revoked-token", Instant.now().plusSeconds(3600), Instant.now());

        // Save tokens
        entityManager.persist(activeToken);
        entityManager.persist(expiredToken);
        entityManager.persist(revokedToken);
        entityManager.flush();
    }

    private RefreshToken createRefreshToken(Long userId, String token, Instant expiresAt, Instant revokedAt) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setToken(token);
        refreshToken.setExpiresAt(expiresAt);
        refreshToken.setRevokedAt(revokedAt);
        refreshToken.setCreatedAt(Instant.now());
        return refreshToken;
    }

    @Test
    @DisplayName("Should find active refresh token by token value")
    void shouldFindActiveRefreshTokenByTokenValue() {
        Optional<RefreshToken> found = refreshTokenRepository.findByTokenAndRevokedAtIsNullAndExpiresAtAfter(
                activeToken.getToken(), Instant.now());

        assertTrue(found.isPresent());
        assertEquals(activeToken.getToken(), found.get().getToken());
        assertEquals(TEST_USER_ID, found.get().getUserId());
        assertNull(found.get().getRevokedAt());
        assertTrue(found.get().getExpiresAt().isAfter(Instant.now()));
    }

    @Test
    @DisplayName("Should not find expired refresh token")
    void shouldNotFindExpiredRefreshToken() {
        Optional<RefreshToken> found = refreshTokenRepository.findByTokenAndRevokedAtIsNullAndExpiresAtAfter(
                expiredToken.getToken(), Instant.now());

        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Should not find revoked refresh token")
    void shouldNotFindRevokedRefreshToken() {
        Optional<RefreshToken> found = refreshTokenRepository.findByTokenAndRevokedAtIsNullAndExpiresAtAfter(
                revokedToken.getToken(), Instant.now());

        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Should not find non-existent token")
    void shouldNotFindNonExistentToken() {
        Optional<RefreshToken> found = refreshTokenRepository.findByTokenAndRevokedAtIsNullAndExpiresAtAfter(
                "non-existent-token", Instant.now());

        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Should revoke all active tokens for user")
    void shouldRevokeAllActiveTokensForUser() {
        // Create another active token for the same user
        RefreshToken anotherActiveToken = createRefreshToken(TEST_USER_ID, "another-active-token",
                Instant.now().plusSeconds(7200), null);
        entityManager.persist(anotherActiveToken);
        entityManager.flush();

        Instant revokeTime = Instant.now();
        int revokedCount = refreshTokenRepository.revokeAllByUserId(TEST_USER_ID, revokeTime, Instant.now());

        assertEquals(2, revokedCount);

        // Verify tokens are revoked by checking their revokedAt field
        entityManager.clear();
        Optional<RefreshToken> token1 = refreshTokenRepository.findById(activeToken.getId());
        Optional<RefreshToken> token2 = refreshTokenRepository.findById(anotherActiveToken.getId());
        
        assertTrue(token1.isPresent());
        assertTrue(token2.isPresent());
        assertNotNull(token1.get().getRevokedAt());
        assertNotNull(token2.get().getRevokedAt());
    }

    @Test
    @DisplayName("Should not revoke expired tokens when revoking all active tokens")
    void shouldNotRevokeExpiredTokensWhenRevokingAllActiveTokens() {
        Instant revokeTime = Instant.now();
        int revokedCount = refreshTokenRepository.revokeAllByUserId(TEST_USER_ID, revokeTime, Instant.now());

        assertEquals(1, revokedCount); // Only the active token should be revoked

        // Verify expired token is still not active (but wasn't revoked by the method)
        entityManager.clear();
        Optional<RefreshToken> expiredTokenFromDb = refreshTokenRepository.findById(expiredToken.getId());
        assertTrue(expiredTokenFromDb.isPresent());
        assertNull(expiredTokenFromDb.get().getRevokedAt()); // Should still be null
    }

    @Test
    @DisplayName("Should not revoke already revoked tokens when revoking all active tokens")
    void shouldNotRevokeAlreadyRevokedTokensWhenRevokingAllActiveTokens() {
        Instant revokeTime = Instant.now();
        int revokedCount = refreshTokenRepository.revokeAllByUserId(TEST_USER_ID, revokeTime, Instant.now());

        assertEquals(1, revokedCount); // Only the active token should be revoked

        // Verify revoked token remains revoked
        entityManager.clear();
        Optional<RefreshToken> revokedTokenFromDb = refreshTokenRepository.findById(revokedToken.getId());
        assertTrue(revokedTokenFromDb.isPresent());
        assertNotNull(revokedTokenFromDb.get().getRevokedAt());
    }

    @Test
    @DisplayName("Should return zero when revoking tokens for user with no active tokens")
    void shouldReturnZeroWhenRevokingTokensForUserWithNoActiveTokens() {
        Instant revokeTime = Instant.now();
        int revokedCount = refreshTokenRepository.revokeAllByUserId(TEST_USER_ID_2, revokeTime, Instant.now());

        assertEquals(0, revokedCount);
    }

    @Test
    @DisplayName("Should delete expired tokens")
    void shouldDeleteExpiredTokens() {
        int deletedCount = refreshTokenRepository.deleteExpiredTokens(Instant.now());

        assertEquals(1, deletedCount);

        // Flush changes and clear persistence context to ensure delete is visible
        entityManager.flush();
        entityManager.clear();

        // Verify expired token is deleted
        Optional<RefreshToken> deletedToken = refreshTokenRepository.findById(expiredToken.getId());
        assertFalse(deletedToken.isPresent());
    }

    @Test
    @DisplayName("Should not delete active tokens when deleting expired tokens")
    void shouldNotDeleteActiveTokensWhenDeletingExpiredTokens() {
        int deletedCount = refreshTokenRepository.deleteExpiredTokens(Instant.now());

        assertEquals(1, deletedCount);

        // Verify active token still exists
        Optional<RefreshToken> activeTokenFromDb = refreshTokenRepository.findById(activeToken.getId());
        assertTrue(activeTokenFromDb.isPresent());
    }

    @Test
    @DisplayName("Should not delete revoked but not expired tokens when deleting expired tokens")
    void shouldNotDeleteRevokedButNotExpiredTokensWhenDeletingExpiredTokens() {
        int deletedCount = refreshTokenRepository.deleteExpiredTokens(Instant.now());

        assertEquals(1, deletedCount);

        // Verify revoked token still exists
        Optional<RefreshToken> revokedTokenFromDb = refreshTokenRepository.findById(revokedToken.getId());
        assertTrue(revokedTokenFromDb.isPresent());
    }

    @Test
    @DisplayName("Should return zero when no expired tokens to delete")
    void shouldReturnZeroWhenNoExpiredTokensToDelete() {
        // Delete the expired token first
        refreshTokenRepository.deleteExpiredTokens(Instant.now());

        // Try to delete again
        int deletedCount = refreshTokenRepository.deleteExpiredTokens(Instant.now());

        assertEquals(0, deletedCount);
    }
}