package com.starter.springboot.security;

import com.starter.springboot.domain.User;
import com.starter.springboot.exceptions.UserNotActivatedException;
import com.starter.springboot.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Custom Authentication Provider optimized for OTP-based authentication.
 * Handles credential validation and OTP requirements in a single provider.
 * 
 * Benefits over DaoAuthenticationProvider:
 * - Direct control over authentication flow
 * - Integrated OTP status awareness
 * - Custom error handling for business logic
 * - Audit logging at authentication point
 * - Single source of truth for auth decisions
 */
public class OtpAwareAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(OtpAwareAuthenticationProvider.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Cache TTL for user authentication data (in minutes)
    private static final int USER_CACHE_TTL_MINUTES = 5;

    public OtpAwareAuthenticationProvider(UserRepository userRepository, PasswordEncoder passwordEncoder, StringRedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    @Transactional
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = (String) authentication.getCredentials();

        log.debug("Authenticating user: {}", username);

        String lowercaseLogin = username.toLowerCase();
        String cacheKey = "auth:user:" + lowercaseLogin;
        User user = null;

        // Try to get from cache first
        String cachedUserJson = redisTemplate.opsForValue().get(cacheKey);
        if (cachedUserJson != null) {
            try {
                user = objectMapper.readValue(cachedUserJson, User.class);
                log.debug("Loaded user from cache: {}", lowercaseLogin);
            } catch (Exception e) {
                log.warn("Failed to deserialize cached user for {}: {}", lowercaseLogin, e.getMessage());
                redisTemplate.delete(cacheKey); // Remove corrupted cache
                cachedUserJson = null;
            }
        }

        if (cachedUserJson == null) {
            // Load user from database
            user = userRepository.findByUsername(lowercaseLogin)
                    .orElseThrow(() -> {
                        log.warn("User not found: {}", lowercaseLogin);
                        return new BadCredentialsException("Invalid username or password");
                    });

            // Cache the user for short time to avoid stale data
            try {
                String userJson = objectMapper.writeValueAsString(user);
                redisTemplate.opsForValue().set(cacheKey, userJson, USER_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                log.debug("Cached user: {}", lowercaseLogin);
            } catch (Exception e) {
                log.warn("Failed to cache user {}: {}", lowercaseLogin, e.getMessage());
            }
        }

        // Validate user is activated
        if (Objects.isNull(user.getEnabled()) || !user.getEnabled()) {
            log.warn("User account disabled: {}", lowercaseLogin);
            throw new UserNotActivatedException("User account is not activated");
        }

        // Validate password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("Invalid password for user: {}", lowercaseLogin);
            throw new BadCredentialsException("Invalid username or password");
        }

        // Build authorities
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority(user.getRole().getName())
        );

        // Create DomainUserDetails with OTP information
        DomainUserDetails userDetails = DomainUserDetails.fromUser(user, authorities);

        log.info("User {} authenticated successfully. OTP Required: {}", lowercaseLogin, user.getIsOtpRequired());

        // Return authenticated token with DomainUserDetails as principal
        // This preserves OTP information in the authentication object
        return new UsernamePasswordAuthenticationToken(
                userDetails,
                password,
                authorities
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}