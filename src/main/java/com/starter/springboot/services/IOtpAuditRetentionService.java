package com.starter.springboot.services;

/**
 * Interface for OTP Audit Retention service operations.
 * Provides contract for managing OTP audit log retention.
 */
public interface IOtpAuditRetentionService {

    /**
     * Purge expired OTP audit entries based on retention policy
     */
    void purgeExpiredEntries();
}