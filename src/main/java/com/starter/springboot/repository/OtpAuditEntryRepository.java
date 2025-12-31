package com.starter.springboot.repository;

import com.starter.springboot.entity.OtpAuditEntry;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OtpAuditEntryRepository extends JpaRepository<OtpAuditEntry, Long> {

    int deleteByExpiresOnBefore(LocalDate cutoffDate);

    Page<OtpAuditEntry> findAll(Pageable pageable);
}