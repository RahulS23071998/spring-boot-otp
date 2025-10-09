package com.starter.springboot.security.jwt;

import org.springframework.http.HttpStatus;

/**
 * Encapsulates the status and optional token returned when creating JWTs.
 */
public record TokenCreationResponse(HttpStatus status, JWTToken token, String message) {

    public static TokenCreationResponse accepted(JWTToken token) {
        return new TokenCreationResponse(HttpStatus.ACCEPTED, token, null);
    }

    public static TokenCreationResponse rejected(String message) {
        return new TokenCreationResponse(HttpStatus.TOO_MANY_REQUESTS, null, message);
    }
}