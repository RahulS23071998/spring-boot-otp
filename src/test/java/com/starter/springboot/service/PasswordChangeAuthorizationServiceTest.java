package com.starter.springboot.service;

import com.starter.springboot.entity.User;
import com.starter.springboot.security.AuthoritiesConstants;
import com.starter.springboot.service.impl.PasswordChangeAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordChangeAuthorizationService Tests")
class PasswordChangeAuthorizationServiceTest {

    @InjectMocks
    private PasswordChangeAuthorizationService authorizationService;

    private static final String AUTHENTICATED_USERNAME = "authenticatedUser";
    private static final String TARGET_USERNAME = "targetUser";
    private static final String ADMIN_USERNAME = "adminUser";

    private User targetUser;
    private User authenticatedUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        targetUser = createTestUser(TARGET_USERNAME, 2L);
        authenticatedUser = createTestUser(AUTHENTICATED_USERNAME, 1L);
        adminUser = createTestUser(ADMIN_USERNAME, 3L);
    }

    @Test
    @DisplayName("Should authorize password change when user changes their own password")
    void shouldAuthorizePasswordChangeWhenUserChangesOwnPassword() {
        // Given
        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            Authentication authentication = createAuthentication(AUTHENTICATED_USERNAME, Collections.emptyList());
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // When & Then - should not throw exception
            assertDoesNotThrow(() -> authorizationService.authorizePasswordChange(authenticatedUser));
        }
    }

    @Test
    @DisplayName("Should authorize password change when admin changes any user's password")
    void shouldAuthorizePasswordChangeWhenAdminChangesAnyPassword() {
        // Given
        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            List<GrantedAuthority> adminAuthorities = Arrays.asList(
                new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN)
            );
            Authentication authentication = createAuthentication(ADMIN_USERNAME, adminAuthorities);
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // When & Then - should not throw exception
            assertDoesNotThrow(() -> authorizationService.authorizePasswordChange(targetUser));
        }
    }

    @Test
    @DisplayName("Should deny password change when non-admin user tries to change another user's password")
    void shouldDenyPasswordChangeWhenNonAdminChangesOtherPassword() {
        // Given
        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            Authentication authentication = createAuthentication(AUTHENTICATED_USERNAME, Collections.emptyList());
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // When & Then
            AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> authorizationService.authorizePasswordChange(targetUser));
            assertEquals("You can only change your own password. Contact an administrator to change other users' passwords.",
                exception.getMessage());
        }
    }

    @Test
    @DisplayName("Should authorize password change by username when user changes their own password")
    void shouldAuthorizePasswordChangeByUsernameWhenUserChangesOwnPassword() {
        // Given
        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            Authentication authentication = createAuthentication(AUTHENTICATED_USERNAME, Collections.emptyList());
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // When & Then - should not throw exception
            assertDoesNotThrow(() -> authorizationService.authorizePasswordChangeByUsername(AUTHENTICATED_USERNAME));
        }
    }

    @Test
    @DisplayName("Should authorize password change by username when admin changes any user's password")
    void shouldAuthorizePasswordChangeByUsernameWhenAdminChangesAnyPassword() {
        // Given
        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            List<GrantedAuthority> adminAuthorities = Arrays.asList(
                new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN)
            );
            Authentication authentication = createAuthentication(ADMIN_USERNAME, adminAuthorities);
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // When & Then - should not throw exception
            assertDoesNotThrow(() -> authorizationService.authorizePasswordChangeByUsername(TARGET_USERNAME));
        }
    }

    @Test
    @DisplayName("Should deny password change by username when non-admin user tries to change another user's password")
    void shouldDenyPasswordChangeByUsernameWhenNonAdminChangesOtherPassword() {
        // Given
        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            Authentication authentication = createAuthentication(AUTHENTICATED_USERNAME, Collections.emptyList());
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // When & Then
            AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> authorizationService.authorizePasswordChangeByUsername(TARGET_USERNAME));
            assertEquals("You can only change your own password. Contact an administrator to change other users' passwords.",
                exception.getMessage());
        }
    }

    @Test
    @DisplayName("Should throw IllegalStateException when no authenticated user found")
    void shouldThrowIllegalStateExceptionWhenNoAuthenticatedUser() {
        // Given
        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(null);
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // When & Then
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> authorizationService.authorizePasswordChange(targetUser));
            assertEquals("No authenticated user found", exception.getMessage());
        }
    }

    @Test
    @DisplayName("Should throw IllegalStateException when authentication is not authenticated")
    void shouldThrowIllegalStateExceptionWhenAuthenticationNotAuthenticated() {
        // Given
        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            Authentication authentication = mock(Authentication.class);
            when(authentication.isAuthenticated()).thenReturn(false);
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // When & Then
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> authorizationService.authorizePasswordChange(targetUser));
            assertEquals("No authenticated user found", exception.getMessage());
        }
    }

    @Test
    @DisplayName("Should throw IllegalStateException when authenticated user has no username")
    void shouldThrowIllegalStateExceptionWhenAuthenticatedUserHasNoUsername() {
        // Given
        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            Authentication authentication = mock(Authentication.class);
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn(null);
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // When & Then
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> authorizationService.authorizePasswordChange(targetUser));
            assertEquals("Authenticated user has no username", exception.getMessage());
        }
    }

    @Test
    @DisplayName("Should return authenticated username from security context")
    void shouldReturnAuthenticatedUsernameFromSecurityContext() {
        // Given
        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            Authentication authentication = createAuthentication(AUTHENTICATED_USERNAME, Collections.emptyList());
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // When
            String result = authorizationService.getAuthenticatedUsername();

            // Then
            assertEquals(AUTHENTICATED_USERNAME, result);
        }
    }

    @Test
    @DisplayName("Should return true when authenticated user is admin")
    void shouldReturnTrueWhenAuthenticatedUserIsAdmin() {
        // Given
        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            List<GrantedAuthority> adminAuthorities = Arrays.asList(
                new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN)
            );
            Authentication authentication = createAuthentication(ADMIN_USERNAME, adminAuthorities);
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // When
            boolean result = authorizationService.isAuthenticatedUserAdmin();

            // Then
            assertTrue(result);
        }
    }

    @Test
    @DisplayName("Should return true when authenticated user has ROLE_ADMIN authority")
    void shouldReturnTrueWhenAuthenticatedUserHasRoleAdminAuthority() {
        // Given
        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            List<GrantedAuthority> adminAuthorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_ADMIN")
            );
            Authentication authentication = createAuthentication(ADMIN_USERNAME, adminAuthorities);
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // When
            boolean result = authorizationService.isAuthenticatedUserAdmin();

            // Then
            assertTrue(result);
        }
    }

    @Test
    @DisplayName("Should return true when authenticated user has ADMIN authority")
    void shouldReturnTrueWhenAuthenticatedUserHasAdminAuthority() {
        // Given
        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            List<GrantedAuthority> adminAuthorities = Arrays.asList(
                new SimpleGrantedAuthority("ADMIN")
            );
            Authentication authentication = createAuthentication(ADMIN_USERNAME, adminAuthorities);
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // When
            boolean result = authorizationService.isAuthenticatedUserAdmin();

            // Then
            assertTrue(result);
        }
    }

    @Test
    @DisplayName("Should return false when authenticated user is not admin")
    void shouldReturnFalseWhenAuthenticatedUserIsNotAdmin() {
        // Given
        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            List<GrantedAuthority> userAuthorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_USER")
            );
            Authentication authentication = createAuthentication(AUTHENTICATED_USERNAME, userAuthorities);
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // When
            boolean result = authorizationService.isAuthenticatedUserAdmin();

            // Then
            assertFalse(result);
        }
    }

    @Test
    @DisplayName("Should return false when no authenticated user found for admin check")
    void shouldReturnFalseWhenNoAuthenticatedUserForAdminCheck() {
        // Given
        try (MockedStatic<SecurityContextHolder> securityContextHolder = Mockito.mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(null);
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // When
            boolean result = authorizationService.isAuthenticatedUserAdmin();

            // Then
            assertFalse(result);
        }
    }

    @Test
    @DisplayName("Should throw NullPointerException when target user is null")
    void shouldThrowNullPointerExceptionWhenTargetUserIsNull() {
        // When & Then
        assertThrows(NullPointerException.class,
            () -> authorizationService.authorizePasswordChange(null));
    }

    @Test
    @DisplayName("Should throw NullPointerException when target username is null")
    void shouldThrowNullPointerExceptionWhenTargetUsernameIsNull() {
        // When & Then
        assertThrows(NullPointerException.class,
            () -> authorizationService.authorizePasswordChangeByUsername(null));
    }

    private User createTestUser(String username, Long id) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        return user;
    }

    private Authentication createAuthentication(String username, List<GrantedAuthority> authorities) {
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getName()).thenReturn(username);
        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        lenient().when(authentication.getAuthorities()).thenReturn((Collection) authorities);
        return authentication;
    }
}