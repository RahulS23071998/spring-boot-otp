package com.starter.springboot.services;

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
        throw new EntityNotFoundException("User with username " + username + " not found");
    }

    @Transactional
    public User createUser(User user) {
        userRepository.findByUsername(user.getUsername()).ifPresent(existing -> {
            throw new EntityExistsException("User with username " + user.getUsername() + " already exists");
        });
        Date now = Date.from(Instant.now());
        user.setLastPasswordResetDate(now);
        if (user.getStatus() == null) {
            user.setStatus(UserStatus.ACTIVE);
        }
        if (user.getEnabled() == null) {
            user.setEnabled(Boolean.TRUE);
        }
        if (user.getRole() == null) {
            Role defaultRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new EntityNotFoundException("Default role ROLE_USER not configured"));
            user.setRole(defaultRole);
        }
        if (user.getAuthority() == null && user.getRole() != null) {
            authorityRepository.findByName(user.getRole().getName().replace("ROLE_", ""))
                    .ifPresent(user::setAuthority);
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Transactional
    public User updateStatus(Long userId, UserStatus status, Boolean enabled) {
        if (status == null && enabled == null) {
            throw new IllegalArgumentException("Either status or enabled must be provided");
        }
        return userRepository.findById(userId)
            .map(existing -> {
                if (status != null) {
                    existing.setStatus(status);
                }
                if (enabled != null) {
                    existing.setEnabled(enabled);
                }
                return userRepository.save(existing);
            })
            .orElseThrow(() -> new EntityNotFoundException("User with id " + userId + " not found"));
    }

    @Transactional
    public User changePasswordById(Long userId, java.util.Map<String, String> payload) {
        return userRepository.findById(userId)
            .map(user -> changePasswordInternal(user, payload))
            .orElseThrow(() -> new EntityNotFoundException("User with id " + userId + " not found"));
    }

    @Transactional
    public User changePasswordByUsername(String username, java.util.Map<String, String> payload) {
        return userRepository.findByUsername(username)
            .map(user -> changePasswordInternal(user, payload))
            .orElseThrow(() -> new EntityNotFoundException("User with username " + username + " not found"));
    }

    private User changePasswordInternal(User user, java.util.Map<String, String> payload) {
        String oldPassword = payload.get("oldpassword");
        String newPassword = payload.get("newpassword");
        String confirmNewPassword = payload.get("confirmnewpassword");

        if (newPassword == null || confirmNewPassword == null) {
            throw new IllegalArgumentException("New password and confirm new password must be provided");
        }
        if (!newPassword.equals(confirmNewPassword)) {
            throw new IllegalArgumentException("New password and confirm new password do not match");
        }
        if (oldPassword == null || !passwordEncoder.matches(oldPassword, user.getPassword())) {
            // Use BadCredentialsException to indicate authentication failure
            throw new org.springframework.security.authentication.BadCredentialsException("Old password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setLastPasswordResetDate(java.util.Date.from(java.time.Instant.now()));
        User saved = userRepository.save(user);
        // Remove any whitelisted token for this user so old tokens are invalidated immediately
        try {
            if (saved.getId() != null) {
                redisTokenService.removeWhitelist(saved.getId());
            }
        } catch (Exception e) {
            // Log and continue; token invalidation best-effort
            LOGGER.warn("Failed to remove token whitelist for user {}: {}", saved.getId(), e.getMessage());
        }
        return saved;
    }

}
