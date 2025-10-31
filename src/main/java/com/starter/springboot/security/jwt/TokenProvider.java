package com.starter.springboot.security.jwt;

import com.starter.springboot.constants.ApplicationConstants;
import com.starter.springboot.domain.User;
import com.starter.springboot.repositories.UserRepository;
import com.starter.springboot.security.DomainUserDetails;
import com.starter.springboot.services.IOtpService;
import com.starter.springboot.services.IRedisTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityNotFoundException;
import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class TokenProvider implements InitializingBean {

    private final Logger log = LoggerFactory.getLogger(TokenProvider.class);

    private static final String AUTHORITIES_KEY = "auth";
    
    private Key key;

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long tokenValidityInSeconds;

    @Value("${jwt.expirationRememberMe:${jwt.expiration}}")
    private long tokenValidityInSecondsForRememberMe;
    
    @Override
    public void afterPropertiesSet() {
        if (Objects.isNull(secretKey) || secretKey.isBlank()) {
            throw new IllegalStateException("JWT secret (`jwt.secret`) is not configured.");
        }
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret is too short. Provide Base64-encoded key of at least 256 bits.");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.jwtParser = Jwts.parserBuilder().setSigningKey(this.key).build();
    }

    private final IOtpService otpService;

    private final UserRepository userRepository;

    private final IRedisTokenService redisTokenService;

    private JwtParser jwtParser;

    public TokenProvider(IOtpService otpService, UserRepository userRepository, IRedisTokenService redisTokenService) {
        this.otpService = otpService;
        this.userRepository = userRepository;
        this.redisTokenService = redisTokenService;
    }


    /**
     * Create token from authentication. If OTP is required, the caller must complete the OTP flow.
     *
     * @param authentication authentication object
     * @param rememberMe remember me indicator
     * @return payload containing HTTP status and optional JWT token
     */
    public TokenCreationResponse createToken(Authentication authentication, Boolean rememberMe) {
        String username = authentication.getName();
        DomainUserDetails userDetails = resolveDomainUserDetails(authentication);

        if (Boolean.TRUE.equals(userDetails.isOtpRequired())) {
            boolean otpIssued = otpService.generateOtp(userDetails.getUsername(), userDetails.getEmail());
            if (Boolean.FALSE.equals(otpIssued)) {
                return TokenCreationResponse.rejected("Maximum OTP attempts exceeded. Try again later.");
            }
            return TokenCreationResponse.pendingOtp("OTP required to complete authentication.");
        }

        // generate jti and token
        String jti = UUID.randomUUID().toString();
        long expirationSeconds = resolveExpiration(rememberMe);
        String tokenValue = generateToken(authentication, expirationSeconds, jti);
        // register jti in redis whitelist for this user
        try {
            if (Objects.nonNull(userDetails.getUserId())) {
                redisTokenService.registerJti(userDetails.getUserId(), jti, expirationSeconds);
            }
        } catch (Exception e) {
            log.warn("Failed to register jti in redis whitelist: {}", e.getMessage());
        }

        JWTToken token = JWTToken.bearerToken(tokenValue, expirationSeconds);
        return TokenCreationResponse.accepted(token);
    }

    /**
     * Create token after verified OTP code
     *
     * @param username provided username
     * @param rememberMe remember me indicator
     * @return String token value
     */
    public JWTToken createTokenAfterVerifiedOtp(String username, Boolean rememberMe)
    {
        User user = userRepository
            .findByUsername(username)
            .orElseThrow(() -> new EntityNotFoundException(ApplicationConstants.USER_NOT_FOUND_SIMPLE_MESSAGE));

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
        return JWTToken.bearerToken(tokenValue, expirationSeconds);
    }

    /**
     * Method for getting authentication context.
     *
     * @param token provided token
     * @return Authentication Object
     */
    public Authentication getAuthentication(String token)
    {
        Claims claims = jwtParser
            .parseClaimsJws(token)
            .getBody();

        String principal = claims.getSubject();
        Collection<GrantedAuthority> authorities = Arrays
            .stream(claims.get(AUTHORITIES_KEY).toString().split(","))
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());

        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    /**
     * Method for validate token.
     *
     * @param authToken - JWT token
     * @return true | false
     */
    public boolean validateToken(String authToken)
    {
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
    private String generateToken(Authentication authentication, long expirationSeconds, String jti)
    {
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
        return Boolean.TRUE.equals(rememberMe) ? this.tokenValidityInSecondsForRememberMe : this.tokenValidityInSeconds;
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

}
