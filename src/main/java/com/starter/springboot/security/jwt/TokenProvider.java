package com.starter.springboot.security.jwt;

import com.starter.springboot.domain.User;
import com.starter.springboot.repositories.UserRepository;
import com.starter.springboot.services.OtpService;
import io.jsonwebtoken.Claims;
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
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("JWT secret (`jwt.secret`) is not configured.");
        }
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret is too short. Provide Base64-encoded key of at least 256 bits.");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    private final OtpService otpService;

    private final UserRepository userRepository;

    public TokenProvider(OtpService otpService, UserRepository userRepository) {
        this.otpService = otpService;
        this.userRepository = userRepository;
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
        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User with username " + username + " not found!"));

        if (Boolean.TRUE.equals(user.getIsOtpRequired())) {
            boolean otpIssued = otpService.generateOtp(user.getUsername());
            if (!otpIssued) {
                return TokenCreationResponse.rejected("Maximum OTP attempts exceeded. Try again later.");
            }
            return TokenCreationResponse.pendingOtp("OTP required to complete authentication.");
        }

        JWTToken token = JWTToken.bearerToken(generateToken(authentication, rememberMe), resolveExpiration(rememberMe));
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
            .orElseThrow(() -> new EntityNotFoundException("User not found!"));

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(user.getRole().getName()));

        Authentication authentication = new UsernamePasswordAuthenticationToken(
            user.getUsername(), user.getPassword(), authorities
        );

        String tokenValue = generateToken(authentication, rememberMe);
        return JWTToken.bearerToken(tokenValue, resolveExpiration(rememberMe));
    }

    /**
     * Method for getting authentication context.
     *
     * @param token provided token
     * @return Authentication Object
     */
    public Authentication getAuthentication(String token)
    {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody();

        String principal = claims.getSubject();
        Collection<? extends GrantedAuthority> authorities = Arrays
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
            Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(authToken).getBody();
            String username = claims.getSubject();
            Date issuedAt = claims.getIssuedAt();
            if (username == null || issuedAt == null) {
                log.warn("JWT missing subject or issuedAt");
                return false;
            }

            // Checking if user's password was reset after token was issued
            return userRepository.findByUsername(username)
                .map(user -> {
                    Date lastReset = user.getLastPasswordResetDate();
                    if (lastReset != null) {
                        if (issuedAt.compareTo(lastReset) <= 0) {
                            log.info("Rejecting JWT for user {}: issuedAt={} <= lastPasswordResetDate={}", username, issuedAt, lastReset);
                            return false;
                        }
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
     * @param rememberMe remember me indicator
     * @return String value of jwt token
     */
    private String generateToken(Authentication authentication, Boolean rememberMe)
    {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = new Date().getTime();
        Date issuedAt = new Date(now);
        Date validity = new Date(now + resolveExpiration(rememberMe) * 1000);

        return Jwts.builder()
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
}
