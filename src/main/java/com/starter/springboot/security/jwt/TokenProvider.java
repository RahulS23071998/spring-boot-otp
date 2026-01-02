package com.starter.springboot.security.jwt;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.starter.springboot.constants.ApplicationConstants;
import com.starter.springboot.dto.OtpGenerationResult;
import com.starter.springboot.entity.Authority;
import com.starter.springboot.entity.RefreshToken;
import com.starter.springboot.entity.Role;
import com.starter.springboot.entity.User;
import com.starter.springboot.repository.UserRepository;
import com.starter.springboot.security.DomainUserDetails;
import com.starter.springboot.service.IOtpService;
import com.starter.springboot.service.IRedisTokenService;
import com.starter.springboot.service.IRefreshTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

import jakarta.persistence.EntityNotFoundException;
import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.time.Duration;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class TokenProvider implements ITokenProvider {

    private final Logger log = LoggerFactory.getLogger(TokenProvider.class);

    private static final String AUTHORITIES_KEY = "auth";
    
    private Key key;

    private final JwtProperties jwtProperties;
    
    @PostConstruct
    public void initialize() {
        String secretKey = jwtProperties.getSecret();
        if (Objects.isNull(secretKey) || secretKey.isBlank()) {
            throw new IllegalStateException("JWT secret (`jwt.secret`) is not configured.");
        }
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret is too short. Provide Base64-encoded key of at least 256 bits.");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.jwtParser = Jwts.parserBuilder().setSigningKey(this.key).build();
        // Initialize ObjectMapper with JavaTimeModule to enable Jackson serialization/deserialization of Java 8 date/time types (LocalDateTime, etc.)
        // Without this module, Jackson would fail to serialize User objects containing LocalDateTime fields during caching
        if (this.objectMapper == null) {
            this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        }
        // Register custom deserializer for SimpleGrantedAuthority to handle JSON deserialization from cached data
        // SimpleGrantedAuthority serializes as {"authority":"ROLE_USER"}, so we need a custom deserializer to reconstruct it
        SimpleModule module = new SimpleModule();
        module.addDeserializer(SimpleGrantedAuthority.class, new SimpleGrantedAuthorityDeserializer());
        this.objectMapper.registerModule(module);
    }

    /**
     * Custom Jackson deserializer for SimpleGrantedAuthority to handle JSON deserialization from cached data.
     * SimpleGrantedAuthority serializes as {"authority":"ROLE_USER"}, so this deserializer reads the "authority" field.
     */
    private static class SimpleGrantedAuthorityDeserializer extends JsonDeserializer<SimpleGrantedAuthority> {
        @Override
        public SimpleGrantedAuthority deserialize(com.fasterxml.jackson.core.JsonParser p, DeserializationContext ctxt) throws java.io.IOException {
            JsonNode node = p.getCodec().readTree(p);
            String authority = node.get("authority").asText();
            return new SimpleGrantedAuthority(authority);
        }
    }

    private final IOtpService otpService;

    private final UserRepository userRepository;

    private final IRedisTokenService redisTokenService;

    private final IRefreshTokenService refreshTokenService;

    private final StringRedisTemplate redisTemplate;

    protected ObjectMapper objectMapper;

    private JwtParser jwtParser;

    // Cache TTL for OTP user details (in minutes)
    private static final int OTP_CACHE_TTL_MINUTES = 5;

    /**
     * Internal record to hold token creation data
     */
    private record TokenCreationData(User user, String tokenValue, long expirationSeconds) {}

    /**
     * Internal method to create token data after OTP verification
     *
     * @param username provided username
     * @param rememberMe remember me indicator
     * @return TokenCreationData containing user, token value, and expiration
     */
    private TokenCreationData createTokenInternal(String username, Boolean rememberMe) {
        User user;
        DomainUserDetails cached = getCachedUserDetailsForOtp(username);
        if (cached != null) {
            // Construct User from cached DomainUserDetails
            user = new User();
            user.setId(cached.getUserId());
            user.setUsername(cached.getUsername());
            user.setPassword(cached.getPassword());
            user.setEmail(cached.getEmail());
            user.setIsOtpRequired(cached.isOtpRequired());
            user.setLastPasswordResetDate(cached.getLastPasswordResetDate());
            Role role = new Role();
            role.setName(cached.getRoleName());
            user.setRole(role);
            Authority authority = new Authority();
            authority.setName(cached.getAuthorityName());
            user.setAuthority(authority);
            log.debug("Using cached user details for token creation after OTP verification: {}", username);
        } else {
            // Fallback to DB query
            user = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException(ApplicationConstants.USER_NOT_FOUND_SIMPLE_MESSAGE));
        }

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(user.getRole().getName()));

        Authentication authentication = new UsernamePasswordAuthenticationToken(
            user.getUsername(), user.getPassword(), authorities
        );

        String jti = UUID.randomUUID().toString();
        long expirationSeconds = resolveExpiration(rememberMe);
        String tokenValue = generateToken(authentication, expirationSeconds, jti);

        try {
            if (Objects.nonNull(user.getId())) {
                redisTokenService.registerJti(user.getId(), jti, expirationSeconds);
            }
        } catch (Exception e) {
            log.warn("Failed to register jti in redis whitelist: {}", e.getMessage());
        }

        return new TokenCreationData(user, tokenValue, expirationSeconds);
    }

    public TokenProvider(IOtpService otpService, UserRepository userRepository, IRedisTokenService redisTokenService, IRefreshTokenService refreshTokenService, StringRedisTemplate redisTemplate, JwtProperties jwtProperties) {
        this.otpService = otpService;
        this.userRepository = userRepository;
        this.redisTokenService = redisTokenService;
        this.refreshTokenService = refreshTokenService;
        this.redisTemplate = redisTemplate;
        this.jwtProperties = jwtProperties;
    }


    /**
     * Create token from authentication. If OTP is required, the caller must complete the OTP flow.
     *
     * @param authentication authentication object
     * @param rememberMe remember me indicator
     * @return payload containing HTTP status and optional JWT token
     */
    @Override
    public TokenCreationResponse createToken(Authentication authentication, Boolean rememberMe) {
        String username = authentication.getName();
        DomainUserDetails userDetails = resolveDomainUserDetails(authentication);

        if (Boolean.TRUE.equals(userDetails.isOtpRequired())) {
            OtpGenerationResult otpResult = otpService.generateOtp(userDetails.getUsername(), userDetails.getEmail());
            if (!otpResult.isSuccess()) {
                return TokenCreationResponse.rejected(otpResult.getMessage());
            }
            // Cache user details to avoid DB query in createTokenAfterVerifiedOtp
            cacheUserDetailsForOtp(username, userDetails);
            return TokenCreationResponse.pendingOtp("OTP required to complete authentication.");
        }

        // generate jti and tokens
        String jti = UUID.randomUUID().toString();
        long expirationSeconds = resolveExpiration(rememberMe);
        String tokenValue = generateToken(authentication, expirationSeconds, jti);

        // Create refresh token
        String refreshTokenValue = null;
        long refreshTokenExpirationSeconds = jwtProperties.getRefreshExpiration();
        if (Objects.nonNull(userDetails.getUserId())) {
            try {
                refreshTokenService.revokeAllUserRefreshTokens(userDetails.getUserId());
                RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getUserId(), refreshTokenExpirationSeconds);
                refreshTokenValue = refreshToken.getToken();
            } catch (Exception e) {
                log.warn("Failed to create refresh token: {}", e.getMessage());
            }
        }

        // register jti in redis whitelist for this user
        try {
            if (Objects.nonNull(userDetails.getUserId())) {
                redisTokenService.registerJti(userDetails.getUserId(), jti, expirationSeconds);
            }
        } catch (Exception e) {
            log.warn("Failed to register jti in redis whitelist: {}", e.getMessage());
        }

        JWTToken token = JWTToken.bearerTokenWithRefresh(tokenValue, refreshTokenValue, expirationSeconds, refreshTokenExpirationSeconds);
        return TokenCreationResponse.accepted(token);
    }

    /**
     * Create access token after verified OTP code (without creating refresh token)
     *
     * @param username provided username
     * @param rememberMe remember me indicator
     * @return JWTToken with access token only
     */
    @Override
    public JWTToken createAccessTokenAfterVerifiedOtp(String username, Boolean rememberMe) {
        TokenCreationData tokenData = createTokenInternal(username, rememberMe);
        return JWTToken.bearerToken(tokenData.tokenValue(), tokenData.expirationSeconds());
    }

    /**
     * Create token after verified OTP code
     *
     * @param username provided username
     * @param rememberMe remember me indicator
     * @return JWTToken with access and refresh tokens
     */
    @Override
    public JWTToken createTokenAfterVerifiedOtp(String username, Boolean rememberMe) {
        TokenCreationData tokenData = createTokenInternal(username, rememberMe);

        // Create refresh token
        String refreshTokenValue = null;
        long refreshTokenExpirationSeconds = jwtProperties.getRefreshExpiration();
        try {
            refreshTokenService.revokeAllUserRefreshTokens(tokenData.user().getId());
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(tokenData.user().getId(), refreshTokenExpirationSeconds);
            refreshTokenValue = refreshToken.getToken();
        } catch (Exception e) {
            log.warn("Failed to create refresh token: {}", e.getMessage());
        }

        return JWTToken.bearerTokenWithRefresh(tokenData.tokenValue(), refreshTokenValue, tokenData.expirationSeconds(), refreshTokenExpirationSeconds);
    }

    /**
     * Method for getting authentication context.
     *
     * @param token provided token
     * @return Authentication Object
     */
    @Override
    public Authentication getAuthentication(String token)
    {
        Claims claims = jwtParser
            .parseClaimsJws(token)
            .getBody();

        String principal = claims.getSubject();
        Collection<GrantedAuthority> authorities = Arrays
            .stream(claims.get(AUTHORITIES_KEY).toString().split(","))
            .map(String::trim)
            .filter(auth -> !auth.isEmpty())
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());

        log.debug("Parsed authorities for user {}: {}", principal, authorities);
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    /**
     * Method for validate token.
     *
     * @param authToken - JWT token
     * @return true | false
     */
    @Override
    @Transactional(readOnly = true)
    public boolean validateToken(String authToken) {
        try
        {
            Claims claims = jwtParser.parseClaimsJws(authToken).getBody();
            String username = claims.getSubject();
            Date issuedAt = claims.getIssuedAt();
            String jti = claims.getId();
            if (Objects.isNull(username) || Objects.isNull(issuedAt) || Objects.isNull(jti)) {
                log.warn("JWT missing subject or issuedAt or jti");
                return false;
            }

            // Check if user's password was reset after token was issued
            return userRepository.findByUsername(username)
                .map(user -> {
                    Date lastReset = user.getLastPasswordResetDate();
                    if (Objects.nonNull(lastReset)) {
                        // reject token if it was issued at or before the last password reset
                        if (issuedAt.compareTo(lastReset) <= 0) {
                            log.info("Rejecting JWT for user {}: issuedAt={} <= lastPasswordResetDate={}", username, issuedAt, lastReset);
                            return false;
                        }
                    }

                    // Check Redis whitelist: token's jti must match currently whitelisted jti for this user
                    try {
                        if (Objects.nonNull(user.getId())) {
                            boolean whitelisted = redisTokenService.isJtiWhitelisted(user.getId(), jti);
                            if (!whitelisted) {
                                log.info("JTI for user {} is not whitelisted in redis", username);
                                return false;
                            }
                        }
                    } catch (Exception e) {
                        // If Redis check fails, fall back to lastPasswordResetDate check only (already done above)
                        log.warn("Redis whitelist check failed: {}", e.getMessage());
                    }

                    return true;
                })
                .orElseGet(() -> {
                    log.warn("User not found while validating JWT: {}", username);
                    return false;
                });
        }
        catch (Exception e)
        {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Generating token from authentication object
     *
     * @param authentication provided authentication
     * @param expirationSeconds token validity in seconds
     * @return String value of jwt token
     */
    private String generateToken(Authentication authentication, long expirationSeconds, String jti) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = System.currentTimeMillis();
        Date issuedAt = new Date(now);
        Date validity = new Date(now + expirationSeconds * 1000);

        return Jwts.builder()
            .setId(jti)
            .setSubject(authentication.getName())
            .claim(AUTHORITIES_KEY, authorities)
            .setIssuedAt(issuedAt)
            .setExpiration(validity)
            .signWith(key)
            .compact();
    }

    private long resolveExpiration(Boolean rememberMe) {
        return Boolean.TRUE.equals(rememberMe) ? jwtProperties.getExpirationRememberMe() : jwtProperties.getExpiration();
    }

    /**
     * Get refresh token validity in seconds
     * @return refresh token validity in seconds
     */
    @Override
    public long getRefreshTokenValidityInSeconds() {
        return jwtProperties.getRefreshExpiration();
    }

    private DomainUserDetails resolveDomainUserDetails(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof DomainUserDetails domainUserDetails) {
            return domainUserDetails;
        }
        throw new IllegalArgumentException(
                "Authentication principal is not an instance of DomainUserDetails. Received: "
                        + principal.getClass()
        );
    }

    // No need for CachedUserDetails, we'll cache User directly

    /**
     * Cache user details during OTP authentication to avoid repeated DB queries
     * @param username the username
     * @param userDetails the user details to cache
     */
    public void cacheUserDetailsForOtp(String username, DomainUserDetails userDetails) {
        try {
            String cacheKey = "otp:user:" + username;
            String jsonValue = objectMapper.writeValueAsString(userDetails);
            redisTemplate.opsForValue().set(cacheKey, jsonValue, Duration.ofMinutes(OTP_CACHE_TTL_MINUTES));
            log.debug("Cached user details for OTP authentication: {}", username);
        } catch (Exception e) {
            log.warn("Failed to cache user details for OTP: {}", e.getMessage());
        }
    }

    /**
     * Retrieve cached user details for OTP authentication
     * @param username the username
     * @return cached user details or null if not found
     */
    public DomainUserDetails getCachedUserDetailsForOtp(String username) {
        try {
            String cacheKey = "otp:user:" + username;
            String json = redisTemplate.opsForValue().get(cacheKey);
            if (json != null) {
                DomainUserDetails userDetails = objectMapper.readValue(json, DomainUserDetails.class);
                log.debug("Retrieved cached user details for OTP authentication: {}", username);
                return userDetails;
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve cached user details for OTP: {}", e.getMessage());
        }
        return null;
    }

}
