package com.starter.springboot.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for refresh token requests.
 */
public record RefreshTokenRequestDTO(
    @NotBlank(message = "{validation.notBlank}")
    String refreshToken
) {}