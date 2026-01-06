package com.starter.springboot.service;

import com.starter.springboot.entity.User;
import com.starter.springboot.entity.UserStatus;
import com.starter.springboot.exception.UserNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * Method for getting all users with pagination
     *
     * @param pageable the pagination parameters
     * @return Page of user objects.
     */
    Page<User> findAllUsers(Pageable pageable);

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

    /**
     * Find or create a user from Google OAuth information
     *
     * @param googleUserInfo - map containing Google user information (sub, email, name, etc.)
     * @return the user (existing or newly created)
     */
    User findOrCreateGoogleOAuthUser(Map<String, Object> googleUserInfo);

    /**
     * Find user by ID
     *
     * @param userId - the user id
     * @return the user
     * @throws UserNotFoundException if user not found
     */
    User findUserById(Long userId);

    /**
     * Find user by username
     *
     * @param username - the username
     * @return the user
     * @throws UserNotFoundException if user not found
     */
    User findUserByUsername(String username);

    /**
     * Get user by ID
     *
     * @param userId - the user id
     * @return the user
     */
    User getUserById(Long userId);

    /**
     * Update user details
     *
     * @param userId - the user id
     * @param userUpdates - the user updates
     * @return the updated user
     */
    User updateUser(Long userId, User userUpdates);

    /**
     * Delete user by ID
     *
     * @param userId - the user id
     */
    void deleteUser(Long userId);

    /**
     * Reset user password
     *
     * @param userId - the user id
     * @param newPassword - the new password
     */
    void resetUserPassword(Long userId, String newPassword);

    /**
     * Get users by status
     *
     * @param status - the user status
     * @param pageable - the pagination parameters
     * @return Page of user objects
     */
    Page<User> getUsersByStatus(UserStatus status, Pageable pageable);

    /**
     * Get all users (alias for findAllUsers)
     *
     * @param pageable - the pagination parameters
     * @return Page of user objects
     */
    Page<User> getAllUsers(Pageable pageable);

    /**
     * Export users to CSV format
     *
     * @return CSV byte array
     */
    byte[] exportUsersToCSV();

    /**
     * Export users to Excel format
     *
     * @return Excel byte array
     */
    byte[] exportUsersToExcel();

    /**
     * Get total user count
     *
     * @return total number of users
     */
    long getTotalUsers();

    /**
     * Get user count by status
     *
     * @param status - the user status
     * @return count of users with given status
     */
    long getUserCountByStatus(UserStatus status);

    /**
     * Get OTP required user count
     *
     * @return count of users with OTP required
     */
    long getOtpRequiredUserCount();

    /**
     * Get email verified user count
     *
     * @return count of users with verified email
     */
    long getEmailVerifiedUserCount();
}