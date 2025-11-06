package com.starter.springboot.otp;

import com.starter.springboot.constants.DatabaseConstants;
import com.starter.springboot.converters.LocalDateToSqlDateConverter;
import com.starter.springboot.converters.LocalDateToUtilDateConverter;
import com.starter.springboot.converters.StringToDateConverter;
import com.starter.springboot.domain.BaseAuditedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = DatabaseConstants.OTP_AUDIT_ENTRIES_TABLE)
public class OtpAuditEntry extends BaseAuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = LocalDateToSqlDateConverter.class)
    @Column(name = DatabaseConstants.ISSUED_ON_COLUMN, nullable = false)
    private LocalDate issuedOn;

    @Convert(converter = LocalDateToUtilDateConverter.class)
    @Column(name = DatabaseConstants.EXPIRES_ON_COLUMN, nullable = false)
    private LocalDate expiresOn;

    @Convert(converter = StringToDateConverter.class)
    @Column(name = DatabaseConstants.PARTNER_EXPIRY_COLUMN)
    private String partnerExpiry;

    @Column(name = DatabaseConstants.OTP_USERNAME_COLUMN, nullable = false)
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