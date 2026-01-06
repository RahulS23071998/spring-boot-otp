package com.starter.springboot.entity;

import com.starter.springboot.constants.DatabaseConstants;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Entity representing refresh tokens for JWT authentication.
 * Refresh tokens allow clients to obtain new access tokens without re-authentication.
 */
@Entity
@Table(name = DatabaseConstants.REFRESH_TOKENS_TABLE,
       indexes = {
           @Index(name = DatabaseConstants.IDX_REFRESH_TOKEN_USER, columnList = DatabaseConstants.REFRESH_TOKEN_USER_ID_COLUMN),
           @Index(name = DatabaseConstants.IDX_REFRESH_TOKEN_EXPIRES, columnList = DatabaseConstants.REFRESH_TOKEN_EXPIRES_AT_COLUMN),
           @Index(name = DatabaseConstants.IDX_REFRESH_TOKEN_TOKEN, columnList = DatabaseConstants.REFRESH_TOKEN_TOKEN_COLUMN, unique = true)
       })
@EntityListeners(AuditingEntityListener.class)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = DatabaseConstants.REFRESH_TOKEN_USER_ID_COLUMN, nullable = false)
    private Long userId;

    @NotNull
    @Size(max = DatabaseConstants.REFRESH_TOKEN_MAX_LENGTH)
    @Column(name = DatabaseConstants.REFRESH_TOKEN_TOKEN_COLUMN, nullable = false, unique = true, length = DatabaseConstants.REFRESH_TOKEN_MAX_LENGTH)
    private String token;

    @NotNull
    @Column(name = DatabaseConstants.REFRESH_TOKEN_EXPIRES_AT_COLUMN, nullable = false)
    private Instant expiresAt;

    @CreatedDate
    @Column(name = DatabaseConstants.REFRESH_TOKEN_CREATED_AT_COLUMN, nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = DatabaseConstants.REFRESH_TOKEN_REVOKED_AT_COLUMN)
    private Instant revokedAt;

    @Column(name = DatabaseConstants.REFRESH_TOKEN_REPLACED_BY_TOKEN_COLUMN)
    private String replacedByToken;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = Boolean.TRUE;

    public RefreshToken() {}

    public RefreshToken(Long userId, String token, Instant expiresAt) {
        this.userId = userId;
        this.token = token;
        this.expiresAt = expiresAt;
        this.isActive = Boolean.TRUE;
    }

    public RefreshToken(Long userId, String token, Instant expiresAt, String ipAddress, String userAgent) {
        this(userId, token, expiresAt);
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getReplacedByToken() {
        return replacedByToken;
    }

    public void setReplacedByToken(String replacedByToken) {
        this.replacedByToken = replacedByToken;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isTokenActive() {
        return !isExpired() && !isRevoked() && Boolean.TRUE.equals(isActive);
    }
}