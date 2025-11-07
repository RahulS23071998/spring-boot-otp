package com.starter.springboot.service;

/**
 * Interface for OTP audit operations.
 */
public interface IOtpAuditService {

    /**
     * Persists an audit entry for OTP generation.
     * @param username the username
     */
    void persistAuditEntry(String username);
}