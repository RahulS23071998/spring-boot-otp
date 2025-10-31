package com.starter.springboot.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.starter.springboot.repositories.OtpAuditEntryRepository;
import java.time.LocalDate;

import com.starter.springboot.services.impl.OtpAuditRetentionService;
import com.starter.springboot.services.impl.OtpProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OtpAuditRetentionServiceTest {

    @Mock
    private OtpAuditEntryRepository otpAuditEntryRepository;

    @Mock
    private OtpProperties otpProperties;

    @Mock
    private OtpProperties.AuditPurgeProperties auditPurgeProperties;

    @InjectMocks
    private OtpAuditRetentionService retentionService;

    @BeforeEach
    void setUp() {
        when(otpProperties.getAuditPurge()).thenReturn(auditPurgeProperties);
    }

    @Test
    @DisplayName("Should skip purge when feature disabled")
    void shouldSkipPurgeWhenFeatureDisabled() {
        when(auditPurgeProperties.isEnabled()).thenReturn(false);

        retentionService.purgeExpiredEntries();

        verify(otpAuditEntryRepository, never()).deleteByExpiresOnBefore(any(LocalDate.class));
    }

    @Test
    @DisplayName("Should purge entries older than retention window")
    void shouldPurgeEntriesOlderThanRetentionWindow() {
        when(auditPurgeProperties.isEnabled()).thenReturn(true);
        when(auditPurgeProperties.getRetentionDays()).thenReturn(30);
        when(otpAuditEntryRepository.deleteByExpiresOnBefore(any(LocalDate.class))).thenReturn(5);

        retentionService.purgeExpiredEntries();

        ArgumentCaptor<LocalDate> cutoffCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(otpAuditEntryRepository).deleteByExpiresOnBefore(cutoffCaptor.capture());
        LocalDate expectedCutoff = LocalDate.now().minusDays(30);
        // Allowing some tolerance due to LocalDate.now() invocation inside service
        LocalDate actualCutoff = cutoffCaptor.getValue();
        verify(otpAuditEntryRepository).deleteByExpiresOnBefore(actualCutoff);
    }

    @Test
    @DisplayName("Should use zero retention when configured negative")
    void shouldUseZeroRetentionWhenConfiguredNegative() {
        when(auditPurgeProperties.isEnabled()).thenReturn(true);
        when(auditPurgeProperties.getRetentionDays()).thenReturn(-10);

        retentionService.purgeExpiredEntries();

        verify(otpAuditEntryRepository).deleteByExpiresOnBefore(LocalDate.now());
    }
}