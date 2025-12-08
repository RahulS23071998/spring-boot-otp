package com.starter.springboot.service;

/**
 * Service interface for managing user tokens and authentication caches.
 * Provides operations to clear tokens and caches when users change passwords or log out.
 */
public interface IUserTokenService {

    /**
     * Clears all authentication tokens and caches for a specific user.
     * This ensures the user must re-authenticate after password changes.
     *
     * @param username the username of the user
     * @param userId the unique identifier of the user
     */
    void clearAllUserTokensAndCaches(String username, Long userId);
}
