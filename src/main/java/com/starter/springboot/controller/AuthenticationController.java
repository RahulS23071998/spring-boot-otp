package com.starter.springboot.controller;

import com.starter.springboot.constants.ApplicationConstants;
import com.starter.springboot.controller.builder.AuthResponseBuilder;
import com.starter.springboot.dto.AuthResponseDTO;
import com.starter.springboot.dto.LoginDTO;
import com.starter.springboot.exception.OtpRequiredException;
import com.starter.springboot.security.jwt.ITokenProvider;
import com.starter.springboot.security.jwt.TokenCreationResponse;
import com.starter.springboot.service.LocalizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * REST Controller for handling user authentication.
 * Handles login with username and password, triggering OTP verification if required.
 * This controller focuses solely on the initial authentication step.
 */
@RestController
@RequestMapping(ApplicationConstants.AUTH_ENDPOINT)
@Validated
@Tag(name = "Authentication", description = "User authentication endpoints")
public class AuthenticationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationController.class);

    private final ITokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final LocalizationService localizationService;

    public AuthenticationController(ITokenProvider tokenProvider,
                                    AuthenticationManager authenticationManager,
                                    LocalizationService localizationService) {
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
        this.localizationService = localizationService;
    }

    @PostMapping(value = ApplicationConstants.AUTHENTICATE_ENDPOINT)
    @Operation(summary = "Authenticate user with credentials", 
        description = "Authenticate a user with username and password. If OTP is enabled for the user, " +
                      "the response will indicate that OTP verification is required. An OTP will be sent to the user's email.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Login credentials with optional device/client information",
        content = @Content(schema = @Schema(implementation = LoginDTO.class),
            examples = @ExampleObject(value = """
                {
                  "username": "admin",
                  "password": "nimda",
                  "rememberMe": true,
                  "clientId": "web-portal",
                  "deviceId": "device-1234"
                }
                """)))
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentication successful, JWT token provided or OTP required",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = AuthResponseDTO.class),
                examples = @ExampleObject(value = """
                    {
                      "username": "admin",
                      "success": true,
                      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                      "rememberMe": false,
                      "clientId": "mobile-app",
                      "deviceId": "device-001"
                    }
                    """))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials or OTP not verified",
            content = @Content(mediaType = "application/json", 
                examples = @ExampleObject(value = """
                    {
                      "username": "admin",
                      "success": false,
                      "message": "Invalid credentials"
                    }
                    """))),
        @ApiResponse(responseCode = "403", description = "Account locked or authentication failed",
            content = @Content(mediaType = "application/json", 
                examples = @ExampleObject(value = """
                    {
                      "username": "admin",
                      "success": false,
                      "message": "Account locked"
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Invalid request format or validation error")
    })
    public ResponseEntity<AuthResponseDTO> authorize(@Valid @RequestBody LoginDTO loginDTO) {
        LOGGER.info("Authentication attempt for user: {}", loginDTO.getUsername());

        AuthResponseBuilder responseBuilder = AuthResponseBuilder.withUsername(loginDTO.getUsername())
            .context(loginDTO.getRememberMe(), loginDTO.getClientId(), loginDTO.getDeviceId());

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
            loginDTO.getUsername(), loginDTO.getPassword()
        );
        try {
            Authentication authentication = authenticationManager.authenticate(authenticationToken);
            TokenCreationResponse createResponse = tokenProvider.createToken(authentication, loginDTO.getRememberMe());

            SecurityContextHolder.getContext().setAuthentication(authentication);

            LOGGER.info("Authentication completed for user: {}", loginDTO.getUsername());
            AuthResponseDTO response = AuthResponseDTO.fromTokenCreation(loginDTO.getUsername(), createResponse)
                .withContext(loginDTO.getRememberMe(), loginDTO.getClientId(), loginDTO.getDeviceId());
            return ResponseEntity
                .status(createResponse.status())
                .body(response);
        } catch (OtpRequiredException ex) {
            LOGGER.info("OTP required for user: {}", loginDTO.getUsername());
            throw ex;
        } catch (BadCredentialsException ex) {
            LOGGER.warn("Authentication failed for user: {} due to bad credentials", loginDTO.getUsername());
            return responseBuilder.unauthorizedResponse(localizationService.getMessage("auth.invalid_credentials"));
        } catch (AuthenticationException ex) {
            LOGGER.warn("Authentication failed for user: {}: {}", loginDTO.getUsername(), ex.getMessage());
            return responseBuilder.forbiddenResponse(ex.getMessage());
        }
    }
}
