package com.starter.springboot.controller;

import com.starter.springboot.constants.ApplicationConstants;
import com.starter.springboot.dto.PaginatedResponse;
import com.starter.springboot.dto.UserResponseDTO;
import com.starter.springboot.entity.User;
import com.starter.springboot.service.IPaginationService;
import com.starter.springboot.service.IUserService;
import com.starter.springboot.utils.PaginationConverter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    private final IPaginationService paginationService;

    public UserResource(IUserService userService, IPaginationService paginationService) {
        this.userService = userService;
        this.paginationService = paginationService;
    }

    @GetMapping(value = ApplicationConstants.USERS_ENDPOINT, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all users with pagination",
        description = "Retrieve a paginated list of all users in the system. This endpoint requires JWT authentication. " +
                      "Only authenticated users with appropriate permissions can access this endpoint. " +
                      "Supports sorting by user fields in ascending or descending order.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved paginated list of users",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PaginatedResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "content": [
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
                      ],
                      "page": 0,
                      "size": 20,
                      "totalElements": 2,
                      "totalPages": 1,
                      "first": true,
                      "last": true,
                      "hasNext": false,
                      "hasPrevious": false
                    }
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
    public ResponseEntity<PaginatedResponse<UserResponseDTO>> getUsers(
            @RequestParam(value = "page", defaultValue = "0") @Parameter(description = "Page number (zero-indexed)") int page,
            @RequestParam(value = "size", defaultValue = "20") @Parameter(description = "Page size (max 100)") int size,
            @RequestParam(value = "sortBy", defaultValue = "id") @Parameter(description = "Field to sort by") String sortBy,
            @RequestParam(value = "sortDirection", defaultValue = "asc") @Parameter(description = "Sort direction (asc or desc)") String sortDirection)
    {
        log.debug(ApplicationConstants.CLIENT_REST_REQUEST_MESSAGE);

        Pageable pageable = paginationService.createPageableWithSort(page, size, sortBy, sortDirection);
        Page<User> userPage = userService.findAllUsers(pageable);
        Page<UserResponseDTO> responseDTO = userPage.map(UserResponseDTO::fromEntity);

        return ResponseEntity.ok(PaginationConverter.toResponse(responseDTO, paginationService));
    }
}