package com.starter.springboot.service.impl;

import com.starter.springboot.entity.OtpAuditEntry;
import com.starter.springboot.repository.OtpAuditEntryRepository;
import com.starter.springboot.service.IOtpAuditService;
import com.starter.springboot.service.IOtpProperties;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Implementation of OTP audit service.
 */
@Service
public class OtpAuditServiceImpl implements IOtpAuditService {

    private final OtpAuditEntryRepository otpAuditEntryRepository;
    private final IOtpProperties otpProperties;

    public OtpAuditServiceImpl(OtpAuditEntryRepository otpAuditEntryRepository, IOtpProperties otpProperties) {
        this.otpAuditEntryRepository = otpAuditEntryRepository;
        this.otpProperties = otpProperties;
    }

    @Override
    public void persistAuditEntry(String username) {
        OtpAuditEntry auditEntry = new OtpAuditEntry();
        LocalDate issuedOn = LocalDate.now();
        LocalDate expiresOn = issuedOn.plusDays(otpProperties.getExpiryMinutes() / 1440); // Assuming 1440 minutes per day
        auditEntry.setUsername(username);
        auditEntry.setIssuedOn(issuedOn);
        auditEntry.setExpiresOn(expiresOn);
        auditEntry.setPartnerExpiry(expiresOn.toString());
        otpAuditEntryRepository.save(auditEntry);
    }
}