package com.starter.springboot.service;

/**
 * Service interface for handling user logout operations.
 * Manages token invalidation, session cleanup, and cache clearance.
 */
public interface ILogoutService {

    /**
     * Perform complete logout for a user including token revocation,
     * cache cleanup, and session invalidation.
     *
     * @param userId the user ID to logout
     * @param username the username to logout
     * @param refreshToken the refresh token to revoke (optional)
     */
    void logout(Long userId, String username, String refreshToken);

    /**
     * Perform logout without refresh token (for access token only logout).
     *
     * @param userId the user ID to logout
     * @param username the username to logout
     */
    void logout(Long userId, String username);

    /**
     * Revoke all sessions for a user by invalidating all refresh tokens
     * and clearing all caches.
     *
     * @param userId the user ID
     * @param username the username
     */
    void revokeAllSessions(Long userId, String username);
}
