package com.starter.springboot.repositories;

import com.starter.springboot.otp.OtpAuditEntry;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OtpAuditEntryRepository extends JpaRepository<OtpAuditEntry, Long> {

}