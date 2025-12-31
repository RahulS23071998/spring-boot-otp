package com.starter.springboot.service.impl;

import com.starter.springboot.constants.ApplicationConstants;
import com.starter.springboot.entity.PasswordHistory;
import com.starter.springboot.entity.User;
import com.starter.springboot.entity.UserStatus;
import com.starter.springboot.repository.PasswordHistoryRepository;
import com.starter.springboot.repository.UserRepository;
import com.starter.springboot.service.IPasswordChangeAuthorizationService;
import com.starter.springboot.service.IUserProvisioningService;
import com.starter.springboot.service.IUserService;
import com.starter.springboot.service.IUserTokenService;
import com.starter.springboot.service.LocalizationService;
import com.starter.springboot.exception.UserNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

/**
 * Service for managing user operations.
 * Delegates user creation to UserProvisioningService (facade)
 * and token/cache management to UserTokenService (facade).
 * Focuses on core user management operations.
 */
@Service
public class UserService implements IUserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final IPasswordChangeAuthorizationService authorizationService;
    private final IUserProvisioningService provisioningService;
    private final IUserTokenService tokenService;
    private final LocalizationService localizationService;
    private final PasswordHistoryRepository passwordHistoryRepository;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       IPasswordChangeAuthorizationService authorizationService,
                       IUserProvisioningService provisioningService,
                       IUserTokenService tokenService,
                       LocalizationService localizationService,
                       com.starter.springboot.repository.PasswordHistoryRepository passwordHistoryRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authorizationService = authorizationService;
        this.provisioningService = provisioningService;
        this.tokenService = tokenService;
        this.localizationService = localizationService;
        this.passwordHistoryRepository = passwordHistoryRepository;
    }

    /**
     * Method for getting all users
     *
     * @return List of user objects.
     */
    @Override
    @Transactional(readOnly = true)
    public List<User> findAllUsers() {
        return this.userRepository.findAll();
    }

    /**
     * Method for getting all users with pagination
     *
     * @param pageable the pagination parameters
     * @return Page of user objects.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<User> findAllUsers(Pageable pageable) {
        return this.userRepository.findAll(pageable);
    }

    /**
     * Method for getting e-mail by username (key)
     *
     * @param username - provided username
     * @return e-mail
     */
    @Override
    @Transactional(readOnly = true)
    public String findEmailByUsername(String username)
    {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            return user.get().getEmail();
        }
        throw new EntityNotFoundException(localizationService.getMessage("user.not_found", username));
    }


    @Override
    @Transactional
    public User createUser(User user) {
        return provisioningService.createUser(user);
    }

    @Override
    @Transactional
    public User updateStatus(Long userId, UserStatus status, Boolean enabled) {
        if (Objects.isNull(status) && Objects.isNull(enabled)) {
            throw new IllegalArgumentException(localizationService.getMessage("user.status_or_enabled_required"));
        }
        return userRepository.findById(userId)
            .map(existing -> {
                if (Objects.nonNull(status)) {
                    existing.setStatus(status);
                }
                if (Objects.nonNull(enabled)) {
                    existing.setEnabled(enabled);
                }
                return userRepository.save(existing);
            })
            .orElseThrow(() -> new EntityNotFoundException(localizationService.getMessage("user.id_not_found", userId)));
    }

    @Override
    @Transactional
    public User changePasswordById(Long userId, Map<String, String> payload) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException(localizationService.getMessage("user.id_not_found", userId)));
        
        // Authorize the password change (user can only change their own password or admin can change any)
        authorizationService.authorizePasswordChange(user);
        
        return changePasswordInternal(user, payload);
    }

    @Override
    @Transactional
    public User changePasswordByUsername(String username, Map<String, String> payload) {
        // Authorize the password change before fetching the user
        authorizationService.authorizePasswordChangeByUsername(username);
        
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new EntityNotFoundException(localizationService.getMessage("user.not_found", username)));
        
        return changePasswordInternal(user, payload);
    }

    private User changePasswordInternal(User user, Map<String, String> payload) {
        String oldPassword = payload.get(ApplicationConstants.OLD_PASSWORD_FIELD);
        String newPassword = payload.get(ApplicationConstants.NEW_PASSWORD_FIELD);
        String confirmNewPassword = payload.get(ApplicationConstants.CONFIRM_PASSWORD_FIELD);

        if (Objects.isNull(newPassword) || Objects.isNull(confirmNewPassword)) {
            throw new IllegalArgumentException(localizationService.getMessage("user.password_fields_required"));
        }
        if (!newPassword.equals(confirmNewPassword)) {
            throw new IllegalArgumentException(localizationService.getMessage("user.password_mismatch"));
        }
        if (Objects.isNull(oldPassword) || !passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BadCredentialsException(localizationService.getMessage("user.old_password_incorrect"));
        }

        checkPasswordHistory(user, newPassword);

        // Save old password to history before updating
        savePasswordToHistory(user);

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setLastPasswordResetDate(Date.from(Instant.now()));
        User savedUser = userRepository.save(user);

        try {
            tokenService.clearAllUserTokensAndCaches(savedUser.getUsername(), savedUser.getId());
        } catch (Exception e) {
            LOGGER.warn("Failed to clear tokens and caches for user {}: {}", savedUser.getUsername(), e.getMessage());
        }
        return savedUser;
    }

    @Override
    @Transactional
    public User findOrCreateGoogleOAuthUser(Map<String, Object> googleUserInfo) {
        return provisioningService.findOrCreateGoogleOAuthUser(googleUserInfo);
    }

    private void checkPasswordHistory(User user, String newPassword) {
        List<PasswordHistory> history = passwordHistoryRepository.findByUserOrderByCreatedDateDesc(user);
        // Check last 3 passwords
        int limit = 3;
        for (int i = 0; i < Math.min(history.size(), limit); i++) {
            if (passwordEncoder.matches(newPassword, history.get(i).getPassword())) {
                throw new IllegalArgumentException("Password has been used recently. Please choose a different password.");
            }
        }
    }

    private void savePasswordToHistory(User user) {
        PasswordHistory history = new PasswordHistory();
        history.setUser(user);
        history.setPassword(user.getPassword()); // Save the current (old) password
        passwordHistoryRepository.save(history);
    }

    @Override
    @Transactional(readOnly = true)
    public User findUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> UserNotFoundException.ofId(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> UserNotFoundException.ofUsername(username));
    }

}
