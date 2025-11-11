package com.starter.springboot.service;

import com.starter.springboot.entity.User;

/**
 * Service for authorizing password change operations.
 * Ensures that only authorized users (themselves or administrators) can change passwords.
 */
public interface IPasswordChangeAuthorizationService {

    /**
     * Authorize password change for a specific user.
     * 
     * @param targetUser the user whose password is being changed
     * @throws org.springframework.security.access.AccessDeniedException if the current user is not authorized
     * @throws IllegalStateException if no authenticated user is found
     */
    void authorizePasswordChange(User targetUser);

    /**
     * Authorize password change by username.
     * 
     * @param targetUsername the username of the user whose password is being changed
     * @throws org.springframework.security.access.AccessDeniedException if the current user is not authorized
     * @throws IllegalStateException if no authenticated user is found
     */
    void authorizePasswordChangeByUsername(String targetUsername);

    /**
     * Get the authenticated username from security context.
     * 
     * @return the authenticated username
     * @throws IllegalStateException if no authenticated user is found
     */
    String getAuthenticatedUsername();

    /**
     * Check if the authenticated user is an admin.
     * 
     * @return true if the authenticated user has ADMIN role, false otherwise
     */
    boolean isAuthenticatedUserAdmin();
}