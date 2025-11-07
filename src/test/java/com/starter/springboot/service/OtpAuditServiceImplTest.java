package com.starter.springboot.service;

import com.starter.springboot.entity.OtpAuditEntry;
import com.starter.springboot.repository.OtpAuditEntryRepository;
import com.starter.springboot.service.impl.OtpAuditServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OtpAuditServiceImpl Tests")
class OtpAuditServiceImplTest {

    @Mock
    private OtpAuditEntryRepository otpAuditEntryRepository;

    @Mock
    private IOtpProperties otpProperties;

    @InjectMocks
    private OtpAuditServiceImpl auditService;

    @BeforeEach
    void setUp() {
        when(otpProperties.getExpiryMinutes()).thenReturn(10); // 10 minutes for testing
    }

    @Test
    @DisplayName("Should persist audit entry successfully")
    void shouldPersistAuditEntrySuccessfully() {
        // Given
        String username = "testUser";
        LocalDate today = LocalDate.now();
        LocalDate expectedExpiry = today.plusDays(0); // 10 minutes is less than a day, so 0 days

        // When
        auditService.persistAuditEntry(username);

        // Then
        ArgumentCaptor<OtpAuditEntry> entryCaptor = ArgumentCaptor.forClass(OtpAuditEntry.class);
        verify(otpAuditEntryRepository).save(entryCaptor.capture());

        OtpAuditEntry savedEntry = entryCaptor.getValue();
        assertEquals(username, savedEntry.getUsername());
        assertEquals(today, savedEntry.getIssuedOn());
        assertEquals(expectedExpiry, savedEntry.getExpiresOn());
        assertEquals(expectedExpiry.toString(), savedEntry.getPartnerExpiry());
    }

    @Test
    @DisplayName("Should calculate expiry correctly for multi-day expiry")
    void shouldCalculateExpiryCorrectlyForMultiDayExpiry() {
        // Given
        String username = "testUser";
        LocalDate today = LocalDate.now();
        when(otpProperties.getExpiryMinutes()).thenReturn(2880); // 2 days
        LocalDate expectedExpiry = today.plusDays(2);

        // When
        auditService.persistAuditEntry(username);

        // Then
        ArgumentCaptor<OtpAuditEntry> entryCaptor = ArgumentCaptor.forClass(OtpAuditEntry.class);
        verify(otpAuditEntryRepository).save(entryCaptor.capture());

        OtpAuditEntry savedEntry = entryCaptor.getValue();
        assertEquals(expectedExpiry, savedEntry.getExpiresOn());
    }

    @Test
    @DisplayName("Should handle repository exception")
    void shouldHandleRepositoryException() {
        // Given
        String username = "testUser";
        doThrow(new RuntimeException("Database error")).when(otpAuditEntryRepository).save(any());

        // When & Then
        assertThrows(RuntimeException.class, () -> auditService.persistAuditEntry(username));
        verify(otpAuditEntryRepository).save(any());
    }
}