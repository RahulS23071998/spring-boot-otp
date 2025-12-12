package com.starter.springboot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SetPasswordResponseDTO(
    @JsonProperty("status") String status,
    @JsonProperty("message") String message,
    @JsonProperty("username") String username,
    @JsonProperty("password_set") Boolean passwordSet,
    @JsonProperty("timestamp") Instant timestamp
) {

    public static SetPasswordResponseDTO success(String username) {
        return new SetPasswordResponseDTO(
            "SUCCESS",
            "Password set successfully. You can now login with your email and password.",
            username,
            Boolean.TRUE,
            Instant.now()
        );
    }

    public static SetPasswordResponseDTO failed(String message) {
        return new SetPasswordResponseDTO(
            "FAILED",
            message,
            null,
            null,
            Instant.now()
        );
    }

    public static SetPasswordResponseDTO failed(String message, String username) {
        return new SetPasswordResponseDTO(
            "FAILED",
            message,
            username,
            null,
            Instant.now()
        );
    }
}
