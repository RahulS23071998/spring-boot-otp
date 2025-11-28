package com.starter.springboot.controller;

import com.starter.springboot.constants.ApplicationConstants;
import com.starter.springboot.constants.SecurityConstants;
import com.starter.springboot.entity.Authority;
import com.starter.springboot.entity.Role;
import com.starter.springboot.entity.User;
import com.starter.springboot.dto.UserRequestDTO;
import com.starter.springboot.dto.UserResponseDTO;
import com.starter.springboot.entity.UserStatus;
import com.starter.springboot.service.IUserService;
import com.starter.springboot.service.LocalizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.AccessDeniedException;

import java.util.Map;

/**
 * REST endpoints for managing users.
 * The creation endpoint is public, while updates require authentication.
 */
@RestController
@RequestMapping(ApplicationConstants.API_BASE_PATH + ApplicationConstants.USERS_ENDPOINT)
@Tag(name = "User Management", description = "User management endpoints - create users (public) and manage user status/password (authenticated)")
public class PublicUserResource {

    private final IUserService userService;

    private final LocalizationService localizationService;

    public PublicUserResource(IUserService userService, LocalizationService localizationService) {
        this.userService = userService;
        this.localizationService = localizationService;
    }

    @PostMapping(ApplicationConstants.PUBLIC_ENDPOINT)
    @Operation(summary = "Create a new user",
        description = "Register a new user in the system. This is a public endpoint that does not require authentication. " +
                      "The user can optionally enable OTP-based authentication during registration.")
    @RequestBody(description = "User registration details",
        content = @Content(schema = @Schema(implementation = UserRequestDTO.class),
            examples = @ExampleObject(value = """
                {
                  "username": "john.doe",
                  "password": "SecurePass123!",
                  "firstName": "John",
                  "lastName": "Doe",
                  "email": "john@example.com",
                  "otpRequired": true,
                  "enabled": true,
                  "status": "ACTIVE",
                  "roleId": 1,
                  "authorityId": 1
                }
                """)))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User created successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserResponseDTO.class),
                examples = @ExampleObject(value = """
                    {
                      "id": 1,
                      "username": "john.doe",
                      "firstName": "John",
                      "lastName": "Doe",
                      "email": "john@example.com",
                      "enabled": true,
                      "status": "ACTIVE",
                      "otpRequired": false
                    }
                    """))),
        @ApiResponse(responseCode = "409", description = "User already exists with the given username or email",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "error": "Conflict",
                      "message": "User with username 'john.doe' already exists"
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Invalid request format or validation error",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "error": "Bad Request",
                      "message": "Email is required"
                    }
                    """)))
    })
    public ResponseEntity<UserResponseDTO> createUser(
        @Valid @org.springframework.web.bind.annotation.RequestBody UserRequestDTO request) {
        try {
            User created = userService.createUser(toEntity(request));
            return ResponseEntity.status(HttpStatus.CREATED).body(UserResponseDTO.fromEntity(created));
        } catch (EntityExistsException exists) {
            String message = localizationService.getMessage("user.already_exists", request.getUsername());
            throw new ResponseStatusException(HttpStatus.CONFLICT, message);
        }
    }

    @PreAuthorize("hasAuthority('" + SecurityConstants.ADMIN_AUTHORITY + "') or isAuthenticated()")
    @PutMapping(ApplicationConstants.STATUS_ENDPOINT)
    @Operation(summary = "Update user status",
        description = "Update the status and enabled state of a user. Requires authentication - " +
                      "Admin users or the user themselves can update user status. Common statuses include ACTIVE, INACTIVE, SUSPENDED.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User status updated successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserResponseDTO.class),
                examples = @ExampleObject(value = """
                    {
                      "id": 1,
                      "username": "john.doe",
                      "firstName": "John",
                      "lastName": "Doe",
                      "email": "john@example.com",
                      "enabled": false,
                      "status": "SUSPENDED"
                    }
                    """))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - User lacks permission to update this user"),
        @ApiResponse(responseCode = "404", description = "User not found with the given ID",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "error": "Not Found",
                      "message": "User with id 999 not found"
                    }
                    """)))
    })
    public ResponseEntity<UserResponseDTO> updateStatus(
        @PathVariable(ApplicationConstants.ID_PARAM) @Parameter(description = "User ID") Long id,
        @RequestParam(ApplicationConstants.STATUS_PARAM) @Parameter(description = "New user status (ACTIVE, INACTIVE, SUSPENDED, etc.)") String status,
        @RequestParam(value = ApplicationConstants.ENABLED_PARAM, required = false) @Parameter(description = "Enable or disable the user account") Boolean enabled) {
        User updated = userService.updateStatus(id, Enum.valueOf(UserStatus.class, status), enabled);
        return ResponseEntity.ok(UserResponseDTO.fromEntity(updated));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping(ApplicationConstants.PUBLIC_ENDPOINT + ApplicationConstants.PASSWORD_ENDPOINT)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Change user password",
        description = "Change the password for a user. Can be done either by user ID or username. " +
                      "Users can only change their own password; administrators can change any user's password. " +
                      "The request must include the current password for verification and the new password. " +
                      "Requires JWT authentication.")
    @RequestBody(description = "Password change request - provide either userId or username along with current and new password",
        content = @Content(schema = @Schema(type = "object"),
            examples = @ExampleObject(value = """
                {
                  "userId": "1",
                  "currentPassword": "OldPass123!",
                  "newPassword": "NewPass456!"
                }
                """)))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password changed successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserResponseDTO.class),
                examples = @ExampleObject(value = """
                    {
                      "id": 1,
                      "username": "john.doe",
                      "firstName": "John",
                      "lastName": "Doe",
                      "email": "john@example.com",
                      "enabled": true,
                      "status": "ACTIVE"
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Invalid request - missing userId or username, or invalid password format",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "error": "Bad Request",
                      "message": "Either userId or username is required"
                    }
                    """))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - missing JWT token or current password is incorrect",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "error": "Unauthorized",
                      "message": "Current password is incorrect"
                    }
                    """))),
        @ApiResponse(responseCode = "403", description = "Forbidden - user lacks permission to change password (can only change own password, admins can change any)",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "error": "Forbidden",
                      "message": "You can only change your own password. Contact an administrator to change other users' passwords."
                    }
                    """))),
        @ApiResponse(responseCode = "404", description = "User not found",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "error": "Not Found",
                      "message": "User 'john.doe' not found"
                    }
                    """))
        ),
        @ApiResponse(responseCode = "500", description = "Internal server error while changing password")
    })
    public ResponseEntity<UserResponseDTO> changePasswordPublic(
        @org.springframework.web.bind.annotation.RequestBody Map<String, String> payload) {
        try {
            User updated;
            if (payload.containsKey(ApplicationConstants.USERID_PARAM)) {
                String idVal = payload.get(ApplicationConstants.USERID_PARAM);
                long id;
                try {
                    id = Long.parseLong(idVal);
                } catch (NumberFormatException nfe) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, localizationService.getMessage("user.invalid_userid_format"));
                }
                updated = userService.changePasswordById(id, payload);
            } else if (payload.containsKey(ApplicationConstants.USERNAME_PARAM)) {
                String username = payload.get(ApplicationConstants.USERNAME_PARAM);
                updated = userService.changePasswordByUsername(username, payload);
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, localizationService.getMessage("user.userid_or_username_required"));
            }
            return ResponseEntity.ok(UserResponseDTO.fromEntity(updated));
        } catch (AccessDeniedException ade) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ade.getMessage());
        } catch (BadCredentialsException bce) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, bce.getMessage());
        } catch (IllegalArgumentException iae) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, iae.getMessage());
        } catch (EntityNotFoundException enfe) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, enfe.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, localizationService.getMessage("user.password_change_error"));
        }
    }

    private User toEntity(UserRequestDTO dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        user.setIsOtpRequired(dto.getOtpRequired());
        user.setStatus(dto.getStatus());
        user.setEnabled(dto.getEnabled());
        if (dto.getRoleId() != null) {
            Role role = new Role();
            role.setId(dto.getRoleId());
            user.setRole(role);
        }
        if (dto.getAuthorityId() != null) {
            Authority authority = new Authority();
            authority.setId(dto.getAuthorityId());
            user.setAuthority(authority);
        }
        return user;
    }
}