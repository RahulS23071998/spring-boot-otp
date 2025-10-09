package com.starter.springboot.rest.dto;

import java.time.Instant;

/**
 * Structured error payload returned by the REST API.
 */
public record ErrorResponse(Instant timestamp, int status, String error, String message, Object details) {

    public static ErrorResponse of(int status, String error, String message, Object details) {
        return new ErrorResponse(Instant.now(), status, error, message, details);
    }
}