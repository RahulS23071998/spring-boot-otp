package com.starter.springboot.service.impl;

import com.starter.springboot.entity.RefreshToken;
import com.starter.springboot.repository.RefreshTokenRepository;
import com.starter.springboot.service.IRefreshTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service implementation for managing refresh tokens.
 */
@Service
public class RefreshTokenService implements IRefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    @Transactional
    public RefreshToken createRefreshToken(Long userId, long expirationSeconds) {
        // Generate a secure random token
        String token = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusSeconds(expirationSeconds);

        RefreshToken refreshToken = new RefreshToken(userId, token, expiresAt);
        RefreshToken savedToken = refreshTokenRepository.save(refreshToken);

        log.debug("Created refresh token for user: {}", userId);
        return savedToken;
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String token) {
        Instant now = Instant.now();
        return refreshTokenRepository.findByTokenAndRevokedAtIsNullAndExpiresAtAfter(token, now)
                .orElseThrow(() -> {
                    log.warn("Invalid or expired refresh token");
                    return new RuntimeException("Invalid refresh token");
                });
    }

    @Override
    @Transactional
    public void revokeRefreshToken(String token) {
        Instant now = Instant.now();
        refreshTokenRepository.findByTokenAndRevokedAtIsNullAndExpiresAtAfter(token, now)
                .ifPresent(refreshToken -> {
                    refreshToken.setRevokedAt(now);
                    refreshTokenRepository.save(refreshToken);
                    log.debug("Revoked refresh token for user: {}", refreshToken.getUserId());
                });
    }

    @Override
    @Transactional
    public void revokeAllUserRefreshTokens(Long userId) {
        Instant now = Instant.now();
        int revokedCount = refreshTokenRepository.revokeAllByUserId(userId, now, now);
        log.debug("Revoked {} refresh tokens for user: {}", revokedCount, userId);
    }

    @Override
    @Transactional
    public RefreshToken rotateRefreshToken(String oldToken, Long userId, long expirationSeconds) {
        // First revoke the old token
        revokeRefreshToken(oldToken);

        // Create new token
        return createRefreshToken(userId, expirationSeconds);
    }

    @Override
    @Transactional
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();
        int deletedCount = refreshTokenRepository.deleteExpiredTokens(now);
        if (deletedCount > 0) {
            log.info("Cleaned up {} expired refresh tokens", deletedCount);
        }
    }
}