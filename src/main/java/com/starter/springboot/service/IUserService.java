package com.starter.springboot.service;

import com.starter.springboot.entity.User;
import com.starter.springboot.entity.UserStatus;
import java.util.List;
import java.util.Map;

/**
 * Interface for User service operations.
 * Provides contract for user management and authentication functionality.
 */
public interface IUserService {

    /**
     * Method for getting all users
     *
     * @return List of user objects.
     */
    List<User> findAllUsers();

    /**
     * Method for getting e-mail by username (key)
     *
     * @param username - provided username
     * @return e-mail
     */
    String findEmailByUsername(String username);

    /**
     * Create a new user
     *
     * @param user - the user to create
     * @return the created user
     */
    User createUser(User user);

    /**
     * Update user status
     *
     * @param userId - the user id
     * @param status - the new status
     * @param enabled - the new enabled state
     * @return the updated user
     */
    User updateStatus(Long userId, UserStatus status, Boolean enabled);

    /**
     * Change password by user id
     *
     * @param userId - the user id
     * @param payload - map containing old and new passwords
     * @return the updated user
     */
    User changePasswordById(Long userId, Map<String, String> payload);

    /**
     * Change password by username
     *
     * @param username - the username
     * @param payload - map containing old and new passwords
     * @return the updated user
     */
    User changePasswordByUsername(String username, Map<String, String> payload);
}