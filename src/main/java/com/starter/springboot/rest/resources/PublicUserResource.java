package com.starter.springboot.rest.resources;

import com.starter.springboot.domain.Authority;
import com.starter.springboot.domain.Role;
import com.starter.springboot.domain.User;
import com.starter.springboot.rest.dto.UserRequestDTO;
import com.starter.springboot.rest.dto.UserResponseDTO;
import com.starter.springboot.services.UserService;
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
@RequestMapping("/api/users")
public class PublicUserResource {

    private final UserService userService;

    public PublicUserResource(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/public")
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserRequestDTO request) {
        try {
            User created = userService.createUser(toEntity(request));
            return ResponseEntity.status(HttpStatus.CREATED).body(UserResponseDTO.fromEntity(created));
        } catch (EntityExistsException exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exists.getMessage());
        }
    }

    @PreAuthorize("hasAuthority('ADMIN') or isAuthenticated()")
    @PutMapping("/{id}/status")
    public ResponseEntity<UserResponseDTO> updateStatus(@PathVariable("id") Long id,
                                                         @RequestParam("status") String status,
                                                         @RequestParam(value = "enabled", required = false) Boolean enabled) {
        User updated = userService.updateStatus(id, Enum.valueOf(com.starter.springboot.domain.UserStatus.class, status), enabled);
        return ResponseEntity.ok(UserResponseDTO.fromEntity(updated));
    }

    @PutMapping("/public/password")
    public ResponseEntity<UserResponseDTO> changePasswordPublic(@RequestBody Map<String, String> payload) {
        try {
            User updated;
            if (payload.containsKey("userid")) {
                String idVal = payload.get("userid");
                long id;
                try {
                    id = Long.parseLong(idVal);
                } catch (NumberFormatException nfe) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid userid format");
                }
                updated = userService.changePasswordById(id, payload);
            } else if (payload.containsKey("username")) {
                String username = payload.get("username");
                updated = userService.changePasswordByUsername(username, payload);
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide either userid or username in payload");
            }
            return ResponseEntity.ok(UserResponseDTO.fromEntity(updated));
        } catch (BadCredentialsException bce) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, bce.getMessage());
        } catch (IllegalArgumentException iae) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, iae.getMessage());
        } catch (EntityNotFoundException enfe) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, enfe.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to change password");
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