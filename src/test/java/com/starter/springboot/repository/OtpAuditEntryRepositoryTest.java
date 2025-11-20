package com.starter.springboot.repository;

import com.starter.springboot.entity.OtpAuditEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.sql.init.mode=never",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false"
})
@DisplayName("OtpAuditEntryRepository Tests")
class OtpAuditEntryRepositoryTest {

    @Autowired
    private OtpAuditEntryRepository otpAuditEntryRepository;

    @Autowired
    private TestEntityManager entityManager;

    private OtpAuditEntry expiredEntry;
    private OtpAuditEntry validEntry;
    private OtpAuditEntry futureEntry;

    @BeforeEach
    void setUp() {
        otpAuditEntryRepository.deleteAll();

        LocalDate today = LocalDate.now();

        expiredEntry = createOtpAuditEntry(1L, "user1", today.minusDays(15), today.minusDays(10));
        validEntry = createOtpAuditEntry(2L, "user2", today.minusDays(5), today.plusDays(5));
        futureEntry = createOtpAuditEntry(3L, "user3", today.plusDays(5), today.plusDays(15));

        entityManager.persist(expiredEntry);
        entityManager.persist(validEntry);
        entityManager.persist(futureEntry);
        entityManager.flush();
    }

    private OtpAuditEntry createOtpAuditEntry(Long id, String username, LocalDate issuedOn, LocalDate expiresOn) {
        OtpAuditEntry entry = new OtpAuditEntry();
        entry.setId(id);
        entry.setUsername(username);
        entry.setIssuedOn(issuedOn);
        entry.setExpiresOn(expiresOn);
        entry.setPartnerExpiry("2024-01-01");
        return entry;
    }

    @Test
    @DisplayName("Should find OTP audit entry by id")
    void shouldFindOtpAuditEntryById() {
        Optional<OtpAuditEntry> found = otpAuditEntryRepository.findById(1L);

        assertTrue(found.isPresent());
        assertEquals("user1", found.get().getUsername());
    }

    @Test
    @DisplayName("Should return empty when OTP audit entry not found by id")
    void shouldReturnEmptyWhenOtpAuditEntryNotFoundById() {
        Optional<OtpAuditEntry> found = otpAuditEntryRepository.findById(999L);

        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Should create and save OTP audit entry")
    void shouldCreateAndSaveOtpAuditEntry() {
        LocalDate today = LocalDate.now();
        OtpAuditEntry newEntry = createOtpAuditEntry(4L, "user4", today, today.plusDays(7));
        OtpAuditEntry savedEntry = otpAuditEntryRepository.save(newEntry);

        assertNotNull(savedEntry.getId());
        assertEquals("user4", savedEntry.getUsername());
    }

    @Test
    @DisplayName("Should delete expired OTP audit entries")
    void shouldDeleteExpiredOtpAuditEntries() {
        LocalDate cutoffDate = LocalDate.now().minusDays(7);
        int deletedCount = otpAuditEntryRepository.deleteByExpiresOnBefore(cutoffDate);

        assertEquals(1, deletedCount);

        entityManager.flush();
        entityManager.clear();

        Optional<OtpAuditEntry> deletedEntry = otpAuditEntryRepository.findById(1L);
        assertFalse(deletedEntry.isPresent());
    }

    @Test
    @DisplayName("Should not delete entries with expiry date on or after cutoff date")
    void shouldNotDeleteEntriesWithExpiryDateOnOrAfterCutoffDate() {
        LocalDate cutoffDate = LocalDate.now().minusDays(7);
        int deletedCount = otpAuditEntryRepository.deleteByExpiresOnBefore(cutoffDate);

        assertEquals(1, deletedCount);

        entityManager.flush();
        entityManager.clear();

        Optional<OtpAuditEntry> validEntryFromDb = otpAuditEntryRepository.findById(2L);
        assertTrue(validEntryFromDb.isPresent());
    }

    @Test
    @DisplayName("Should return zero when no entries to delete")
    void shouldReturnZeroWhenNoEntriesToDelete() {
        LocalDate cutoffDate = LocalDate.now().minusDays(20);
        int deletedCount = otpAuditEntryRepository.deleteByExpiresOnBefore(cutoffDate);

        assertEquals(0, deletedCount);
    }

    @Test
    @DisplayName("Should delete multiple expired entries")
    void shouldDeleteMultipleExpiredEntries() {
        LocalDate today = LocalDate.now();
        OtpAuditEntry anotherExpiredEntry = createOtpAuditEntry(4L, "user4", today.minusDays(20), today.minusDays(12));
        entityManager.persist(anotherExpiredEntry);
        entityManager.flush();

        LocalDate cutoffDate = LocalDate.now().minusDays(7);
        int deletedCount = otpAuditEntryRepository.deleteByExpiresOnBefore(cutoffDate);

        assertEquals(2, deletedCount);

        entityManager.flush();
        entityManager.clear();

        long remainingCount = otpAuditEntryRepository.count();
        assertEquals(2, remainingCount);
    }

    @Test
    @DisplayName("Should retrieve all OTP audit entries")
    void shouldRetrieveAllOtpAuditEntries() {
        var allEntries = otpAuditEntryRepository.findAll();

        assertEquals(3, allEntries.size());
    }

    @Test
    @DisplayName("Should count total OTP audit entries")
    void shouldCountTotalOtpAuditEntries() {
        long count = otpAuditEntryRepository.count();
        assertEquals(3, count);
    }

    @Test
    @DisplayName("Should delete OTP audit entry by id")
    void shouldDeleteOtpAuditEntryById() {
        Long entryId = validEntry.getId();
        otpAuditEntryRepository.deleteById(entryId);

        entityManager.flush();
        entityManager.clear();

        Optional<OtpAuditEntry> deletedEntry = otpAuditEntryRepository.findById(entryId);
        assertFalse(deletedEntry.isPresent());
    }
}
