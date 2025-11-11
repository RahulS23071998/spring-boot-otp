package com.starter.springboot.controller;

import com.starter.springboot.constants.ApplicationConstants;
import com.starter.springboot.dto.UserResponseDTO;
import com.starter.springboot.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller for retrieving user information.
 * Requires JWT authentication for access.
 */
@RestController
@RequestMapping(ApplicationConstants.API_BASE_PATH)
@Tag(name = "Users", description = "User information retrieval endpoints (requires authentication)")
@SecurityRequirement(name = "bearerAuth")
public class UserResource {

    private final Logger log = LoggerFactory.getLogger(UserResource.class);

    private final IUserService userService;

    public UserResource(IUserService userService) {
        this.userService = userService;
    }

    @GetMapping(value = ApplicationConstants.USERS_ENDPOINT, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all users",
        description = "Retrieve a list of all users in the system. This endpoint requires JWT authentication. " +
                      "Only authenticated users with appropriate permissions can access this endpoint.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved list of users",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserResponseDTO.class, type = "array"),
                examples = @ExampleObject(value = """
                    [
                      {
                        "id": 1,
                        "username": "john.doe",
                        "firstName": "John",
                        "lastName": "Doe",
                        "email": "john@example.com",
                        "enabled": true,
                        "status": "ACTIVE"
                      },
                      {
                        "id": 2,
                        "username": "jane.smith",
                        "firstName": "Jane",
                        "lastName": "Smith",
                        "email": "jane@example.com",
                        "enabled": true,
                        "status": "ACTIVE"
                      }
                    ]
                    """))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "error": "Unauthorized",
                      "message": "Invalid or expired token"
                    }
                    """))),
        @ApiResponse(responseCode = "403", description = "Forbidden - User lacks required permissions",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "error": "Forbidden",
                      "message": "Access denied"
                    }
                    """)))
    })
    public ResponseEntity<List<UserResponseDTO>> getUsers()
    {
        log.debug(ApplicationConstants.CLIENT_REST_REQUEST_MESSAGE);

        List<UserResponseDTO> userList = userService.findAllUsers().stream()
            .map(UserResponseDTO::fromEntity)
            .toList();
        return ResponseEntity.ok(userList);
    }
}
