package com.starter.springboot.security;

import com.starter.springboot.entity.Role;
import com.starter.springboot.entity.User;
import com.starter.springboot.exception.UserNotActivatedException;
import com.starter.springboot.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OtpAwareAuthenticationProvider Tests")
class OtpAwareAuthenticationProviderTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private OtpAwareAuthenticationProvider authenticationProvider;

    private static final String TEST_USERNAME = "diona.smith";
    private static final String TEST_PASSWORD = "SecurePass123";
    private static final String ENCODED_PASSWORD = "$2a$10$encodedPassword";
    private static final String ROLE_NAME = "ROLE_USER";

    private User testUser;
    private Role testRole;
    private UsernamePasswordAuthenticationToken authenticationToken;

    @BeforeEach
    void setUp() {
        // Create test role
        testRole = new Role();
        testRole.setId(1L);
        testRole.setName(ROLE_NAME);
        testRole.setDescription("Test Role");

        // Create test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername(TEST_USERNAME);
        testUser.setPassword(ENCODED_PASSWORD);
        testUser.setFirstName("Diona");
        testUser.setLastName("Smith");
        testUser.setEmail("diona.smith@example.com");
        testUser.setEnabled(true);
        testUser.setRole(testRole);
        testUser.setIsOtpRequired(true);

        // Create authentication token
        authenticationToken = new UsernamePasswordAuthenticationToken(
                TEST_USERNAME,
                TEST_PASSWORD
        );

        // Mock Redis operations
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.get(anyString())).thenReturn(null);
    }

    @Test
    @DisplayName("Should authenticate user successfully with valid credentials")
    void shouldAuthenticateUserSuccessfullyWithValidCredentials() {
        // Given
        when(userRepository.findByUsername(TEST_USERNAME.toLowerCase()))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD))
                .thenReturn(true);

        // When
        Authentication result = authenticationProvider.authenticate(authenticationToken);

        // Then
        assertNotNull(result);
        assertTrue(result.isAuthenticated());
        assertNotNull(result.getPrincipal());
        assertTrue(result.getPrincipal() instanceof DomainUserDetails);

        DomainUserDetails userDetails = (DomainUserDetails) result.getPrincipal();
        assertEquals(TEST_USERNAME, userDetails.getUsername());
        assertEquals(ENCODED_PASSWORD, userDetails.getPassword());
        assertTrue(userDetails.isOtpRequired());
        assertEquals(1L, userDetails.getUserId());
        assertEquals("diona.smith@example.com", userDetails.getEmail());

        // Verify interactions
        verify(userRepository).findByUsername(TEST_USERNAME.toLowerCase());
        verify(passwordEncoder).matches(TEST_PASSWORD, ENCODED_PASSWORD);
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when user not found")
    void shouldThrowBadCredentialsExceptionWhenUserNotFound() {
        // Given
        when(userRepository.findByUsername(TEST_USERNAME.toLowerCase()))
                .thenReturn(Optional.empty());

        // When & Then
        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authenticationProvider.authenticate(authenticationToken)
        );

        assertEquals("Invalid username or password", exception.getMessage());
        verify(userRepository).findByUsername(TEST_USERNAME.toLowerCase());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw UserNotActivatedException when user account is disabled")
    void shouldThrowUserNotActivatedExceptionWhenUserAccountIsDisabled() {
        // Given
        testUser.setEnabled(false);
        when(userRepository.findByUsername(TEST_USERNAME.toLowerCase()))
                .thenReturn(Optional.of(testUser));

        // When & Then
        UserNotActivatedException exception = assertThrows(
                UserNotActivatedException.class,
                () -> authenticationProvider.authenticate(authenticationToken)
        );

        assertEquals("User account is not activated", exception.getMessage());
        verify(userRepository).findByUsername(TEST_USERNAME.toLowerCase());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw UserNotActivatedException when user enabled is null")
    void shouldThrowUserNotActivatedExceptionWhenUserEnabledIsNull() {
        // Given
        testUser.setEnabled(null);
        when(userRepository.findByUsername(TEST_USERNAME.toLowerCase()))
                .thenReturn(Optional.of(testUser));

        // When & Then
        UserNotActivatedException exception = assertThrows(
                UserNotActivatedException.class,
                () -> authenticationProvider.authenticate(authenticationToken)
        );

        assertEquals("User account is not activated", exception.getMessage());
        verify(userRepository).findByUsername(TEST_USERNAME.toLowerCase());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when password is invalid")
    void shouldThrowBadCredentialsExceptionWhenPasswordIsInvalid() {
        // Given
        when(userRepository.findByUsername(TEST_USERNAME.toLowerCase()))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD))
                .thenReturn(false);

        // When & Then
        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authenticationProvider.authenticate(authenticationToken)
        );

        assertEquals("Invalid username or password", exception.getMessage());
        verify(userRepository).findByUsername(TEST_USERNAME.toLowerCase());
        verify(passwordEncoder).matches(TEST_PASSWORD, ENCODED_PASSWORD);
    }

    @Test
    @DisplayName("Should verify OTP status is preserved in authentication token")
    void shouldVerifyOtpStatusIsPreservedInAuthenticationToken() {
        // Given
        testUser.setIsOtpRequired(true);
        when(userRepository.findByUsername(TEST_USERNAME.toLowerCase()))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD))
                .thenReturn(true);

        // When
        Authentication result = authenticationProvider.authenticate(authenticationToken);

        // Then
        DomainUserDetails userDetails = (DomainUserDetails) result.getPrincipal();
        assertTrue(userDetails.isOtpRequired());
    }

    @Test
    @DisplayName("Should verify OTP status is false when not required")
    void shouldVerifyOtpStatusIsFalseWhenNotRequired() {
        // Given
        testUser.setIsOtpRequired(false);
        when(userRepository.findByUsername(TEST_USERNAME.toLowerCase()))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD))
                .thenReturn(true);

        // When
        Authentication result = authenticationProvider.authenticate(authenticationToken);

        // Then
        DomainUserDetails userDetails = (DomainUserDetails) result.getPrincipal();
        assertFalse(userDetails.isOtpRequired());
    }

    @Test
    @DisplayName("Should return true when supports UsernamePasswordAuthenticationToken")
    void shouldReturnTrueWhenSupportsUsernamePasswordAuthenticationToken() {
        // When
        boolean result = authenticationProvider.supports(UsernamePasswordAuthenticationToken.class);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("Should return false when supports other authentication types")
    void shouldReturnFalseWhenSupportsOtherAuthenticationTypes() {
        // When & Then
        assertFalse(authenticationProvider.supports(String.class));
        assertFalse(authenticationProvider.supports(Integer.class));
        assertFalse(authenticationProvider.supports(Object.class));
    }

    @Test
    @DisplayName("Should convert username to lowercase for lookup")
    void shouldConvertUsernameToLowercaseForLookup() {
        // Given
        String mixedCaseUsername = "TestUser";
        UsernamePasswordAuthenticationToken mixedCaseToken = 
                new UsernamePasswordAuthenticationToken(mixedCaseUsername, TEST_PASSWORD);
        
        when(userRepository.findByUsername(mixedCaseUsername.toLowerCase()))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD))
                .thenReturn(true);

        // When
        Authentication result = authenticationProvider.authenticate(mixedCaseToken);

        // Then
        assertNotNull(result);
        verify(userRepository).findByUsername(mixedCaseUsername.toLowerCase());
    }

    @Test
    @DisplayName("Should include user authorities in authentication result")
    void shouldIncludeUserAuthoritiesInAuthenticationResult() {
        // Given
        when(userRepository.findByUsername(TEST_USERNAME.toLowerCase()))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD))
                .thenReturn(true);

        // When
        Authentication result = authenticationProvider.authenticate(authenticationToken);

        // Then
        assertNotNull(result.getAuthorities());
        assertEquals(1, result.getAuthorities().size());
        
        GrantedAuthority authority = result.getAuthorities().stream()
                .findFirst()
                .orElseThrow();
        assertEquals(ROLE_NAME, authority.getAuthority());
    }

    @Test
    @DisplayName("Should create DomainUserDetails with all user information")
    void shouldCreateDomainUserDetailsWithAllUserInformation() {
        // Given
        String testEmail = "user@example.com";
        testUser.setEmail(testEmail);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setId(42L);
        
        when(userRepository.findByUsername(TEST_USERNAME.toLowerCase()))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD))
                .thenReturn(true);

        // When
        Authentication result = authenticationProvider.authenticate(authenticationToken);

        // Then
        DomainUserDetails userDetails = (DomainUserDetails) result.getPrincipal();
        assertEquals(TEST_USERNAME, userDetails.getUsername());
        assertEquals(testEmail, userDetails.getEmail());
        assertEquals(42L, userDetails.getUserId());
        assertEquals(ROLE_NAME, userDetails.getRoleName());
    }

    @Test
    @DisplayName("Should return authenticated token with DomainUserDetails as principal")
    void shouldReturnAuthenticatedTokenWithDomainUserDetailsAsPrincipal() {
        // Given
        when(userRepository.findByUsername(TEST_USERNAME.toLowerCase()))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD))
                .thenReturn(true);

        // When
        Authentication result = authenticationProvider.authenticate(authenticationToken);

        // Then
        assertTrue(result.isAuthenticated());
        assertInstanceOf(DomainUserDetails.class, result.getPrincipal());
        assertNull(result.getCredentials(),"Credentials should be cleared after authentication");
        assertNotNull(result.getAuthorities());
    }

}