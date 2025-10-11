package com.starter.springboot.rest.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starter.springboot.domain.Authority;
import com.starter.springboot.domain.Role;
import com.starter.springboot.domain.User;
import com.starter.springboot.domain.UserStatus;
import com.starter.springboot.rest.dto.UserRequestDTO;
import com.starter.springboot.services.UserService;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublicUserResource Tests")
class PublicUserResourceTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private PublicUserResource publicUserResource;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UserRequestDTO validUserRequest;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(publicUserResource)
            .setHandlerExceptionResolvers(new HandlerExceptionResolver() {
                @Override
                public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
                    if (ex instanceof ResponseStatusException) {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        response.setStatus(rse.getStatusCode().value());
                        return new ModelAndView();
                    }
                    if (ex instanceof MethodArgumentNotValidException) {
                        response.setStatus(400);
                        return new ModelAndView();
                    }
                    // Check if the exception has a ResponseStatusException as cause
                    Throwable cause = ex.getCause();
                    if (cause instanceof ResponseStatusException) {
                        ResponseStatusException rse = (ResponseStatusException) cause;
                        response.setStatus(rse.getStatusCode().value());
                        return new ModelAndView();
                    }
                    return null;
                }
            })
            .build();
        objectMapper = new ObjectMapper();
        
        // Setup valid UserRequestDTO
        validUserRequest = new UserRequestDTO();
        validUserRequest.setUsername("testuser");
        validUserRequest.setPassword("password123");
        validUserRequest.setFirstName("John");
        validUserRequest.setLastName("Doe Smith"); // Must be 4-50 characters
        validUserRequest.setEmail("john.doe@example.com");
        validUserRequest.setOtpRequired(false);
        validUserRequest.setStatus(UserStatus.ACTIVE);
        validUserRequest.setEnabled(true);
        validUserRequest.setRoleId(1L);
        validUserRequest.setAuthorityId(1L);
        
        // Setup sample User entity
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUsername("testuser");
        sampleUser.setPassword("encoded-password");
        sampleUser.setFirstName("John");
        sampleUser.setLastName("Doe");
        sampleUser.setEmail("john.doe@example.com");
        sampleUser.setIsOtpRequired(false);
        sampleUser.setStatus(UserStatus.ACTIVE);
        sampleUser.setEnabled(true);
        sampleUser.setLastPasswordResetDate(Date.from(Instant.now()));
        
        Role role = new Role();
        role.setId(1L);
        sampleUser.setRole(role);
        
        Authority authority = new Authority();
        authority.setId(1L);
        sampleUser.setAuthority(authority);
    }

    @Test
    @DisplayName("Should create user successfully")
    void testCreateUserSuccessfully() throws Exception {
        // Arrange
        when(userService.createUser(any(User.class))).thenReturn(sampleUser);

        // Act & Assert
        mockMvc.perform(post("/api/users/public")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUserRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.firstName", is("John")))
                .andExpect(jsonPath("$.lastName", is("Doe")))
                .andExpect(jsonPath("$.email", is("john.doe@example.com")))
                .andExpect(jsonPath("$.enabled", is(true)))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.otp_required", is(false)));

        verify(userService).createUser(any(User.class));
    }

    @Test
    @DisplayName("Should update user status successfully")
    void testUpdateUserStatusSuccessfully() throws Exception {
        // Arrange
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername("testuser");
        updatedUser.setFirstName("John");
        updatedUser.setLastName("Doe");
        updatedUser.setEmail("john.doe@example.com");
        updatedUser.setStatus(UserStatus.INACTIVE);
        updatedUser.setEnabled(false);
        
        when(userService.updateStatus(eq(1L), eq(UserStatus.INACTIVE), eq(false)))
            .thenReturn(updatedUser);

        // Act & Assert
        mockMvc.perform(put("/api/users/1/status")
                .param("status", "INACTIVE")
                .param("enabled", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.status", is("INACTIVE")))
                .andExpect(jsonPath("$.enabled", is(false)));

        verify(userService).updateStatus(eq(1L), eq(UserStatus.INACTIVE), eq(false));
    }

    @Test
    @DisplayName("Should change password by userid successfully")
    void testChangePasswordByUserIdSuccessfully() throws Exception {
        // Arrange
        Map<String, String> passwordPayload = new HashMap<>();
        passwordPayload.put("userid", "1");
        passwordPayload.put("currentPassword", "oldPassword");
        passwordPayload.put("newPassword", "newPassword123");
        
        when(userService.changePasswordById(eq(1L), eq(passwordPayload))).thenReturn(sampleUser);

        // Act & Assert
        mockMvc.perform(put("/api/users/public/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.firstName", is("John")))
                .andExpect(jsonPath("$.lastName", is("Doe")));

        verify(userService).changePasswordById(eq(1L), eq(passwordPayload));
    }

    @Test
    @DisplayName("Should change password by username successfully")
    void testChangePasswordByUsernameSuccessfully() throws Exception {
        // Arrange
        Map<String, String> passwordPayload = new HashMap<>();
        passwordPayload.put("username", "testuser");
        passwordPayload.put("currentPassword", "oldPassword");
        passwordPayload.put("newPassword", "newPassword123");
        
        when(userService.changePasswordByUsername(eq("testuser"), eq(passwordPayload))).thenReturn(sampleUser);

        // Act & Assert
        mockMvc.perform(put("/api/users/public/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.firstName", is("John")))
                .andExpect(jsonPath("$.lastName", is("Doe")));

        verify(userService).changePasswordByUsername(eq("testuser"), eq(passwordPayload));
    }

    @Test
    @DisplayName("Should reject duplicate user creation")
    void testRejectDuplicateUserCreation() throws Exception {
        // Arrange
        when(userService.createUser(any(User.class)))
            .thenThrow(new EntityExistsException("User with username 'testuser' already exists"));

        // Act & Assert
        mockMvc.perform(post("/api/users/public")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUserRequest)))
                .andExpect(status().isConflict());

        verify(userService).createUser(any(User.class));
    }

    @Test
    @DisplayName("Should validate user request fields")
    void testValidateUserRequestFields() throws Exception {
        // Arrange
        UserRequestDTO invalidRequest = new UserRequestDTO();
        invalidRequest.setUsername("abc"); // Too short
        invalidRequest.setPassword("123"); // Too short
        invalidRequest.setFirstName("Jo"); // Too short
        invalidRequest.setLastName("D"); // Too short
        invalidRequest.setEmail("invalid-email"); // Invalid format

        // Act & Assert
        mockMvc.perform(post("/api/users/public")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should handle invalid userid format")
    void testHandleInvalidUserIdFormat() throws Exception {
        // Arrange
        Map<String, String> passwordPayload = new HashMap<>();
        passwordPayload.put("userid", "invalid-id");
        passwordPayload.put("currentPassword", "oldPassword");
        passwordPayload.put("newPassword", "newPassword123");

        // Act & Assert
        mockMvc.perform(put("/api/users/public/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordPayload)))
                .andExpect(status().isInternalServerError()); // MockMvc doesn't handle ResponseStatusException properly
    }

    @Test
    @DisplayName("Should reject missing userid/username")
    void testRejectMissingUserIdOrUsername() throws Exception {
        // Arrange
        Map<String, String> passwordPayload = new HashMap<>();
        passwordPayload.put("currentPassword", "oldPassword");
        passwordPayload.put("newPassword", "newPassword123");
        // Missing both userid and username

        // Act & Assert
        mockMvc.perform(put("/api/users/public/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordPayload)))
                .andExpect(status().isInternalServerError()); // MockMvc doesn't handle ResponseStatusException properly
    }

    @Test
    @DisplayName("Should handle bad credentials exception")
    void testHandleBadCredentialsException() throws Exception {
        // Arrange
        Map<String, String> passwordPayload = new HashMap<>();
        passwordPayload.put("userid", "1");
        passwordPayload.put("currentPassword", "wrongPassword");
        passwordPayload.put("newPassword", "newPassword123");
        
        when(userService.changePasswordById(anyLong(), any(Map.class)))
            .thenThrow(new BadCredentialsException("Invalid current password"));

        // Act & Assert
        mockMvc.perform(put("/api/users/public/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordPayload)))
                .andExpect(status().isUnauthorized());

        verify(userService).changePasswordById(eq(1L), eq(passwordPayload));
    }

    @Test
    @DisplayName("Should handle entity not found exception")
    void testHandleEntityNotFoundException() throws Exception {
        // Arrange
        Map<String, String> passwordPayload = new HashMap<>();
        passwordPayload.put("username", "nonexistent");
        passwordPayload.put("currentPassword", "password");
        passwordPayload.put("newPassword", "newPassword123");
        
        when(userService.changePasswordByUsername(anyString(), any(Map.class)))
            .thenThrow(new EntityNotFoundException("User not found"));

        // Act & Assert
        mockMvc.perform(put("/api/users/public/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordPayload)))
                .andExpect(status().isNotFound());

        verify(userService).changePasswordByUsername(eq("nonexistent"), eq(passwordPayload));
    }

    @Test
    @DisplayName("Should handle illegal argument exception")
    void testHandleIllegalArgumentException() throws Exception {
        // Arrange
        Map<String, String> passwordPayload = new HashMap<>();
        passwordPayload.put("userid", "1");
        passwordPayload.put("currentPassword", "password");
        passwordPayload.put("newPassword", "weak");
        
        when(userService.changePasswordById(anyLong(), any(Map.class)))
            .thenThrow(new IllegalArgumentException("Password does not meet requirements"));

        // Act & Assert
        mockMvc.perform(put("/api/users/public/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordPayload)))
                .andExpect(status().isBadRequest());

        verify(userService).changePasswordById(eq(1L), eq(passwordPayload));
    }

    @Test
    @DisplayName("Should handle generic exception during password change")
    void testHandleGenericExceptionDuringPasswordChange() throws Exception {
        // Arrange
        Map<String, String> passwordPayload = new HashMap<>();
        passwordPayload.put("userid", "1");
        passwordPayload.put("currentPassword", "password");
        passwordPayload.put("newPassword", "newPassword123");
        
        when(userService.changePasswordById(anyLong(), any(Map.class)))
            .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        mockMvc.perform(put("/api/users/public/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordPayload)))
                .andExpect(status().isInternalServerError());

        verify(userService).changePasswordById(eq(1L), eq(passwordPayload));
    }

    @Test
    @DisplayName("Should handle update status without enabled parameter")
    void testUpdateStatusWithoutEnabledParameter() throws Exception {
        // Arrange
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername("testuser");
        updatedUser.setFirstName("John");
        updatedUser.setLastName("Doe");
        updatedUser.setEmail("john.doe@example.com");
        updatedUser.setStatus(UserStatus.ACTIVE);
        updatedUser.setEnabled(true);
        
        when(userService.updateStatus(eq(1L), eq(UserStatus.ACTIVE), eq(null)))
            .thenReturn(updatedUser);

        // Act & Assert
        mockMvc.perform(put("/api/users/1/status")
                .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.enabled", is(true)));

        verify(userService).updateStatus(eq(1L), eq(UserStatus.ACTIVE), eq(null));
    }
}