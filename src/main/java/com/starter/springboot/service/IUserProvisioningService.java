package com.starter.springboot.service;

import com.starter.springboot.entity.User;
import java.util.Map;

/**
 * Service interface for user provisioning and creation.
 * Handles user creation logic including role assignment, authority linking,
 * and both web signup and OAuth user flows.
 */
public interface IUserProvisioningService {

    /**
     * Creates a new web signup user with default role and authority.
     *
     * @param user the user to create
     * @return the created user
     * @throws jakarta.persistence.EntityExistsException if username already exists
     */
    User createUser(User user);

    /**
     * Finds or creates a user from Google OAuth information.
     * Supports linking existing users and creating new OAuth users.
     *
     * @param googleUserInfo map containing Google user details
     * @return the existing or newly created user
     */
    User findOrCreateGoogleOAuthUser(Map<String, Object> googleUserInfo);
}
