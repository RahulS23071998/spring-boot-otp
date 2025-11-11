package com.starter.springboot.controller;

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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for greeting endpoint.
 * Requires JWT authentication with ADMIN or USER role.
 */
@RestController
@RequestMapping("/api/greeting")
@Tag(name = "Greeting", description = "Greeting endpoint (requires JWT authentication with ADMIN or USER role)")
@SecurityRequirement(name = "bearerAuth")
public class GreetingController {

    private final Logger log = LoggerFactory.getLogger(GreetingController.class);

    @GetMapping(value = "/hello", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Hello World",
        description = "A greeting endpoint that requires JWT authentication. Only users with ADMIN or USER roles can access this. " +
                      "Returns a welcome message with timestamp.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved greeting message",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(value = """
                    {
                      "message": "Hello World!",
                      "timestamp": "2024-01-15T10:30:45.123Z",
                      "status": "SUCCESS"
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
        @ApiResponse(responseCode = "403", description = "Forbidden - User lacks required role (ADMIN or USER)",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "error": "Forbidden",
                      "message": "Access denied - Insufficient permissions"
                    }
                    """)))
    })
    public ResponseEntity<Map<String, Object>> hello() {
        log.debug("✅ Hello World endpoint called");
        
        // Get current authentication for debugging
        org.springframework.security.core.Authentication auth = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null) {
            log.debug("✅ User: {} | Authenticated: {} | Authorities: {}", 
                auth.getName(), auth.isAuthenticated(), auth.getAuthorities());
        } else {
            log.warn("⚠️  No authentication found!");
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello World!");
        response.put("timestamp", java.time.Instant.now());
        response.put("status", "SUCCESS");
        
        return ResponseEntity.ok(response);
    }
}