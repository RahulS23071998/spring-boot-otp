package com.starter.springboot.service;

/**
 * Service for managing authentication-related caches.
 * Provides methods to invalidate cached authentication data when user information changes.
 */
public interface IAuthCacheService {

    /**
     * Clears the cached authentication data for a specific user.
     * Should be called when user credentials or sensitive data changes.
     *
     * @param username the username whose cache should be cleared
     */
    void clearAuthCacheForUser(String username);

    /**
     * Clears all authentication caches.
     * Use with caution as this affects all users.
     */
    void clearAllAuthCache();

    /**
     * Clears both authentication cache and token whitelist for a user.
     * Comprehensive cache clearing when user password changes.
     *
     * @param username the username whose caches should be cleared
     * @param userId the user ID for token whitelist clearing
     */
    void clearAllCachesForUser(String username, Long userId);
}