package com.starter.springboot.otp;

import com.starter.springboot.converters.LocalDateToSqlDateConverter;
import com.starter.springboot.converters.LocalDateToUtilDateConverter;
import com.starter.springboot.converters.StringToDateConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "otp_audit_entries")
public class OtpAuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = LocalDateToSqlDateConverter.class)
    @Column(name = "issued_on", nullable = false)
    private LocalDate issuedOn;

    @Convert(converter = LocalDateToUtilDateConverter.class)
    @Column(name = "expires_on", nullable = false)
    private LocalDate expiresOn;

    @Convert(converter = StringToDateConverter.class)
    @Column(name = "partner_expiry", columnDefinition = "DATE")
    private String partnerExpiry;

    @Column(name = "username", nullable = false)
    private String username;

    public Long getId() {
        return id;
    }

    public LocalDate getIssuedOn() {
        return issuedOn;
    }

    public void setIssuedOn(LocalDate issuedOn) {
        this.issuedOn = issuedOn;
    }

    public LocalDate getExpiresOn() {
        return expiresOn;
    }

    public void setExpiresOn(LocalDate expiresOn) {
        this.expiresOn = expiresOn;
    }

    public String getPartnerExpiry() {
        return partnerExpiry;
    }

    public void setPartnerExpiry(String partnerExpiry) {
        this.partnerExpiry = partnerExpiry;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}