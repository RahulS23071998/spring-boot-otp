package com.starter.springboot.entity;

import com.starter.springboot.constants.DatabaseConstants;
import com.starter.springboot.converter.LocalDateToSqlDateConverter;
import com.starter.springboot.converter.LocalDateToUtilDateConverter;
import com.starter.springboot.converter.StringToDateConverter;
import com.starter.springboot.listener.CustomAuditingListener;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Entity
@Table(name = DatabaseConstants.OTP_AUDIT_ENTRIES_TABLE,
       indexes = {
           @Index(name = DatabaseConstants.IDX_OTP_AUDIT_USERNAME, columnList = DatabaseConstants.OTP_USERNAME_COLUMN),
           @Index(name = DatabaseConstants.IDX_OTP_AUDIT_CREATED_DATE, columnList = DatabaseConstants.CREATED_DATE_COLUMN)
       })
@EntityListeners({CustomAuditingListener.class, IdGeneratorEntityListener.class})
public class OtpAuditEntry extends BaseAuditedEntity implements BaseEntityWithId {

    @Id
    private Long id;

    @Convert(converter = LocalDateToSqlDateConverter.class)
    @Column(name = DatabaseConstants.ISSUED_ON_COLUMN, nullable = false)
    @NotNull
    private LocalDate issuedOn;

    @Convert(converter = LocalDateToUtilDateConverter.class)
    @Column(name = DatabaseConstants.EXPIRES_ON_COLUMN, nullable = false)
    @NotNull
    private LocalDate expiresOn;

    @Convert(converter = StringToDateConverter.class)
    @Column(name = DatabaseConstants.PARTNER_EXPIRY_COLUMN, length = 255)
    @Size(max = 255)
    private String partnerExpiry;

    @Column(name = DatabaseConstants.OTP_USERNAME_COLUMN, nullable = false, length = DatabaseConstants.USERNAME_MAX_LENGTH)
    @NotNull
    @Size(max = DatabaseConstants.USERNAME_MAX_LENGTH)
    private String username;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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