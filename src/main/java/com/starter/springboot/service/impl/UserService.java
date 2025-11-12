package com.starter.springboot.service.impl;

import com.starter.springboot.constants.ApplicationConstants;
import com.starter.springboot.constants.SecurityConstants;
import com.starter.springboot.entity.Role;
import com.starter.springboot.entity.User;
import com.starter.springboot.entity.UserStatus;
import com.starter.springboot.repository.AuthorityRepository;
import com.starter.springboot.repository.RoleRepository;
import com.starter.springboot.repository.UserRepository;
import com.starter.springboot.service.IAuthCacheService;
import com.starter.springboot.service.IdGeneratorService;
import com.starter.springboot.service.IPasswordChangeAuthorizationService;
import com.starter.springboot.service.IRedisTokenService;
import com.starter.springboot.service.IRefreshTokenService;
import com.starter.springboot.service.IUserService;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

@Service
public class UserService implements IUserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final RoleRepository roleRepository;

    private final AuthorityRepository authorityRepository;

    private final IRedisTokenService redisTokenService;

    private final IPasswordChangeAuthorizationService authorizationService;

    private final IdGeneratorService idGeneratorService;

    private final IAuthCacheService authCacheService;

    private final IRefreshTokenService refreshTokenService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       RoleRepository roleRepository,
                       AuthorityRepository authorityRepository,
                       IRedisTokenService redisTokenService,
                       IPasswordChangeAuthorizationService authorizationService,
                       IdGeneratorService idGeneratorService,
                       IAuthCacheService authCacheService,
                       IRefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.authorityRepository = authorityRepository;
        this.redisTokenService = redisTokenService;
        this.authorizationService = authorizationService;
        this.idGeneratorService = idGeneratorService;
        this.authCacheService = authCacheService;
        this.refreshTokenService = refreshTokenService;
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
        throw new EntityNotFoundException(ApplicationConstants.USER_NOT_FOUND_MESSAGE + username + " not found");
    }


    @Override
    @Transactional
    public User createUser(User user) {
        userRepository.findByUsername(user.getUsername()).ifPresent(existing -> {
            throw new EntityExistsException(ApplicationConstants.USER_ALREADY_EXISTS_MESSAGE + user.getUsername() + " already exists");
        });
        Date now = Date.from(Instant.now());
        user.setLastPasswordResetDate(now);
        if (Objects.isNull(user.getStatus())) {
            user.setStatus(UserStatus.ACTIVE);
        }
        if (Objects.isNull(user.getEnabled())) {
            user.setEnabled(Boolean.TRUE);
        }
        if (Objects.isNull(user.getRole())) {
            Role defaultRole = roleRepository.findByName(SecurityConstants.USER_AUTHORITY)
                    .orElseThrow(() -> new EntityNotFoundException(ApplicationConstants.DEFAULT_ROLE_NOT_CONFIGURED_MESSAGE));
            user.setRole(defaultRole);
        }
        if (Objects.isNull(user.getAuthority()) && Objects.nonNull(user.getRole())) {
            authorityRepository.findByName(user.getRole().getName().replace(SecurityConstants.ROLE_PREFIX, ""))
                    .ifPresent(user::setAuthority);
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateStatus(Long userId, UserStatus status, Boolean enabled) {
        if (Objects.isNull(status) && Objects.isNull(enabled)) {
            throw new IllegalArgumentException(ApplicationConstants.STATUS_OR_ENABLED_REQUIRED_MESSAGE);
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
            .orElseThrow(() -> new EntityNotFoundException(ApplicationConstants.USER_ID_NOT_FOUND_MESSAGE + userId + " not found"));
    }

    @Override
    @Transactional
    public User changePasswordById(Long userId, Map<String, String> payload) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException(ApplicationConstants.USER_ID_NOT_FOUND_MESSAGE + userId + " not found"));
        
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
            .orElseThrow(() -> new EntityNotFoundException(ApplicationConstants.USER_NOT_FOUND_MESSAGE + username + " not found"));
        
        return changePasswordInternal(user, payload);
    }

    private User changePasswordInternal(User user, Map<String, String> payload) {
        String oldPassword = payload.get(ApplicationConstants.OLD_PASSWORD_FIELD);
        String newPassword = payload.get(ApplicationConstants.NEW_PASSWORD_FIELD);
        String confirmNewPassword = payload.get(ApplicationConstants.CONFIRM_PASSWORD_FIELD);

        if (Objects.isNull(newPassword) || Objects.isNull(confirmNewPassword)) {
            throw new IllegalArgumentException(ApplicationConstants.PASSWORD_FIELDS_REQUIRED_MESSAGE);
        }
        if (!newPassword.equals(confirmNewPassword)) {
            throw new IllegalArgumentException(ApplicationConstants.PASSWORD_MISMATCH_MESSAGE);
        }
        if (Objects.isNull(oldPassword) || !passwordEncoder.matches(oldPassword, user.getPassword())) {
            // Use BadCredentialsException to indicate authentication failure
            throw new BadCredentialsException(ApplicationConstants.OLD_PASSWORD_INCORRECT_MESSAGE);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setLastPasswordResetDate(Date.from(java.time.Instant.now()));
        User savedUser = userRepository.save(user);

        // Clear authentication cache, token whitelist, and refresh tokens for this user
        // This ensures the new password is used immediately on next authentication
        try {
            authCacheService.clearAllCachesForUser(savedUser.getUsername(), savedUser.getId());
            refreshTokenService.revokeAllUserRefreshTokens(savedUser.getId());
            LOGGER.info("Cleared authentication cache, token whitelist, and refresh tokens for user after password change: {}", savedUser.getUsername());
        } catch (Exception e) {
            // Log and continue; cache clearing is best-effort
            LOGGER.warn("Failed to clear caches for user {}: {}", savedUser.getUsername(), e.getMessage());
        }
        return savedUser;
    }

}
