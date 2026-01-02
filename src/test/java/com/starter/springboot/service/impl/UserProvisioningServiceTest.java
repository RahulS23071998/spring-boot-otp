package com.starter.springboot.service.impl;

import com.starter.springboot.constants.SecurityConstants;
import com.starter.springboot.entity.AuthType;
import com.starter.springboot.entity.Authority;
import com.starter.springboot.entity.Role;
import com.starter.springboot.entity.User;
import com.starter.springboot.entity.UserStatus;
import com.starter.springboot.exception.UserAlreadyExistsException;
import com.starter.springboot.repository.AuthorityRepository;
import com.starter.springboot.repository.RoleRepository;
import com.starter.springboot.repository.UserRepository;
import com.starter.springboot.service.LocalizationService;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProvisioningServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AuthorityRepository authorityRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private LocalizationService localizationService;

    @InjectMocks
    private UserProvisioningService userProvisioningService;

    private User testUser;
    private Role testRole;
    private Authority testAuthority;

    @BeforeEach
    void setUp() {
        testRole = new Role();
        testRole.setId(1L);
        testRole.setName(SecurityConstants.ROLE_PREFIX + "USER");

        testAuthority = new Authority();
        testAuthority.setId(1L);
        testAuthority.setName("USER");

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword("password");
        testUser.setEmail("test@example.com");
    }

    @Test
    @DisplayName("Should successfully create a new user")
    void shouldSuccessfullyCreateUser() {
        // Given
        when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.empty());
        when(roleRepository.findByName(SecurityConstants.USER_AUTHORITY)).thenReturn(Optional.of(testRole));
        when(authorityRepository.findByName("USER")).thenReturn(Optional.of(testAuthority));
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User createdUser = userProvisioningService.createUser(testUser);

        // Then
        assertNotNull(createdUser);
        assertEquals(UserStatus.ACTIVE, createdUser.getStatus());
        assertTrue(createdUser.getEnabled());
        assertEquals(AuthType.WEB_SIGNUP, createdUser.getAuthType());
        assertTrue(createdUser.getIsOtpRequired());
        assertTrue(createdUser.getPasswordSet());
        assertEquals("encodedPassword", createdUser.getPassword());
        assertEquals(testRole, createdUser.getRole());
        assertEquals(testAuthority, createdUser.getAuthority());
        assertNotNull(createdUser.getLastPasswordResetDate());

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw UserAlreadyExistsException when username already exists")
    void shouldThrowUserAlreadyExistsExceptionWhenUsernameExists() {
        // Given
        when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.of(new User()));
        when(localizationService.getMessage(eq("user.already_exists"), anyString())).thenReturn("User already exists");

        // When & Then
        assertThrows(UserAlreadyExistsException.class, () -> userProvisioningService.createUser(testUser));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when default role not found")
    void shouldThrowEntityNotFoundExceptionWhenDefaultRoleNotFound() {
        // Given
        when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.empty());
        when(roleRepository.findByName(SecurityConstants.USER_AUTHORITY)).thenReturn(Optional.empty());
        when(localizationService.getMessage("user.default_role_not_configured")).thenReturn("Default role not configured");

        // When & Then
        assertThrows(EntityNotFoundException.class, () -> userProvisioningService.createUser(testUser));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should return existing Google OAuth user")
    void shouldReturnExistingGoogleOAuthUser() {
        // Given
        Map<String, Object> googleInfo = new HashMap<>();
        googleInfo.put("sub", "google123");
        googleInfo.put("email", "test@example.com");

        User existingUser = new User();
        existingUser.setGoogleId("google123");
        existingUser.setEmail("test@example.com");

        when(userRepository.findByGoogleId("google123")).thenReturn(Optional.of(existingUser));

        // When
        User result = userProvisioningService.findOrCreateGoogleOAuthUser(googleInfo);

        // Then
        assertEquals(existingUser, result);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should link existing user to Google OAuth")
    void shouldLinkExistingUserToGoogleOAuth() {
        // Given
        Map<String, Object> googleInfo = new HashMap<>();
        googleInfo.put("sub", "google123");
        googleInfo.put("email", "test@example.com");
        googleInfo.put("email_verified", true);

        User existingUser = new User();
        existingUser.setUsername("test@example.com");
        existingUser.setEmail("test@example.com");

        when(userRepository.findByGoogleId("google123")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = userProvisioningService.findOrCreateGoogleOAuthUser(googleInfo);

        // Then
        assertEquals("google123", result.getGoogleId());
        assertEquals(AuthType.GOOGLE_OAUTH, result.getAuthType());
        assertTrue(result.getEmailVerified());
        verify(userRepository).save(existingUser);
    }

    @Test
    @DisplayName("Should create new Google OAuth user")
    void shouldCreateNewGoogleOAuthUser() {
        // Given
        Map<String, Object> googleInfo = new HashMap<>();
        googleInfo.put("sub", "google123");
        googleInfo.put("email", "new@example.com");
        googleInfo.put("given_name", "John");
        googleInfo.put("family_name", "DoeDoe");
        googleInfo.put("email_verified", true);

        when(userRepository.findByGoogleId("google123")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("new@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName(SecurityConstants.USER_AUTHORITY)).thenReturn(Optional.of(testRole));
        when(authorityRepository.findByName("USER")).thenReturn(Optional.of(testAuthority));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedRandomPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = userProvisioningService.findOrCreateGoogleOAuthUser(googleInfo);

        // Then
        assertNotNull(result);
        assertEquals("new@example.com", result.getEmail());
        assertEquals("John", result.getFirstName());
        assertEquals("DoeDoe", result.getLastName());
        assertEquals("google123", result.getGoogleId());
        assertEquals(AuthType.GOOGLE_OAUTH, result.getAuthType());
        assertTrue(result.getEmailVerified());
        assertFalse(result.getIsOtpRequired());
        assertFalse(result.getPasswordSet());
        assertEquals(testRole, result.getRole());
        assertEquals(testAuthority, result.getAuthority());

        verify(userRepository).save(any(User.class));
    }
}
