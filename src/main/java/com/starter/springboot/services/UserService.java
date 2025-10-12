package com.starter.springboot.services;

import com.starter.springboot.constants.ApplicationConstants;
import com.starter.springboot.constants.DatabaseConstants;
import com.starter.springboot.constants.SecurityConstants;
import com.starter.springboot.domain.Role;
import com.starter.springboot.domain.User;
import com.starter.springboot.domain.UserStatus;
import com.starter.springboot.repositories.AuthorityRepository;
import com.starter.springboot.repositories.RoleRepository;
import com.starter.springboot.repositories.UserRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final RoleRepository roleRepository;

    private final AuthorityRepository authorityRepository;

    private final RedisTokenService redisTokenService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       RoleRepository roleRepository,
                       AuthorityRepository authorityRepository,
                       RedisTokenService redisTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.authorityRepository = authorityRepository;
        this.redisTokenService = redisTokenService;
    }

    /**
     * Method for getting all users
     *
     * @return List of user objects.
     */
    public List<User> findAllUsers() {
        return this.userRepository.findAll();
    }

    /**
     * Method for getting e-mail by username (key)
     *
     * @param username - provided username
     * @return e-mail
     */
    public String findEmailByUsername(String username)
    {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            return user.get().getEmail();
        }
        throw new EntityNotFoundException(ApplicationConstants.USER_NOT_FOUND_MESSAGE + username + " not found");
    }

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

    @Transactional
    public User changePasswordById(Long userId, java.util.Map<String, String> payload) {
        return userRepository.findById(userId)
            .map(user -> changePasswordInternal(user, payload))
            .orElseThrow(() -> new EntityNotFoundException(ApplicationConstants.USER_ID_NOT_FOUND_MESSAGE + userId + " not found"));
    }

    @Transactional
    public User changePasswordByUsername(String username, java.util.Map<String, String> payload) {
        return userRepository.findByUsername(username)
            .map(user -> changePasswordInternal(user, payload))
            .orElseThrow(() -> new EntityNotFoundException(ApplicationConstants.USER_NOT_FOUND_MESSAGE + username + " not found"));
    }

    private User changePasswordInternal(User user, java.util.Map<String, String> payload) {
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
            throw new org.springframework.security.authentication.BadCredentialsException(ApplicationConstants.OLD_PASSWORD_INCORRECT_MESSAGE);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setLastPasswordResetDate(java.util.Date.from(java.time.Instant.now()));
        User saved = userRepository.save(user);
        // Remove any whitelisted token for this user so old tokens are invalidated immediately
        try {
            if (Objects.nonNull(saved.getId())) {
                redisTokenService.removeWhitelist(saved.getId());
            }
        } catch (Exception e) {
            // Log and continue; token invalidation best-effort
            LOGGER.warn("Failed to remove token whitelist for user {}: {}", saved.getId(), e.getMessage());
        }
        return saved;
    }

}
