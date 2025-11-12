package com.starter.springboot.service;

import com.starter.springboot.entity.RefreshToken;

/**
 * Service interface for managing refresh tokens.
 */
public interface IRefreshTokenService {

    /**
     * Create a new refresh token for a user
     * @param userId the user ID
     * @param expirationSeconds token validity in seconds
     * @return the created refresh token
     */
    RefreshToken createRefreshToken(Long userId, long expirationSeconds);

    /**
     * Validate and retrieve refresh token
     * @param token the refresh token string
     * @return the refresh token entity if valid
     */
    RefreshToken validateRefreshToken(String token);

    /**
     * Revoke a specific refresh token
     * @param token the refresh token to revoke
     */
    void revokeRefreshToken(String token);

    /**
     * Revoke all refresh tokens for a user
     * @param userId the user ID
     */
    void revokeAllUserRefreshTokens(Long userId);

    /**
     * Refresh token rotation - revoke old token and create new one
     * @param oldToken the old refresh token
     * @param userId the user ID
     * @param expirationSeconds new token validity in seconds
     * @return new refresh token
     */
    RefreshToken rotateRefreshToken(String oldToken, Long userId, long expirationSeconds);

    /**
     * Clean up expired refresh tokens
     */
    void cleanupExpiredTokens();
}