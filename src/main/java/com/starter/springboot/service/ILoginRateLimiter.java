package com.starter.springboot.service;

public interface ILoginRateLimiter {
    /**
     * Check if the login attempt is allowed.
     * @param key The key to check (username or IP)
     * @return true if allowed, false if rate limited
     */
    boolean isAllowed(String key);

    /**
     * Record a login attempt.
     * @param key The key to record (username or IP)
     */
    void recordAttempt(String key);
}
