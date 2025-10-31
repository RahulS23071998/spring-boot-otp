package com.starter.springboot.services;

/**
 * Interface for Redis token service operations.
 * Provides contract for JWT token whitelist management.
 */
public interface IRedisTokenService {

    /**
     * Register a token jti for a user in Redis whitelist with given ttlSeconds.
     * This will overwrite any existing jti for the user (single active jti per user).
     *
     * @param userId - the user id
     * @param jti - the JWT id (jti) to register
     * @param ttlSeconds - the time to live in seconds
     */
    void registerJti(Long userId, String jti, long ttlSeconds);

    /**
     * Check if the provided jti matches the jti stored in Redis whitelist for the user.
     *
     * @param userId - the user id
     * @param jti - the JWT id (jti) to check
     * @return true if the jti is whitelisted for the user, false otherwise
     */
    boolean isJtiWhitelisted(Long userId, String jti);

    /**
     * Remove whitelist entry for a user (useful on password change or logout).
     *
     * @param userId - the user id
     */
    void removeWhitelist(Long userId);
}