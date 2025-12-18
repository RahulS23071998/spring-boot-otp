package com.starter.springboot.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.starter.springboot.entity.User;
import com.starter.springboot.exception.UserNotActivatedException;
import com.starter.springboot.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import com.starter.springboot.entity.Authority;
import com.starter.springboot.entity.Role;

public class OtpAwareAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(OtpAwareAuthenticationProvider.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    private static final int USER_CACHE_TTL_MINUTES = 5;

    private final ObjectMapper objectMapper;

    public OtpAwareAuthenticationProvider(UserRepository userRepository, PasswordEncoder passwordEncoder, StringRedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;

        // configure ObjectMapper once
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        final String providedPassword = authentication.getCredentials() == null ? null : String.valueOf(authentication.getCredentials());
        final String normalizedUsername = Objects.isNull(authentication.getName()) ? null : authentication.getName().toLowerCase();

        log.debug("Starting authentication for {}", normalizedUsername);

        if (Objects.isNull(normalizedUsername)) {
            log.warn("Authentication failed: username is null");
            throw new BadCredentialsException("Invalid username or password");
        }

        User user = loadUserFromCacheOrDb(normalizedUsername);

        validateUserState(user, normalizedUsername);
        validatePassword(user, providedPassword, normalizedUsername);

        List<GrantedAuthority> authorities = buildAuthorities(user);

        DomainUserDetails userDetails = DomainUserDetails.fromUser(user, authorities);

        log.info("User '{}' authenticated. OTP Required: {}", normalizedUsername, user.getIsOtpRequired());

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null, //we should not store password in authentication object because we are storing it in cache
                authorities
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private User loadUserFromCacheOrDb(String normalizedUsername) {
        final String cacheKey = "auth:user:" + normalizedUsername;

        // Try cache first
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (Objects.nonNull(cachedJson)) {
                try {
                    CachedUser cached = objectMapper.readValue(cachedJson, CachedUser.class);
                    log.debug("Loaded user {} from cache", normalizedUsername);
                    return toUser(cached);
                } catch (Exception ex) {
                    log.warn("Corrupted cache for {}. Removing entry.", normalizedUsername);
                    try {
                        redisTemplate.delete(cacheKey);
                    } catch (Exception e) {
                        log.warn("Failed to delete corrupted cache for {}: {}", normalizedUsername, e.getMessage());
                    }
                }
            }
        } catch (RedisConnectionFailureException ex) {
            log.warn("Redis unavailable: {}", ex.getMessage());
        }

        // Load from DB
        User user = userRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> {
                    log.warn("User '{}' not found", normalizedUsername);
                    return new BadCredentialsException("Invalid username or password");
                });

        cacheUser(normalizedUsername, user);
        return user;
    }

    private void cacheUser(String normalizedUsername, User user) {
        try {
            CachedUser cached = fromUser(user);
            redisTemplate.opsForValue().set(
                    "auth:user:" + normalizedUsername,
                    objectMapper.writeValueAsString(cached),
                    USER_CACHE_TTL_MINUTES,
                    TimeUnit.MINUTES
            );
            log.debug("Cached user {}", normalizedUsername);
        } catch (Exception ex) {
            log.debug("Failed to cache user {}: {}", normalizedUsername, ex.getMessage());
        }
    }

    private CachedUser fromUser(User user) {
        String roleName = Objects.nonNull(user.getRole()) ? user.getRole().getName() : null;
        String authorityName = Objects.nonNull(user.getAuthority()) ? user.getAuthority().getName() : null;
        return new CachedUser(
                user.getId(),
                user.getUsername(),
                user.getPassword(), // Store password hash in cache
                user.getEmail(),
                user.getEnabled(),
                user.getIsOtpRequired(),
                roleName,
                authorityName,
                user.getLastPasswordResetDate()
        );
    }

    private User toUser(CachedUser c) {
        User userEntity = new User();
        userEntity.setId(c.getId());
        userEntity.setUsername(c.getUsername());
        userEntity.setPassword(c.getPassword());
        userEntity.setEmail(c.getEmail());
        userEntity.setEnabled(c.getEnabled());
        userEntity.setIsOtpRequired(c.getOtpRequired());
        userEntity.setLastPasswordResetDate(c.getLastPasswordResetDate());
        // set minimal Role/Authority objects so authority names are preserved when loaded from cache
        if (Objects.nonNull(c.getRoleName())) {
            Role roleEntity = new Role();
            roleEntity.setName(c.getRoleName());
            userEntity.setRole(roleEntity);
        }
        if (Objects.nonNull(c.getAuthorityName())) {
            Authority authorityEntity = new Authority();
            authorityEntity.setName(c.getAuthorityName());
            userEntity.setAuthority(authorityEntity);
        }
        return userEntity;
    }

    private void validateUserState(User user, String normalizedUsername) {
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            log.warn("User '{}' is not activated", normalizedUsername);
            throw new UserNotActivatedException("User account is not activated");
        }

        if (user.getAuthType() != null &&
            user.getAuthType().toString().equals("GOOGLE_OAUTH") &&
            !Boolean.TRUE.equals(user.getPasswordSet())) {
            log.warn("Google OAuth user '{}' has not set password yet", normalizedUsername);
            throw new BadCredentialsException("Please set your password first before signing in with email/password");
        }
    }

    private void validatePassword(User user, String providedPassword, String normalizedUsername) {
        if (Objects.isNull(providedPassword) || Objects.isNull(user.getPassword())) {
            log.warn("Invalid password (null) for '{}'", normalizedUsername);
            throw new BadCredentialsException("Invalid username or password");
        }

        if (!passwordEncoder.matches(providedPassword, user.getPassword())) {
            log.warn("Invalid password for '{}'", normalizedUsername);
            throw new BadCredentialsException("Invalid username or password");
        }
    }

    private List<GrantedAuthority> buildAuthorities(User user) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        if (user.getRole() != null && user.getRole().getName() != null) {
            authorities.add(new SimpleGrantedAuthority(user.getRole().getName()));
        }

        if (user.getAuthority() != null && user.getAuthority().getName() != null) {
            authorities.add(new SimpleGrantedAuthority(user.getAuthority().getName()));
        }

        return authorities;
    }
}