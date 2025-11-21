package com.starter.springboot.security;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

/**
 * Lightweight DTO stored in Redis to avoid serializing full JPA entities (which
 * may include lazy proxies or sensitive fields). Contains only the data needed
 * to authenticate and build DomainUserDetails.
 */
public class CachedUser {
    private final Long id;
    private final String username;
    private final String password; // hashed password
    private final String email;
    private final Boolean enabled;
    private final Boolean otpRequired;
    private final String roleName;
    private final String authorityName;
    private final Date lastPasswordResetDate;

    @JsonCreator
    public CachedUser(@JsonProperty("id") Long id,
                      @JsonProperty("username") String username,
                      @JsonProperty("password") String password,
                      @JsonProperty("email") String email,
                      @JsonProperty("enabled") Boolean enabled,
                      @JsonProperty("otpRequired") Boolean otpRequired,
                      @JsonProperty("roleName") String roleName,
                      @JsonProperty("authorityName") String authorityName,
                      @JsonProperty("lastPasswordResetDate") Date lastPasswordResetDate) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.enabled = enabled;
        this.otpRequired = otpRequired;
        this.roleName = roleName;
        this.authorityName = authorityName;
        this.lastPasswordResetDate = lastPasswordResetDate;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }
    public Boolean getEnabled() { return enabled; }
    public Boolean getOtpRequired() { return otpRequired; }
    public String getRoleName() { return roleName; }
    public String getAuthorityName() { return authorityName; }
    public Date getLastPasswordResetDate() { return lastPasswordResetDate; }
}

