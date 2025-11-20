package com.starter.springboot.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.starter.springboot.entity.User;
import com.starter.springboot.exception.UserNotActivatedException;
import com.starter.springboot.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class OtpAwareAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(OtpAwareAuthenticationProvider.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    private static final int USER_CACHE_TTL_MINUTES = 5;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public OtpAwareAuthenticationProvider(UserRepository userRepository, PasswordEncoder passwordEncoder, StringRedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        final String rawUsername = authentication.getName();
        final String password = String.valueOf(authentication.getCredentials());
        final String username = rawUsername.toLowerCase();

        log.debug("Starting authentication for {}", username);

        User user = loadUserFromCacheOrDb(username);

        validateUserState(user, username);
        validatePassword(user, password, username);

        List<GrantedAuthority> authorities = buildAuthorities(user);

        DomainUserDetails userDetails = DomainUserDetails.fromUser(user, authorities);

        log.info("User '{}' authenticated. OTP Required: {}", username, user.getIsOtpRequired());

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

    private User loadUserFromCacheOrDb(String username) {
        final String cacheKey = "auth:user:" + username;

        // Try cache first
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                try {
                    User cachedUser = objectMapper.readValue(cachedJson, User.class);
                    log.debug("Loaded user {} from cache", username);
                    return cachedUser;
                } catch (Exception ex) {
                    log.warn("Corrupted cache for {}. Removing entry.", username);
                    redisTemplate.delete(cacheKey);
                }
            }
        } catch (Exception ex) {
            log.warn("Redis unavailable: {}", ex.getMessage());
        }

        // Load from DB
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User '{}' not found", username);
                    return new BadCredentialsException("Invalid username or password");
                });

        cacheUser(username, user);
        return user;
    }

    private void cacheUser(String username, User user) {
        try {
            redisTemplate.opsForValue().set(
                    "auth:user:" + username,
                    objectMapper.writeValueAsString(user),
                    USER_CACHE_TTL_MINUTES,
                    TimeUnit.MINUTES
            );
            log.debug("Cached user {}", username);
        } catch (Exception ex) {
            log.debug("Failed to cache user {}: {}", username, ex.getMessage());
        }
    }

    private void validateUserState(User user, String username) {
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            log.warn("User '{}' is not activated", username);
            throw new UserNotActivatedException("User account is not activated");
        }
    }

    private void validatePassword(User user, String rawPassword, String username) {
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            log.warn("Invalid password for '{}'", username);
            throw new BadCredentialsException("Invalid username or password");
        }
    }

    private List<GrantedAuthority> buildAuthorities(User user) {
        return List.of(new SimpleGrantedAuthority(user.getRole().getName()));
    }
}