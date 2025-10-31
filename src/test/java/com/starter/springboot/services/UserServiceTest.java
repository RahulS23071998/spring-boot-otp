package com.starter.springboot.services;

import com.starter.springboot.domain.Authority;
import com.starter.springboot.domain.Role;
import com.starter.springboot.domain.User;
import com.starter.springboot.domain.UserStatus;
import com.starter.springboot.repositories.AuthorityRepository;
import com.starter.springboot.repositories.RoleRepository;
import com.starter.springboot.repositories.UserRepository;
import com.starter.springboot.services.impl.RedisTokenService;
import com.starter.springboot.services.impl.UserService;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AuthorityRepository authorityRepository;

    @Mock
    private RedisTokenService redisTokenService;

    @InjectMocks
    private UserService userService;

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String ENCODED_PASSWORD = "encodedPassword123";
    private static final Long TEST_USER_ID = 1L;
    
    private User testUser;
    private Role testRole;
    private Authority testAuthority;

    @BeforeEach
    void setUp() {
        testUser = createTestUser();
        testRole = createTestRole();
        testAuthority = createTestAuthority();
    }

    @Test
    @DisplayName("Should successfully find all users")
    void shouldSuccessfullyFindAllUsers() {
        // Given
        List<User> expectedUsers = Arrays.asList(testUser, createAnotherTestUser());
        when(userRepository.findAll()).thenReturn(expectedUsers);

        // When
        List<User> actualUsers = userService.findAllUsers();

        // Then
        assertEquals(expectedUsers, actualUsers);
        assertEquals(2, actualUsers.size());
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("Should successfully find email by username")
    void shouldSuccessfullyFindEmailByUsername() {
        // Given
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));

        // When
        String email = userService.findEmailByUsername(TEST_USERNAME);

        // Then
        assertEquals(TEST_EMAIL, email);
        verify(userRepository).findByUsername(TEST_USERNAME);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when user not found by username")
    void shouldThrowEntityNotFoundExceptionWhenUserNotFoundByUsername() {
        // Given
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        // When & Then
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> userService.findEmailByUsername(TEST_USERNAME));

        assertEquals("User with username " + TEST_USERNAME + " not found", exception.getMessage());
        verify(userRepository).findByUsername(TEST_USERNAME);
    }

    @Test
    @DisplayName("Should successfully create user with defaults")
    void shouldSuccessfullyCreateUserWithDefaults() {
        // Given
        User newUser = createNewUserForCreation();
        when(userRepository.findByUsername(newUser.getUsername())).thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(testRole));
        when(authorityRepository.findByName("USER")).thenReturn(Optional.of(testAuthority));
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // When
        User createdUser = userService.createUser(newUser);

        // Then
        assertNotNull(createdUser);
        assertEquals(UserStatus.ACTIVE, createdUser.getStatus());
        assertEquals(Boolean.TRUE, createdUser.getEnabled());
        assertEquals(testRole, createdUser.getRole());
        assertEquals(testAuthority, createdUser.getAuthority());
        assertNotNull(createdUser.getLastPasswordResetDate());
        
        verify(userRepository).findByUsername(newUser.getUsername());
        verify(roleRepository).findByName("ROLE_USER");
        verify(authorityRepository).findByName("USER");
        verify(passwordEncoder).encode(TEST_PASSWORD);
        
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals(ENCODED_PASSWORD, savedUser.getPassword());
    }

    @Test
    @DisplayName("Should throw EntityExistsException when creating user with existing username")
    void shouldThrowEntityExistsExceptionWhenCreatingUserWithExistingUsername() {
        // Given
        User newUser = createNewUserForCreation();
        when(userRepository.findByUsername(newUser.getUsername())).thenReturn(Optional.of(testUser));

        // When & Then
        EntityExistsException exception = assertThrows(EntityExistsException.class,
                () -> userService.createUser(newUser));

        assertEquals("User with username " + newUser.getUsername() + " already exists", exception.getMessage());
        verify(userRepository).findByUsername(newUser.getUsername());
        verify(roleRepository, never()).findByName(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when default role not found during user creation")
    void shouldThrowEntityNotFoundExceptionWhenDefaultRoleNotFoundDuringUserCreation() {
        // Given
        User newUser = createNewUserForCreation();
        when(userRepository.findByUsername(newUser.getUsername())).thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());

        // When & Then
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> userService.createUser(newUser));

        assertEquals("Default role ROLE_USER not configured", exception.getMessage());
        verify(userRepository).findByUsername(newUser.getUsername());
        verify(roleRepository).findByName("ROLE_USER");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should successfully update user status")
    void shouldSuccessfullyUpdateUserStatus() {
        // Given
        UserStatus newStatus = UserStatus.INACTIVE;
        Boolean newEnabled = false;
        
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User updatedUser = userService.updateStatus(TEST_USER_ID, newStatus, newEnabled);

        // Then
        assertNotNull(updatedUser);
        verify(userRepository).findById(TEST_USER_ID);
        
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals(newStatus, savedUser.getStatus());
        assertEquals(newEnabled, savedUser.getEnabled());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when both status and enabled are null")
    void shouldThrowIllegalArgumentExceptionWhenBothStatusAndEnabledAreNull() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.updateStatus(TEST_USER_ID, null, null));

        assertEquals("Either status or enabled must be provided", exception.getMessage());
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when updating status for non-existing user")
    void shouldThrowEntityNotFoundExceptionWhenUpdatingStatusForNonExistingUser() {
        // Given
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

        // When & Then
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> userService.updateStatus(TEST_USER_ID, UserStatus.ACTIVE, true));

        assertEquals("User with id " + TEST_USER_ID + " not found", exception.getMessage());
        verify(userRepository).findById(TEST_USER_ID);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should successfully change password by ID")
    void shouldSuccessfullyChangePasswordById() {
        // Given
        Map<String, String> payload = createPasswordChangePayload();
        testUser.setPassword(ENCODED_PASSWORD);
        
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User updatedUser = userService.changePasswordById(TEST_USER_ID, payload);

        // Then
        assertNotNull(updatedUser);
        verify(userRepository).findById(TEST_USER_ID);
        verify(passwordEncoder).matches(TEST_PASSWORD, ENCODED_PASSWORD);
        verify(passwordEncoder).encode("newPassword123");
        verify(redisTokenService).removeWhitelist(TEST_USER_ID);
        
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("encodedNewPassword", savedUser.getPassword());
        assertNotNull(savedUser.getLastPasswordResetDate());
    }

    @Test
    @DisplayName("Should successfully change password by username")
    void shouldSuccessfullyChangePasswordByUsername() {
        // Given
        Map<String, String> payload = createPasswordChangePayload();
        testUser.setPassword(ENCODED_PASSWORD);
        
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User updatedUser = userService.changePasswordByUsername(TEST_USERNAME, payload);

        // Then
        assertNotNull(updatedUser);
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(passwordEncoder).matches(TEST_PASSWORD, ENCODED_PASSWORD);
        verify(passwordEncoder).encode("newPassword123");
        verify(redisTokenService).removeWhitelist(TEST_USER_ID);
        
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("encodedNewPassword", savedUser.getPassword());
        assertNotNull(savedUser.getLastPasswordResetDate());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when changing password for non-existing user by ID")
    void shouldThrowEntityNotFoundExceptionWhenChangingPasswordForNonExistingUserById() {
        // Given
        Map<String, String> payload = createPasswordChangePayload();
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

        // When & Then
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> userService.changePasswordById(TEST_USER_ID, payload));

        assertEquals("User with id " + TEST_USER_ID + " not found", exception.getMessage());
        verify(userRepository).findById(TEST_USER_ID);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when changing password for non-existing user by username")
    void shouldThrowEntityNotFoundExceptionWhenChangingPasswordForNonExistingUserByUsername() {
        // Given
        Map<String, String> payload = createPasswordChangePayload();
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.empty());

        // When & Then
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> userService.changePasswordByUsername(TEST_USERNAME, payload));

        assertEquals("User with username " + TEST_USERNAME + " not found", exception.getMessage());
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when new password is null")
    void shouldThrowIllegalArgumentExceptionWhenNewPasswordIsNull() {
        // Given
        Map<String, String> payload = new HashMap<>();
        payload.put("oldpassword", TEST_PASSWORD);
        payload.put("newpassword", null);
        payload.put("confirmnewpassword", "newPassword123");
        
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.changePasswordById(TEST_USER_ID, payload));

        assertEquals("New password and confirm new password must be provided", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when passwords do not match")
    void shouldThrowIllegalArgumentExceptionWhenPasswordsDoNotMatch() {
        // Given
        Map<String, String> payload = new HashMap<>();
        payload.put("oldpassword", TEST_PASSWORD);
        payload.put("newpassword", "newPassword123");
        payload.put("confirmnewpassword", "differentPassword");
        
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.changePasswordById(TEST_USER_ID, payload));

        assertEquals("New password and confirm new password do not match", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when old password is incorrect")
    void shouldThrowBadCredentialsExceptionWhenOldPasswordIsIncorrect() {
        // Given
        Map<String, String> payload = createPasswordChangePayload();
        testUser.setPassword(ENCODED_PASSWORD);
        
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(false);

        // When & Then
        BadCredentialsException exception = assertThrows(BadCredentialsException.class,
                () -> userService.changePasswordById(TEST_USER_ID, payload));

        assertEquals("Old password is incorrect", exception.getMessage());
        verify(passwordEncoder).matches(TEST_PASSWORD, ENCODED_PASSWORD);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should handle token whitelist removal failure gracefully")
    void shouldHandleTokenWhitelistRemovalFailureGracefully() {
        // Given
        Map<String, String> payload = createPasswordChangePayload();
        testUser.setPassword(ENCODED_PASSWORD);
        
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        doThrow(new RuntimeException("Redis connection failed")).when(redisTokenService).removeWhitelist(TEST_USER_ID);

        // When
        User updatedUser = userService.changePasswordById(TEST_USER_ID, payload);

        // Then
        assertNotNull(updatedUser);
        verify(redisTokenService).removeWhitelist(TEST_USER_ID);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should create user with existing role and status")
    void shouldCreateUserWithExistingRoleAndStatus() {
        // Given
        User newUser = createNewUserForCreation();
        newUser.setRole(testRole);
        newUser.setStatus(UserStatus.INACTIVE);
        newUser.setEnabled(false);
        
        when(userRepository.findByUsername(newUser.getUsername())).thenReturn(Optional.empty());
        when(authorityRepository.findByName("USER")).thenReturn(Optional.of(testAuthority));
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // When
        User createdUser = userService.createUser(newUser);

        // Then
        assertNotNull(createdUser);
        assertEquals(UserStatus.INACTIVE, createdUser.getStatus());
        assertEquals(false, createdUser.getEnabled());
        assertEquals(testRole, createdUser.getRole());
        assertEquals(testAuthority, createdUser.getAuthority());
        
        verify(roleRepository, never()).findByName("ROLE_USER");
        verify(authorityRepository).findByName("USER");
    }

    // Helper methods
    private User createTestUser() {
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setUsername(TEST_USERNAME);
        user.setEmail(TEST_EMAIL);
        user.setPassword(ENCODED_PASSWORD);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEnabled(true);
        user.setStatus(UserStatus.ACTIVE);
        user.setLastPasswordResetDate(Date.from(Instant.now()));
        user.setRole(testRole);
        user.setAuthority(testAuthority);
        return user;
    }

    private User createAnotherTestUser() {
        User user = new User();
        user.setId(2L);
        user.setUsername("anotheruser");
        user.setEmail("another@example.com");
        user.setPassword("encodedPassword456");
        user.setFirstName("Another");
        user.setLastName("User");
        user.setEnabled(true);
        user.setStatus(UserStatus.ACTIVE);
        user.setLastPasswordResetDate(Date.from(Instant.now()));
        return user;
    }

    private User createNewUserForCreation() {
        User user = new User();
        user.setUsername("newuser");
        user.setEmail("newuser@example.com");
        user.setPassword(TEST_PASSWORD);
        user.setFirstName("New");
        user.setLastName("User");
        return user;
    }

    private Role createTestRole() {
        Role role = new Role();
        role.setId(1L);
        role.setName("ROLE_USER");
        role.setDescription("User role");
        return role;
    }

    private Authority createTestAuthority() {
        Authority authority = new Authority();
        authority.setId(1L);
        authority.setName("USER");
        return authority;
    }

    private Map<String, String> createPasswordChangePayload() {
        Map<String, String> payload = new HashMap<>();
        payload.put("oldpassword", TEST_PASSWORD);
        payload.put("newpassword", "newPassword123");
        payload.put("confirmnewpassword", "newPassword123");
        return payload;
    }
}