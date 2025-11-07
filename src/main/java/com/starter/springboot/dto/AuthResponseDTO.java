package com.starter.springboot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.starter.springboot.constants.ApplicationConstants;
import com.starter.springboot.security.jwt.JWTToken;
import com.starter.springboot.security.jwt.TokenCreationResponse;

import java.time.Instant;

/**
 * Standard authentication response shared by /auth endpoints.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponseDTO(
    @JsonProperty("username") String username,
    @JsonProperty("status") String status,
    @JsonProperty("message") String message,
    @JsonProperty("otp_required") Boolean otpRequired,
    @JsonProperty("token") JWTToken token,
    @JsonProperty("issued_at") Instant issuedAt,
    @JsonProperty("remember_me") Boolean rememberMe,
    @JsonProperty("client_id") String clientId,
    @JsonProperty("device_id") String deviceId
) {

    public static AuthResponseDTO fromTokenCreation(String username, TokenCreationResponse response) {
        String statusValue = switch (response.status()) {
            case OK -> ApplicationConstants.SUCCESS_STATUS;
            case ACCEPTED -> ApplicationConstants.OTP_PENDING_STATUS;
            case TOO_MANY_REQUESTS -> ApplicationConstants.RATE_LIMITED_STATUS;
            default -> response.status().name();
        };

        return new AuthResponseDTO(
            username,
            statusValue,
            response.message(),
            response.otpRequired(),
            response.token(),
            Instant.now(),
            null,
            null,
            null
        );
    }

    public static AuthResponseDTO success(String username, JWTToken token, Boolean rememberMe) {
        return new AuthResponseDTO(
            username,
            ApplicationConstants.SUCCESS_STATUS,
            ApplicationConstants.SUCCESS_MESSAGE,
            Boolean.FALSE,
            token,
            Instant.now(),
            rememberMe,
            null,
            null
        );
    }

    public static AuthResponseDTO failed(String username, String message) {
        return new AuthResponseDTO(
            username,
            ApplicationConstants.FAILED_STATUS,
            message,
            Boolean.FALSE,
            null,
            Instant.now(),
            null,
            null,
            null
        );
    }

    public AuthResponseDTO withContext(Boolean rememberMe, String clientId, String deviceId) {
        return new AuthResponseDTO(
            this.username,
            this.status,
            this.message,
            this.otpRequired,
            this.token,
            this.issuedAt,
            rememberMe,
            clientId,
            deviceId
        );
    }
}