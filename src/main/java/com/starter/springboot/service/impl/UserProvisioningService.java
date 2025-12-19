package com.starter.springboot.service.impl;

import com.starter.springboot.constants.SecurityConstants;
import com.starter.springboot.entity.AuthType;
import com.starter.springboot.entity.Role;
import com.starter.springboot.entity.User;
import com.starter.springboot.entity.UserStatus;
import com.starter.springboot.repository.AuthorityRepository;
import com.starter.springboot.repository.RoleRepository;
import com.starter.springboot.repository.UserRepository;
import com.starter.springboot.service.IUserProvisioningService;
import com.starter.springboot.service.LocalizationService;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Facade service for user provisioning and creation.
 * Handles complex user creation logic including role assignment,
 * authority linking, and both web signup and OAuth user flows.
 */
@Service
public class UserProvisioningService implements IUserProvisioningService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserProvisioningService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthorityRepository authorityRepository;
    private final PasswordEncoder passwordEncoder;
    private final LocalizationService localizationService;

    public UserProvisioningService(UserRepository userRepository,
                                   RoleRepository roleRepository,
                                   AuthorityRepository authorityRepository,
                                   PasswordEncoder passwordEncoder,
                                   LocalizationService localizationService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.authorityRepository = authorityRepository;
        this.passwordEncoder = passwordEncoder;
        this.localizationService = localizationService;
    }

    /**
     * Creates a new web signup user with default role and authority.
     *
     * @param user the user to create
     * @return the created user
     * @throws EntityExistsException if username already exists
     */
    @Override
    @Transactional
    public User createUser(User user) {
        userRepository.findByUsername(user.getUsername()).ifPresent(existing -> {
            throw new EntityExistsException(localizationService.getMessage("user.already_exists", user.getUsername()));
        });

        Date now = Date.from(Instant.now());
        user.setLastPasswordResetDate(now);

        if (Objects.isNull(user.getStatus())) {
            user.setStatus(UserStatus.ACTIVE);
        }
        if (Objects.isNull(user.getEnabled())) {
            user.setEnabled(Boolean.TRUE);
        }
        if (Objects.isNull(user.getAuthType())) {
            user.setAuthType(AuthType.WEB_SIGNUP);
        }
        if (Objects.isNull(user.getIsOtpRequired())) {
            user.setIsOtpRequired(Boolean.TRUE);
        }
        if (Objects.isNull(user.getPasswordSet()) || Boolean.FALSE.equals(user.getPasswordSet())) {
            user.setPasswordSet(Boolean.TRUE);
        }

        assignDefaultRoleAndAuthority(user);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    /**
     * Finds or creates a user from Google OAuth information.
     * Supports linking existing users and creating new OAuth users.
     *
     * @param googleUserInfo map containing Google user details
     * @return the existing or newly created user
     */
    @Override
    @Transactional
    public User findOrCreateGoogleOAuthUser(Map<String, Object> googleUserInfo) {
        String googleId = (String) googleUserInfo.get("sub");
        String email = (String) googleUserInfo.get("email");
        String givenName = (String) googleUserInfo.get("given_name");
        String familyName = (String) googleUserInfo.get("family_name");
        String name = (String) googleUserInfo.get("name");
        Boolean emailVerified = (Boolean) googleUserInfo.get("email_verified");

        Optional<User> existingUser = userRepository.findByGoogleId(googleId);
        if (existingUser.isPresent()) {
            LOGGER.info("Google OAuth user already exists: {}", email);
            return existingUser.get();
        }

        Optional<User> existingUserByEmail = userRepository.findByUsername(email);
        if (existingUserByEmail.isPresent()) {
            User user = existingUserByEmail.get();
            if (Objects.isNull(user.getGoogleId())) {
                user.setGoogleId(googleId);
                user.setAuthType(AuthType.GOOGLE_OAUTH);
                user.setEmailVerified(emailVerified);
                LOGGER.info("Linked existing user to Google OAuth: {}", email);
                return userRepository.save(user);
            }
        }

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setFirstName((givenName != null && !givenName.isBlank()) ? givenName : (name != null && !name.isBlank()) ? name : "Google");
        String lastName = familyName != null && familyName.length() >= 4 ? familyName : "User";
        newUser.setLastName(lastName);
        newUser.setUsername(email);
        newUser.setPassword(passwordEncoder.encode(generateRandomPassword()));
        newUser.setEnabled(Boolean.TRUE);
        newUser.setStatus(UserStatus.ACTIVE);
        newUser.setAuthType(AuthType.GOOGLE_OAUTH);
        newUser.setGoogleId(googleId);
        newUser.setEmailVerified(emailVerified);
        newUser.setIsOtpRequired(Boolean.FALSE);
        newUser.setPasswordSet(Boolean.FALSE);

        Date now = Date.from(Instant.now());
        newUser.setLastPasswordResetDate(now);

        assignDefaultRoleAndAuthority(newUser);

        User savedUser = userRepository.save(newUser);
        LOGGER.info("New Google OAuth user created: {}", email);
        return savedUser;
    }

    /**
     * Assigns the default user role and corresponding authority to a user.
     *
     * @param user the user to configure
     */
    private void assignDefaultRoleAndAuthority(User user) {
        if (Objects.isNull(user.getRole())) {
            Role defaultRole = roleRepository.findByName(SecurityConstants.USER_AUTHORITY)
                    .orElseThrow(() -> new EntityNotFoundException(localizationService.getMessage("user.default_role_not_configured")));
            user.setRole(defaultRole);
        }

        if (Objects.isNull(user.getAuthority()) && Objects.nonNull(user.getRole())) {
            authorityRepository.findByName(user.getRole().getName().replace(SecurityConstants.ROLE_PREFIX, ""))
                    .ifPresent(user::setAuthority);
        }
    }

    /**
     * Generates a random 32-character password for OAuth users.
     *
     * @return a random password string
     */
    private String generateRandomPassword() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }
}
