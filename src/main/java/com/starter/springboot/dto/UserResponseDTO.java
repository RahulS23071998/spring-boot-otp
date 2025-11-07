package com.starter.springboot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.starter.springboot.entity.User;
import com.starter.springboot.entity.UserStatus;

import java.time.Instant;
import java.util.Date;

/**
 * Canonical representation of a user that can be safely returned to clients.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponseDTO(
    Long id,
    String username,
    String firstName,
    String lastName,
    String email,
    Boolean enabled,
    UserStatus status,
    @JsonProperty("otp_required") Boolean otpRequired,
    @JsonProperty("last_password_reset") Instant lastPasswordReset
) {

    public static UserResponseDTO fromEntity(User user) {
        Date lastReset = user.getLastPasswordResetDate();
        return new UserResponseDTO(
            user.getId(),
            user.getUsername(),
            user.getFirstName(),
            user.getLastName(),
            user.getEmail(),
            user.getEnabled(),
            user.getStatus(),
            user.getIsOtpRequired(),
            lastReset != null ? lastReset.toInstant() : null
        );
    }
}