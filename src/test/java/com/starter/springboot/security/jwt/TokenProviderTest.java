package com.starter.springboot.security.jwt;

import com.starter.springboot.domain.Authority;
import com.starter.springboot.domain.Role;
import com.starter.springboot.domain.User;
import com.starter.springboot.domain.UserStatus;
import com.starter.springboot.repositories.UserRepository;
import com.starter.springboot.security.DomainUserDetails;
import com.starter.springboot.services.dto.OtpGenerationResult;
import com.starter.springboot.services.impl.OtpService;
import com.starter.springboot.services.impl.RedisTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.security.Key;
import java.time.Duration;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenProvider Tests")
class TokenProviderTest {

    @Mock
    private OtpService otpService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisTokenService redisTokenService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenProvider tokenProvider;

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final Long TEST_USER_ID = 1L;
    private static final String VALID_SECRET = "dGhpcyBpcyBhIHZlcnkgc2VjcmV0IGtleSBmb3IgSldUIHRva2VuIHNpZ25pbmcgYW5kIGl0IGlzIGxvbmcgZW5vdWdo"; // Base64 encoded 256-bit key
    private static final String SHORT_SECRET = "c2hvcnQ="; // Base64 encoded "short"
    private static final long TOKEN_VALIDITY = 86400; // 1 day in seconds
    private static final long REMEMBER_ME_VALIDITY = 2592000; // 30 days in seconds

    private User testUser;
    private Role testRole;
    private Authority testAuthority;
    private Authentication testAuthentication;

    @BeforeEach
    void setUp() {
        testUser = createTestUser();
        testRole = createTestRole();
        testAuthority = createTestAuthority();
        testAuthentication = createTestAuthentication();
        
        // Set up valid configuration
        ReflectionTestUtils.setField(tokenProvider, "secretKey", VALID_SECRET);
        ReflectionTestUtils.setField(tokenProvider, "tokenValidityInSeconds", TOKEN_VALIDITY);
        ReflectionTestUtils.setField(tokenProvider, "tokenValidityInSecondsForRememberMe", REMEMBER_ME_VALIDITY);

        // Mock Redis operations
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.get(anyString())).thenReturn(null);
        lenient().doNothing().when(valueOperations).set(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should successfully create token without OTP when OTP not required")
    void shouldSuccessfullyCreateTokenWithoutOtpWhenOtpNotRequired() throws Exception {
        // Given
        testUser.setIsOtpRequired(false);
        doNothing().when(redisTokenService).registerJti(eq(TEST_USER_ID), anyString(), eq(TOKEN_VALIDITY));
        
        // Initialize the TokenProvider
        tokenProvider.afterPropertiesSet();

        // When
        TokenCreationResponse response = tokenProvider.createToken(buildAuthenticationWithDomainUserDetails(), false);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.status());
        assertNotNull(response.token());
        assertFalse(response.otpRequired());
        assertEquals("Bearer", response.token().getTokenType());
        assertEquals(TOKEN_VALIDITY, response.token().getExpiresIn());
        
        verify(userRepository, never()).findByUsername(anyString());
        verify(otpService, never()).generateOtp(anyString(), anyString());
        verify(redisTokenService).registerJti(eq(TEST_USER_ID), anyString(), eq(TOKEN_VALIDITY));
    }

    @Test
    @DisplayName("Should require OTP when user has OTP enabled")
    void shouldRequireOtpWhenUserHasOtpEnabled() throws Exception {
        // Given
        testUser.setIsOtpRequired(true);
        Authentication authenticationWithPrincipal = buildAuthenticationWithDomainUserDetails();
        when(otpService.generateOtp(TEST_USERNAME, TEST_EMAIL)).thenReturn(OtpGenerationResult.success());
        
        // Initialize the TokenProvider
        tokenProvider.afterPropertiesSet();

        // When
        TokenCreationResponse response = tokenProvider.createToken(authenticationWithPrincipal, false);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.ACCEPTED, response.status());
        assertNull(response.token());
        assertTrue(response.otpRequired());
        assertEquals("OTP required to complete authentication.", response.message());
        
        verify(userRepository, never()).findByUsername(anyString());
        verify(otpService).generateOtp(TEST_USERNAME, TEST_EMAIL);
        verify(redisTokenService, never()).registerJti(anyLong(), anyString(), anyLong());
    }

    @Test
    @DisplayName("Should reject token creation when OTP generation fails")
    void shouldRejectTokenCreationWhenOtpGenerationFails() throws Exception {
        // Given
        testUser.setIsOtpRequired(true);
        Authentication authenticationWithPrincipal = buildAuthenticationWithDomainUserDetails();
        when(otpService.generateOtp(TEST_USERNAME, TEST_EMAIL)).thenReturn(OtpGenerationResult.maxAttemptsExceeded());
        
        // Initialize the TokenProvider
        tokenProvider.afterPropertiesSet();

        // When
        TokenCreationResponse response = tokenProvider.createToken(authenticationWithPrincipal, false);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.status());
        assertNull(response.token());
        assertTrue(response.otpRequired());
        assertEquals("Maximum OTP attempts exceeded. Try again later.", response.message());
        
        verify(userRepository, never()).findByUsername(anyString());
        verify(otpService).generateOtp(TEST_USERNAME, TEST_EMAIL);
        verify(redisTokenService, never()).registerJti(anyLong(), anyString(), anyLong());
    }

    @Test
    @DisplayName("Should successfully create token with remember me option")
    void shouldSuccessfullyCreateTokenWithRememberMeOption() throws Exception {
        // Given
        testUser.setIsOtpRequired(false);
        doNothing().when(redisTokenService).registerJti(eq(TEST_USER_ID), anyString(), eq(REMEMBER_ME_VALIDITY));
        
        // Initialize the TokenProvider
        tokenProvider.afterPropertiesSet();

        // When
        TokenCreationResponse response = tokenProvider.createToken(buildAuthenticationWithDomainUserDetails(), true);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.status());
        assertNotNull(response.token());
        assertEquals(REMEMBER_ME_VALIDITY, response.token().getExpiresIn());
        
        verify(userRepository, never()).findByUsername(anyString());
        verify(redisTokenService).registerJti(eq(TEST_USER_ID), anyString(), eq(REMEMBER_ME_VALIDITY));
    }

    @Test
    @DisplayName("Should successfully create token after verified OTP")
    void shouldSuccessfullyCreateTokenAfterVerifiedOtp() throws Exception {
        // Given
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        doNothing().when(redisTokenService).registerJti(eq(TEST_USER_ID), anyString(), eq(TOKEN_VALIDITY));
        
        // Initialize the TokenProvider
        tokenProvider.afterPropertiesSet();

        // When
        JWTToken token = tokenProvider.createTokenAfterVerifiedOtp(TEST_USERNAME, false);

        // Then
        assertNotNull(token);
        assertEquals("Bearer", token.getTokenType());
        assertEquals(TOKEN_VALIDITY, token.getExpiresIn());
        assertNotNull(token.getIdToken());
        
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(redisTokenService).registerJti(eq(TEST_USER_ID), anyString(), eq(TOKEN_VALIDITY));
    }

    @Test
    @DisplayName("Should successfully extract authentication from valid token")
    void shouldSuccessfullyExtractAuthenticationFromValidToken() throws Exception {
        // Given
        tokenProvider.afterPropertiesSet();
        String token = generateValidToken();

        // When
        Authentication authentication = tokenProvider.getAuthentication(token);

        // Then
        assertNotNull(authentication);
        assertEquals(TEST_USERNAME, authentication.getName());
        assertEquals("", authentication.getCredentials());
        
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        assertEquals(1, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    @DisplayName("Should successfully validate token with all checks passing")
    void shouldSuccessfullyValidateTokenWithAllChecksPassing() throws Exception {
        // Given
        tokenProvider.afterPropertiesSet();
        String token = generateValidToken();
        
        // Extract JTI from token for whitelist check
        Claims claims = extractClaimsFromToken(token);
        String jti = claims.getId();
        
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(redisTokenService.isJtiWhitelisted(TEST_USER_ID, jti)).thenReturn(true);

        // When
        boolean isValid = tokenProvider.validateToken(token);

        // Then
        assertTrue(isValid);
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(redisTokenService).isJtiWhitelisted(TEST_USER_ID, jti);
    }

    @Test
    @DisplayName("Should reject token when user not found")
    void shouldRejectTokenWhenUserNotFound() throws Exception {
        // Given
        tokenProvider.afterPropertiesSet();
        String token = generateValidToken();
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        // When
        boolean isValid = tokenProvider.validateToken(token);

        // Then
        assertFalse(isValid);
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(redisTokenService, never()).isJtiWhitelisted(anyLong(), anyString());
    }

    @Test
    @DisplayName("Should reject token when issued before password reset")
    void shouldRejectTokenWhenIssuedBeforePasswordReset() throws Exception {
        // Given
        tokenProvider.afterPropertiesSet();
        String token = generateValidToken();
        
        // Set password reset date after token was issued
        Date futureResetDate = new Date(System.currentTimeMillis() + 60000); // 1 minute in future
        testUser.setLastPasswordResetDate(futureResetDate);
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));

        // When
        boolean isValid = tokenProvider.validateToken(token);

        // Then
        assertFalse(isValid);
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(redisTokenService, never()).isJtiWhitelisted(anyLong(), anyString());
    }

    @Test
    @DisplayName("Should reject token when JTI not whitelisted")
    void shouldRejectTokenWhenJtiNotWhitelisted() throws Exception {
        // Given
        tokenProvider.afterPropertiesSet();
        String token = generateValidToken();
        
        Claims claims = extractClaimsFromToken(token);
        String jti = claims.getId();
        
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(redisTokenService.isJtiWhitelisted(TEST_USER_ID, jti)).thenReturn(false);

        // When
        boolean isValid = tokenProvider.validateToken(token);

        // Then
        assertFalse(isValid);
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(redisTokenService).isJtiWhitelisted(TEST_USER_ID, jti);
    }

    @Test
    @DisplayName("Should handle Redis whitelist check failure gracefully")
    void shouldHandleRedisWhitelistCheckFailureGracefully() throws Exception {
        // Given
        tokenProvider.afterPropertiesSet();
        String token = generateValidToken();
        
        Claims claims = extractClaimsFromToken(token);
        String jti = claims.getId();
        
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(redisTokenService.isJtiWhitelisted(TEST_USER_ID, jti)).thenThrow(new RuntimeException("Redis connection failed"));

        // When
        boolean isValid = tokenProvider.validateToken(token);

        // Then
        assertTrue(isValid); // Should fall back to password reset date check only
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(redisTokenService).isJtiWhitelisted(TEST_USER_ID, jti);
    }

    @Test
    @DisplayName("Should reject malformed JWT token")
    void shouldRejectMalformedJwtToken() throws Exception {
        // Given
        tokenProvider.afterPropertiesSet();
        String malformedToken = "invalid.jwt.token";

        // When
        boolean isValid = tokenProvider.validateToken(malformedToken);

        // Then
        assertFalse(isValid);
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when principal is not DomainUserDetails")
    void shouldThrowIllegalArgumentExceptionWhenPrincipalNotDomainUserDetails() throws Exception {
        // Given
        Authentication authenticationWithStringPrincipal = new UsernamePasswordAuthenticationToken(TEST_USERNAME, TEST_PASSWORD);
        tokenProvider.afterPropertiesSet();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tokenProvider.createToken(authenticationWithStringPrincipal, false));

        assertTrue(exception.getMessage().contains("Authentication principal is not an instance of DomainUserDetails"));
        verify(userRepository, never()).findByUsername(anyString());
        verify(otpService, never()).generateOtp(anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when user not found during OTP token creation")
    void shouldThrowEntityNotFoundExceptionWhenUserNotFoundDuringOtpTokenCreation() throws Exception {
        // Given
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());
        tokenProvider.afterPropertiesSet();

        // When & Then
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> tokenProvider.createTokenAfterVerifiedOtp(TEST_USERNAME, false));

        assertEquals("User not found!", exception.getMessage());
        verify(userRepository).findByUsername(TEST_USERNAME);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when JWT secret is not configured")
    void shouldThrowIllegalStateExceptionWhenJwtSecretNotConfigured() {
        // Given
        ReflectionTestUtils.setField(tokenProvider, "secretKey", null);

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> tokenProvider.afterPropertiesSet());

        assertEquals("JWT secret (`jwt.secret`) is not configured.", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when JWT secret is blank")
    void shouldThrowIllegalStateExceptionWhenJwtSecretIsBlank() {
        // Given
        ReflectionTestUtils.setField(tokenProvider, "secretKey", "   ");

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> tokenProvider.afterPropertiesSet());

        assertEquals("JWT secret (`jwt.secret`) is not configured.", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when JWT secret is too short")
    void shouldThrowIllegalArgumentExceptionWhenJwtSecretTooShort() {
        // Given
        ReflectionTestUtils.setField(tokenProvider, "secretKey", SHORT_SECRET);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tokenProvider.afterPropertiesSet());

        assertEquals("JWT secret is too short. Provide Base64-encoded key of at least 256 bits.", exception.getMessage());
    }

    @Test
    @DisplayName("Should continue token creation when Redis registration fails")
    void shouldContinueTokenCreationWhenRedisRegistrationFails() throws Exception {
        // Given
        testUser.setIsOtpRequired(false);
        doThrow(new RuntimeException("Redis connection failed"))
                .when(redisTokenService).registerJti(eq(TEST_USER_ID), anyString(), eq(TOKEN_VALIDITY));
        
        tokenProvider.afterPropertiesSet();

        // When
        TokenCreationResponse response = tokenProvider.createToken(buildAuthenticationWithDomainUserDetails(), false);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.status());
        assertNotNull(response.token());
        assertFalse(response.otpRequired());
        
        verify(userRepository, never()).findByUsername(anyString());
        verify(redisTokenService).registerJti(eq(TEST_USER_ID), anyString(), eq(TOKEN_VALIDITY));
    }

    // Helper methods
    private User createTestUser() {
        Role role = createTestRole();
        Authority authority = createTestAuthority();
        
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setUsername(TEST_USERNAME);
        user.setEmail(TEST_EMAIL);
        user.setPassword(TEST_PASSWORD);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEnabled(true);
        user.setStatus(UserStatus.ACTIVE);
        user.setIsOtpRequired(false);
        user.setLastPasswordResetDate(new Date(System.currentTimeMillis() - 60000)); // 1 minute ago
        user.setRole(role);
        user.setAuthority(authority);
        return user;
    }

    private Role createTestRole() {
        Role role = new Role();
        role.setId(1L);
        role.setName("ROLE_USER");
        role.setDescription("Default user role");
        return role;
    }

    private Authority createTestAuthority() {
        Authority authority = new Authority();
        authority.setId(1L);
        authority.setName("USER");
        return authority;
    }

    private Authentication createTestAuthentication() {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        DomainUserDetails domainUserDetails = DomainUserDetails.fromUser(testUser, authorities);
        return new UsernamePasswordAuthenticationToken(domainUserDetails, TEST_PASSWORD, authorities);
    }

    private Authentication buildAuthenticationWithDomainUserDetails() {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        DomainUserDetails domainUserDetails = DomainUserDetails.fromUser(testUser, authorities);
        return new UsernamePasswordAuthenticationToken(domainUserDetails, TEST_PASSWORD, authorities);
    }

    private String generateValidToken() throws Exception {
        // Create a key from the valid secret
        byte[] keyBytes = Decoders.BASE64.decode(VALID_SECRET);
        Key key = Keys.hmacShaKeyFor(keyBytes);

        long now = System.currentTimeMillis();
        Date issuedAt = new Date(now);
        Date validity = new Date(now + TOKEN_VALIDITY * 1000);

        return Jwts.builder()
                .setId("test-jti-12345")
                .setSubject(TEST_USERNAME)
                .claim("auth", "ROLE_USER")
                .setIssuedAt(issuedAt)
                .setExpiration(validity)
                .signWith(key)
                .compact();
    }

    private Claims extractClaimsFromToken(String token) throws Exception {
        byte[] keyBytes = Decoders.BASE64.decode(VALID_SECRET);
        Key key = Keys.hmacShaKeyFor(keyBytes);

        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    @Test
    @DisplayName("Should successfully cache user details for OTP")
    void shouldSuccessfullyCacheUserDetailsForOtp() throws Exception {
        // Given
        DomainUserDetails userDetails = DomainUserDetails.fromUser(testUser, List.of(new SimpleGrantedAuthority("ROLE_USER")));

        // Initialize the TokenProvider
        tokenProvider.afterPropertiesSet();

        // When
        tokenProvider.cacheUserDetailsForOtp(TEST_USERNAME, userDetails);

        // Then
        verify(valueOperations).set(
                eq("otp:user:" + TEST_USERNAME),
                anyString(), // JSON value - hard to verify exact content without parsing
                eq(Duration.ofMinutes(5))
        );
    }

    @Test
    @DisplayName("Should handle Redis failure when caching user details for OTP")
    void shouldHandleRedisFailureWhenCachingUserDetailsForOtp() throws Exception {
        // Given
        DomainUserDetails userDetails = DomainUserDetails.fromUser(testUser, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        doThrow(new RuntimeException("Redis connection failed")).when(valueOperations).set(anyString(), anyString(), any());

        // Initialize the TokenProvider
        tokenProvider.afterPropertiesSet();

        // When
        tokenProvider.cacheUserDetailsForOtp(TEST_USERNAME, userDetails);

        // Then
        // Method should not throw, just log warning
        verify(valueOperations).set(
                eq("otp:user:" + TEST_USERNAME),
                anyString(),
                eq(Duration.ofMinutes(5))
        );
    }

    @Test
    @DisplayName("Should successfully retrieve cached user details for OTP")
    void shouldSuccessfullyRetrieveCachedUserDetailsForOtp() throws Exception {
        // Given
        DomainUserDetails originalUserDetails = DomainUserDetails.fromUser(testUser, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        String jsonValue = mapper.writeValueAsString(originalUserDetails);

        when(valueOperations.get("otp:user:" + TEST_USERNAME)).thenReturn(jsonValue);

        // Initialize the TokenProvider with custom ObjectMapper
        ReflectionTestUtils.setField(tokenProvider, "objectMapper", mapper);
        tokenProvider.afterPropertiesSet();

        // When
        DomainUserDetails retrievedUserDetails = tokenProvider.getCachedUserDetailsForOtp(TEST_USERNAME);

        // Then
        assertNotNull(retrievedUserDetails);
        assertEquals(TEST_USERNAME, retrievedUserDetails.getUsername());
        assertEquals(TEST_EMAIL, retrievedUserDetails.getEmail());
        assertEquals(TEST_USER_ID, retrievedUserDetails.getUserId());
        assertTrue(retrievedUserDetails.getAuthorities().stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));
        verify(valueOperations).get("otp:user:" + TEST_USERNAME);
    }

    @Test
    @DisplayName("Should return null when no cached user details found for OTP")
    void shouldReturnNullWhenNoCachedUserDetailsFoundForOtp() throws Exception {
        // Given
        when(valueOperations.get("otp:user:" + TEST_USERNAME)).thenReturn(null);

        // Initialize the TokenProvider
        tokenProvider.afterPropertiesSet();

        // When
        DomainUserDetails retrievedUserDetails = tokenProvider.getCachedUserDetailsForOtp(TEST_USERNAME);

        // Then
        assertNull(retrievedUserDetails);
        verify(valueOperations).get("otp:user:" + TEST_USERNAME);
    }

    @Test
    @DisplayName("Should return null and handle deserialization failure gracefully")
    void shouldReturnNullAndHandleDeserializationFailureGracefully() throws Exception {
        // Given
        String invalidJson = "{invalid json}";

        when(valueOperations.get("otp:user:" + TEST_USERNAME)).thenReturn(invalidJson);

        // Initialize the TokenProvider
        tokenProvider.afterPropertiesSet();

        // When
        DomainUserDetails retrievedUserDetails = tokenProvider.getCachedUserDetailsForOtp(TEST_USERNAME);

        // Then
        assertNull(retrievedUserDetails);
        verify(valueOperations).get("otp:user:" + TEST_USERNAME);
    }
}