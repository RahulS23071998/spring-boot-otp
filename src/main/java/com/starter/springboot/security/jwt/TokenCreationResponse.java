package com.starter.springboot.security.jwt;

import org.springframework.http.HttpStatus;

/**
 * Encapsulates the status and optional token returned when creating JWTs.
 */
public record TokenCreationResponse(HttpStatus status, JWTToken token) {

    public static TokenCreationResponse accepted() {
        return new TokenCreationResponse(HttpStatus.ACCEPTED, null);
    }
}