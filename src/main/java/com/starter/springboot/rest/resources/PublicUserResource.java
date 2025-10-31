package com.starter.springboot.rest.resources;

import com.starter.springboot.constants.ApplicationConstants;
import com.starter.springboot.constants.SecurityConstants;
import com.starter.springboot.domain.Authority;
import com.starter.springboot.domain.Role;
import com.starter.springboot.domain.User;
import com.starter.springboot.rest.dto.UserRequestDTO;
import com.starter.springboot.rest.dto.UserResponseDTO;
import com.starter.springboot.services.IUserService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * REST endpoints for managing users.
 * The creation endpoint is public, while updates require authentication.
 */
@RestController
@RequestMapping(ApplicationConstants.API_BASE_PATH + ApplicationConstants.USERS_ENDPOINT)
public class PublicUserResource {

    private final IUserService userService;

    public PublicUserResource(IUserService userService) {
        this.userService = userService;
    }

    @PostMapping(ApplicationConstants.PUBLIC_ENDPOINT)
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserRequestDTO request) {
        try {
            User created = userService.createUser(toEntity(request));
            return ResponseEntity.status(HttpStatus.CREATED).body(UserResponseDTO.fromEntity(created));
        } catch (EntityExistsException exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exists.getMessage());
        }
    }

    @PreAuthorize("hasAuthority('" + SecurityConstants.ADMIN_AUTHORITY + "') or isAuthenticated()")
    @PutMapping(ApplicationConstants.STATUS_ENDPOINT)
    public ResponseEntity<UserResponseDTO> updateStatus(@PathVariable(ApplicationConstants.ID_PARAM) Long id,
                                                         @RequestParam(ApplicationConstants.STATUS_PARAM) String status,
                                                         @RequestParam(value = ApplicationConstants.ENABLED_PARAM, required = false) Boolean enabled) {
        User updated = userService.updateStatus(id, Enum.valueOf(com.starter.springboot.domain.UserStatus.class, status), enabled);
        return ResponseEntity.ok(UserResponseDTO.fromEntity(updated));
    }

    @PutMapping(ApplicationConstants.PUBLIC_ENDPOINT + ApplicationConstants.PASSWORD_ENDPOINT)
    public ResponseEntity<UserResponseDTO> changePasswordPublic(@RequestBody Map<String, String> payload) {
        try {
            User updated;
            if (payload.containsKey(ApplicationConstants.USERID_PARAM)) {
                String idVal = payload.get(ApplicationConstants.USERID_PARAM);
                long id;
                try {
                    id = Long.parseLong(idVal);
                } catch (NumberFormatException nfe) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ApplicationConstants.INVALID_USERID_FORMAT_MESSAGE);
                }
                updated = userService.changePasswordById(id, payload);
            } else if (payload.containsKey(ApplicationConstants.USERNAME_PARAM)) {
                String username = payload.get(ApplicationConstants.USERNAME_PARAM);
                updated = userService.changePasswordByUsername(username, payload);
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ApplicationConstants.USERID_OR_USERNAME_REQUIRED_MESSAGE);
            }
            return ResponseEntity.ok(UserResponseDTO.fromEntity(updated));
        } catch (BadCredentialsException bce) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, bce.getMessage());
        } catch (IllegalArgumentException iae) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, iae.getMessage());
        } catch (EntityNotFoundException enfe) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, enfe.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ApplicationConstants.PASSWORD_CHANGE_ERROR_MESSAGE);
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