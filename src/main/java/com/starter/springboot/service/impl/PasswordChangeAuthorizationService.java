package com.starter.springboot.service.impl;

import com.starter.springboot.security.AuthoritiesConstants;
import com.starter.springboot.entity.User;
import com.starter.springboot.service.IPasswordChangeAuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Implementation of password change authorization service.
 * Validates that only authorized users can change passwords:
 * - Users can change their own password
 * - Admins can change any user's password
 */
@Service
public class PasswordChangeAuthorizationService implements IPasswordChangeAuthorizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordChangeAuthorizationService.class);

    @Override
    public void authorizePasswordChange(User targetUser) {
        Objects.requireNonNull(targetUser, "Target user cannot be null");
        
        String authenticatedUsername = getAuthenticatedUsername();
        String targetUsername = targetUser.getUsername();

        // Check if user is changing their own password or is an admin
        boolean isSameUser = authenticatedUsername.equalsIgnoreCase(targetUsername);
        boolean isAdmin = isAuthenticatedUserAdmin();

        if (!isSameUser && !isAdmin) {
            LOGGER.warn("Unauthorized password change attempt: User {} attempted to change password for user {}", 
                    authenticatedUsername, targetUsername);
            throw new AccessDeniedException("You can only change your own password. Contact an administrator to change other users' passwords.");
        }

        LOGGER.debug("Password change authorized for user {} by {} (admin: {})", 
                targetUsername, authenticatedUsername, isAdmin);
    }

    @Override
    public void authorizePasswordChangeByUsername(String targetUsername) {
        Objects.requireNonNull(targetUsername, "Target username cannot be null");
        
        String authenticatedUsername = getAuthenticatedUsername();

        // Check if user is changing their own password or is an admin
        boolean isSameUser = authenticatedUsername.equalsIgnoreCase(targetUsername);
        boolean isAdmin = isAuthenticatedUserAdmin();

        if (!isSameUser && !isAdmin) {
            LOGGER.warn("Unauthorized password change attempt: User {} attempted to change password for user {}", 
                    authenticatedUsername, targetUsername);
            throw new AccessDeniedException("You can only change your own password. Contact an administrator to change other users' passwords.");
        }

        LOGGER.debug("Password change authorized for user {} by {} (admin: {})", 
                targetUsername, authenticatedUsername, isAdmin);
    }

    @Override
    public String getAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            LOGGER.error("No authenticated user found in security context");
            throw new IllegalStateException("No authenticated user found");
        }

        String username = authentication.getName();
        if (username == null || username.isEmpty()) {
            LOGGER.error("Authenticated user has no username");
            throw new IllegalStateException("Authenticated user has no username");
        }

        return username;
    }

    @Override
    public boolean isAuthenticatedUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> 
                    auth.equals(AuthoritiesConstants.ADMIN) || 
                    auth.equals("ROLE_ADMIN") ||
                    auth.equals("ADMIN")
                );
    }
}