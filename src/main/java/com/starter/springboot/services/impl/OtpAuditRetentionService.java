package com.starter.springboot.services.impl;

import com.starter.springboot.constants.OtpConstants;
import com.starter.springboot.repositories.OtpAuditEntryRepository;
import java.time.LocalDate;

import com.starter.springboot.services.IOtpAuditRetentionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Description;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Description(OtpConstants.OTP_AUDIT_PURGE_DESCRIPTION)
public class OtpAuditRetentionService implements IOtpAuditRetentionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OtpAuditRetentionService.class);

    private final OtpAuditEntryRepository otpAuditEntryRepository;
    private final OtpProperties otpProperties;

    public OtpAuditRetentionService(OtpAuditEntryRepository otpAuditEntryRepository,
                                    OtpProperties otpProperties) {
        this.otpAuditEntryRepository = otpAuditEntryRepository;
        this.otpProperties = otpProperties;
    }

    @Scheduled(cron = OtpConstants.OTP_AUDIT_PURGE_CRON)
    @Override
    public void purgeExpiredEntries() {
        OtpProperties.AuditPurgeProperties auditPurge = otpProperties.getAuditPurge();
        if (auditPurge == null || !auditPurge.isEnabled()) {
            LOGGER.debug("Skipping OTP audit purge because it is disabled");
            return;
        }

        int retentionDays = Math.max(auditPurge.getRetentionDays(), 0);
        LocalDate cutoffDate = LocalDate.now().minusDays(retentionDays);
        int removed = otpAuditEntryRepository.deleteByExpiresOnBefore(cutoffDate);

        if (removed > 0) {
            LOGGER.info("Purged {} OTP audit entries with expiry before {}", removed, cutoffDate);
        } else {
            LOGGER.debug("No OTP audit entries required purging before {}", cutoffDate);
        }
    }
}