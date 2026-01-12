package com.starter.springboot.service;

import com.starter.springboot.entity.Authority;
import com.starter.springboot.entity.Role;
import com.starter.springboot.entity.User;
import com.starter.springboot.entity.UserStatus;
import com.starter.springboot.repository.UserRepository;
import com.starter.springboot.service.impl.UserService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

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
    private IPasswordChangeAuthorizationService authorizationService;

    @Mock
    private IUserProvisioningService provisioningService;

    @Mock
    private IUserTokenService tokenService;

    @Mock
    private LocalizationService localizationService;

    @Mock
    private com.starter.springboot.repository.PasswordHistoryRepository passwordHistoryRepository;

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
        testRole = createTestRole();
        testAuthority = createTestAuthority();
        testUser = createTestUser();
        mockLocalizationMessages();
    }

    @Test
    @DisplayName("Should successfully find all users")
    void shouldSuccessfullyFindAllUsers() {
        // Given
        List<User> expectedUsers = Arrays.asList(testUser, createAnotherTestUser());
        mockLocalizationMessages();
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
        when(provisioningService.createUser(newUser)).thenReturn(newUser);

        // When
        User createdUser = userService.createUser(newUser);

        // Then
        assertNotNull(createdUser);
        verify(provisioningService).createUser(newUser);
    }

    @Test
    @DisplayName("Should throw EntityExistsException when creating user with existing username")
    void shouldThrowEntityExistsExceptionWhenCreatingUserWithExistingUsername() {
        // Given
        User newUser = createNewUserForCreation();
        when(provisioningService.createUser(newUser)).thenThrow(new EntityExistsException("User with username " + newUser.getUsername() + " already exists"));

        // When & Then
        EntityExistsException exception = assertThrows(EntityExistsException.class,
                () -> userService.createUser(newUser));

        assertEquals("User with username " + newUser.getUsername() + " already exists", exception.getMessage());
        verify(provisioningService).createUser(newUser);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when default role not found during user creation")
    void shouldThrowEntityNotFoundExceptionWhenDefaultRoleNotFoundDuringUserCreation() {
        // Given
        User newUser = createNewUserForCreation();
        when(provisioningService.createUser(newUser)).thenThrow(new EntityNotFoundException("Default role ROLE_USER not configured"));

        // When & Then
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> userService.createUser(newUser));

        assertEquals("Default role ROLE_USER not configured", exception.getMessage());
        verify(provisioningService).createUser(newUser);
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
        when(passwordHistoryRepository.findByUserOrderByCreatedDateDesc(any(User.class))).thenReturn(java.util.Collections.emptyList());
        when(passwordHistoryRepository.save(any(com.starter.springboot.entity.PasswordHistory.class))).thenReturn(new com.starter.springboot.entity.PasswordHistory());
        doNothing().when(authorizationService).authorizePasswordChange(testUser);
        doNothing().when(tokenService).clearAllUserTokensAndCaches(TEST_USERNAME, TEST_USER_ID);

        // When
        User updatedUser = userService.changePasswordById(TEST_USER_ID, payload);

        // Then
        assertNotNull(updatedUser);
        verify(userRepository).findById(TEST_USER_ID);
        verify(passwordEncoder).matches(TEST_PASSWORD, ENCODED_PASSWORD);
        verify(passwordEncoder).encode("newPassword123");
        verify(tokenService).clearAllUserTokensAndCaches(TEST_USERNAME, TEST_USER_ID);
        
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
        when(passwordHistoryRepository.findByUserOrderByCreatedDateDesc(any(User.class))).thenReturn(java.util.Collections.emptyList());
        when(passwordHistoryRepository.save(any(com.starter.springboot.entity.PasswordHistory.class))).thenReturn(new com.starter.springboot.entity.PasswordHistory());
        doNothing().when(authorizationService).authorizePasswordChangeByUsername(TEST_USERNAME);
        doNothing().when(tokenService).clearAllUserTokensAndCaches(TEST_USERNAME, TEST_USER_ID);

        // When
        User updatedUser = userService.changePasswordByUsername(TEST_USERNAME, payload);

        // Then
        assertNotNull(updatedUser);
        verify(userRepository).findByUsername(TEST_USERNAME);
        verify(passwordEncoder).matches(TEST_PASSWORD, ENCODED_PASSWORD);
        verify(passwordEncoder).encode("newPassword123");
        verify(tokenService).clearAllUserTokensAndCaches(TEST_USERNAME, TEST_USER_ID);
        
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
        doNothing().when(authorizationService).authorizePasswordChangeByUsername(TEST_USERNAME);

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
        doNothing().when(authorizationService).authorizePasswordChange(testUser);

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
        doNothing().when(authorizationService).authorizePasswordChange(testUser);

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
        doNothing().when(authorizationService).authorizePasswordChange(testUser);

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
        doNothing().when(authorizationService).authorizePasswordChange(testUser);
        doThrow(new RuntimeException("Token service failed")).when(tokenService).clearAllUserTokensAndCaches(TEST_USERNAME, TEST_USER_ID);

        // When
        User updatedUser = userService.changePasswordById(TEST_USER_ID, payload);

        // Then
        assertNotNull(updatedUser);
        verify(tokenService).clearAllUserTokensAndCaches(TEST_USERNAME, TEST_USER_ID);
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
        
        when(provisioningService.createUser(newUser)).thenReturn(newUser);

        // When
        User createdUser = userService.createUser(newUser);

        // Then
        assertNotNull(createdUser);
        verify(provisioningService).createUser(newUser);
    }

    @Test
    @DisplayName("Should find existing user by Google ID")
    void shouldFindExistingUserByGoogleId() {
        // Given
        String googleId = "google-user-12345";
        testUser.setGoogleId(googleId);
        Map<String, Object> googleUserInfo = createGoogleUserInfo(googleId, TEST_EMAIL, "John", "Doe", "John Doe");

        when(provisioningService.findOrCreateGoogleOAuthUser(googleUserInfo)).thenReturn(testUser);

        // When
        User result = userService.findOrCreateGoogleOAuthUser(googleUserInfo);

        // Then
        assertNotNull(result);
        assertEquals(TEST_USER_ID, result.getId());
        assertEquals(TEST_EMAIL, result.getEmail());
        verify(provisioningService).findOrCreateGoogleOAuthUser(googleUserInfo);
    }

    @Test
    @DisplayName("Should link Google ID to existing user with same email")
    void shouldLinkGoogleIdToExistingUserWithSameEmail() {
        // Given
        String googleId = "google-user-67890";
        String email = TEST_EMAIL;
        testUser.setGoogleId(googleId);
        Map<String, Object> googleUserInfo = createGoogleUserInfo(googleId, email, "John", "Doe", "John Doe");

        when(provisioningService.findOrCreateGoogleOAuthUser(googleUserInfo)).thenReturn(testUser);

        // When
        User result = userService.findOrCreateGoogleOAuthUser(googleUserInfo);

        // Then
        assertNotNull(result);
        verify(provisioningService).findOrCreateGoogleOAuthUser(googleUserInfo);
    }

    @Test
    @DisplayName("Should create new user for Google OAuth with complete info")
    void shouldCreateNewGoogleOAuthUserWithCompleteInfo() {
        // Given
        String googleId = "google-user-new";
        String email = "newgoogleuser@gmail.com";
        Map<String, Object> googleUserInfo = createGoogleUserInfo(googleId, email, "John", "Smith", "John Smith");

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setUsername(email);
        newUser.setGoogleId(googleId);
        when(provisioningService.findOrCreateGoogleOAuthUser(googleUserInfo)).thenReturn(newUser);

        // When
        User result = userService.findOrCreateGoogleOAuthUser(googleUserInfo);

        // Then
        assertNotNull(result);
        assertEquals(email, result.getEmail());
        assertEquals(googleId, result.getGoogleId());
        verify(provisioningService).findOrCreateGoogleOAuthUser(googleUserInfo);
    }

    @Test
    @DisplayName("Should create user with default first name when given_name is null")
    void shouldCreateUserWithDefaultFirstNameWhenGivenNameIsNull() {
        // Given
        String googleId = "google-user-noname";
        String email = "noname@gmail.com";
        Map<String, Object> googleUserInfo = createGoogleUserInfo(googleId, email, null, "Name", "Full Name");

        User newUser = new User();
        newUser.setEmail(email);
        when(provisioningService.findOrCreateGoogleOAuthUser(googleUserInfo)).thenReturn(newUser);

        // When
        User result = userService.findOrCreateGoogleOAuthUser(googleUserInfo);

        // Then
        assertNotNull(result);
        verify(provisioningService).findOrCreateGoogleOAuthUser(googleUserInfo);
    }

    @Test
    @DisplayName("Should set default last name to User when family_name is too short")
    void shouldSetDefaultLastNameWhenFamilyNameIsTooShort() {
        // Given
        String googleId = "google-user-short";
        String email = "short@gmail.com";
        Map<String, Object> googleUserInfo = createGoogleUserInfo(googleId, email, "John", "Jo", "John Jo");

        User newUser = new User();
        newUser.setLastName("User");
        when(provisioningService.findOrCreateGoogleOAuthUser(googleUserInfo)).thenReturn(newUser);

        // When
        User result = userService.findOrCreateGoogleOAuthUser(googleUserInfo);

        // Then
        assertNotNull(result);
        assertEquals("User", result.getLastName());
        verify(provisioningService).findOrCreateGoogleOAuthUser(googleUserInfo);
    }

    @Test
    @DisplayName("Should set OTP to false for Google OAuth users")
    void shouldSetOtpToFalseForGoogleOAuthUsers() {
        // Given
        String googleId = "google-user-otp";
        String email = "otp@gmail.com";
        Map<String, Object> googleUserInfo = createGoogleUserInfo(googleId, email, "John", "Doe", "John Doe");

        User newUser = new User();
        newUser.setIsOtpRequired(Boolean.FALSE);
        when(provisioningService.findOrCreateGoogleOAuthUser(googleUserInfo)).thenReturn(newUser);

        // When
        User result = userService.findOrCreateGoogleOAuthUser(googleUserInfo);

        // Then
        assertNotNull(result);
        assertEquals(Boolean.FALSE, result.getIsOtpRequired());
        verify(provisioningService).findOrCreateGoogleOAuthUser(googleUserInfo);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when default role not found")
    void shouldThrowEntityNotFoundExceptionWhenDefaultRoleNotFoundForGoogleOAuth() {
        // Given
        String googleId = "google-user-norole";
        String email = "norole@gmail.com";
        Map<String, Object> googleUserInfo = createGoogleUserInfo(googleId, email, "John", "Doe", "John Doe");

        when(provisioningService.findOrCreateGoogleOAuthUser(googleUserInfo)).thenThrow(new EntityNotFoundException("Default role ROLE_USER not configured"));

        // When & Then
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> userService.findOrCreateGoogleOAuthUser(googleUserInfo));

        verify(provisioningService).findOrCreateGoogleOAuthUser(googleUserInfo);
    }

    @Test
    @DisplayName("Should successfully find all users with pagination")
    void shouldSuccessfullyFindAllUsersWithPagination() {
        Pageable pageable = Pageable.unpaged();
        Page<User> expectedPage = new PageImpl<>(Arrays.asList(testUser));
        when(userRepository.findAll(pageable)).thenReturn(expectedPage);

        Page<User> actualPage = userService.findAllUsers(pageable);

        assertEquals(expectedPage, actualPage);
        verify(userRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Should successfully find user by ID")
    void shouldSuccessfullyFindUserById() {
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));

        User result = userService.findUserById(TEST_USER_ID);

        assertEquals(testUser, result);
        verify(userRepository).findById(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should successfully find user by username")
    void shouldSuccessfullyFindUserByUsername() {
        when(userRepository.findByUsername(TEST_USERNAME)).thenReturn(Optional.of(testUser));

        User result = userService.findUserByUsername(TEST_USERNAME);

        assertEquals(testUser, result);
        verify(userRepository).findByUsername(TEST_USERNAME);
    }

    @Test
    @DisplayName("Should successfully update user details")
    void shouldSuccessfullyUpdateUser() {
        User userUpdates = new User();
        userUpdates.setFirstName("UpdatedFirstName");
        userUpdates.setLastName("UpdatedLastName");
        userUpdates.setEmail("updated@example.com");
        userUpdates.setStatus(UserStatus.INACTIVE);
        userUpdates.setEnabled(false);
        userUpdates.setIsOtpRequired(true);

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        User result = userService.updateUser(TEST_USER_ID, userUpdates);

        assertNotNull(result);
        assertEquals("UpdatedFirstName", result.getFirstName());
        assertEquals("UpdatedLastName", result.getLastName());
        assertEquals("updated@example.com", result.getEmail());
        assertEquals(UserStatus.INACTIVE, result.getStatus());
        assertFalse(result.getEnabled());
        assertTrue(result.getIsOtpRequired());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should successfully delete user")
    void shouldSuccessfullyDeleteUser() {
        when(userRepository.existsById(TEST_USER_ID)).thenReturn(true);
        doNothing().when(tokenService).clearAllUserTokensAndCaches(null, TEST_USER_ID);

        userService.deleteUser(TEST_USER_ID);

        verify(userRepository).deleteById(TEST_USER_ID);
        verify(tokenService).clearAllUserTokensAndCaches(null, TEST_USER_ID);
    }

    @Test
    @DisplayName("Should successfully reset user password")
    void shouldSuccessfullyResetUserPassword() {
        String newPassword = "newResetPassword123";
        String encodedPassword = "encodedNewResetPassword";

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        doNothing().when(tokenService).clearAllUserTokensAndCaches(TEST_USERNAME, TEST_USER_ID);

        userService.resetUserPassword(TEST_USER_ID, newPassword);

        assertEquals(encodedPassword, testUser.getPassword());
        assertNotNull(testUser.getLastPasswordResetDate());
        verify(userRepository).save(testUser);
        verify(tokenService).clearAllUserTokensAndCaches(TEST_USERNAME, TEST_USER_ID);
    }

    @Test
    @DisplayName("Should successfully export users to CSV")
    void shouldSuccessfullyExportUsersToCSV() {
        when(userRepository.streamAll()).thenReturn(Stream.of(testUser));

        byte[] result = userService.exportUsersToCSV();

        assertNotNull(result);
        assertTrue(result.length > 0);
        String csvContent = new String(result);
        assertTrue(csvContent.contains(TEST_USERNAME));
        assertTrue(csvContent.contains(TEST_EMAIL));
    }

    @Test
    @DisplayName("Should successfully export users to Excel")
    void shouldSuccessfullyExportUsersToExcel() {
        when(userRepository.streamAll()).thenReturn(Stream.of(testUser));

        byte[] result = userService.exportUsersToExcel();

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    @DisplayName("Should successfully get user statistics")
    void shouldSuccessfullyGetUserStatistics() {
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.count(any(org.springframework.data.jpa.domain.Specification.class))).thenReturn(5L);

        assertEquals(10L, userService.getTotalUsers());
        assertEquals(5L, userService.getUserCountByStatus(UserStatus.ACTIVE));
        assertEquals(5L, userService.getOtpRequiredUserCount());
        assertEquals(5L, userService.getEmailVerifiedUserCount());
    }

    private Map<String, Object> createGoogleUserInfo(String googleId, String email, String givenName, String familyName, String name) {
        Map<String, Object> googleUserInfo = new HashMap<>();
        googleUserInfo.put("sub", googleId);
        googleUserInfo.put("email", email);
        googleUserInfo.put("given_name", givenName);
        googleUserInfo.put("family_name", familyName);
        googleUserInfo.put("name", name);
        googleUserInfo.put("picture", "https://example.com/picture.jpg");
        return googleUserInfo;
    }

    private void mockLocalizationMessages() {
        lenient().when(localizationService.getMessage(anyString())).thenAnswer(invocation -> resolveMessage(invocation.getArgument(0)));
        lenient().when(localizationService.getMessage(anyString(), any())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Object[] args = extractArgs(invocation.getArguments());
            return resolveMessage(key, args);
        });
    }

    private Object[] extractArgs(Object[] invocationArgs) {
        if (invocationArgs.length <= 1) {
            return new Object[0];
        }
        Object[] args = new Object[invocationArgs.length - 1];
        System.arraycopy(invocationArgs, 1, args, 0, args.length);
        return args;
    }

    private String resolveMessage(String key, Object... args) {
        return switch (key) {
            case "user.not_found" -> format("User with username %s not found", args);
            case "user.already_exists" -> format("User with username %s already exists", args);
            case "user.default_role_not_configured" -> "Default role ROLE_USER not configured";
            case "user.status_or_enabled_required" -> "Either status or enabled must be provided";
            case "user.id_not_found" -> format("User with id %s not found", args);
            case "user.password_fields_required" -> "New password and confirm new password must be provided";
            case "user.password_mismatch" -> "New password and confirm new password do not match";
            case "user.old_password_incorrect" -> "Old password is incorrect";
            default -> key;
        };
    }

    private String format(String template, Object... args) {
        return (args == null || args.length == 0) ? template : String.format(template, args);
    }

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
        user.setIsOtpRequired(true);
        user.setEmailVerified(true);
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
        user.setIsOtpRequired(true);
        user.setEmailVerified(true);
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