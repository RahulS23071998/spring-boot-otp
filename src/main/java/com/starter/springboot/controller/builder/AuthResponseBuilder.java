package com.starter.springboot.controller.builder;

import com.starter.springboot.dto.AuthResponseDTO;
import com.starter.springboot.security.jwt.JWTToken;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class AuthResponseBuilder {

    private final String username;
    private Boolean rememberMe;
    private String clientId;
    private String deviceId;

    private AuthResponseBuilder(String username) {
        this.username = username;
    }

    public static AuthResponseBuilder withUsername(String username) {
        return new AuthResponseBuilder(username);
    }

    public AuthResponseBuilder rememberMe(Boolean rememberMe) {
        this.rememberMe = rememberMe;
        return this;
    }

    public AuthResponseBuilder clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public AuthResponseBuilder deviceId(String deviceId) {
        this.deviceId = deviceId;
        return this;
    }

    public AuthResponseBuilder context(Boolean rememberMe, String clientId, String deviceId) {
        this.rememberMe = rememberMe;
        this.clientId = clientId;
        this.deviceId = deviceId;
        return this;
    }

    public ResponseEntity<AuthResponseDTO> successResponse(JWTToken token) {
        return ResponseEntity.ok(
            AuthResponseDTO.success(username, token, rememberMe)
                .withContext(rememberMe, clientId, deviceId)
        );
    }

    public ResponseEntity<AuthResponseDTO> unauthorizedResponse(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(AuthResponseDTO.failed(username, message)
                .withContext(rememberMe, clientId, deviceId));
    }

    public ResponseEntity<AuthResponseDTO> forbiddenResponse(String message) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(AuthResponseDTO.failed(username, message)
                .withContext(rememberMe, clientId, deviceId));
    }

    public ResponseEntity<AuthResponseDTO> lockedResponse(String message) {
        return ResponseEntity.status(HttpStatus.LOCKED)
            .body(AuthResponseDTO.failed(username, message)
                .withContext(rememberMe, clientId, deviceId));
    }

    public ResponseEntity<AuthResponseDTO> failedResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status)
            .body(AuthResponseDTO.failed(username, message)
                .withContext(rememberMe, clientId, deviceId));
    }
}
