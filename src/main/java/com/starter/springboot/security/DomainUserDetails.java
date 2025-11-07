package com.starter.springboot.security;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.starter.springboot.entity.Authority;
import com.starter.springboot.entity.Role;
import com.starter.springboot.entity.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Date;

/**
 * Custom UserDetails implementation that retains key domain attributes to avoid
 * redundant database lookups during authentication flows.
 */
public class DomainUserDetails extends org.springframework.security.core.userdetails.User {

    private final Long userId;
    private final String email;
    private final Boolean otpRequired;
    private final String roleName;
    private final String authorityName;
    private final Date lastPasswordResetDate;

    @JsonCreator
    DomainUserDetails(@JsonProperty("username") String username,
                      @JsonProperty("password") String password,
                      @JsonProperty("authorities") @JsonDeserialize(contentAs = SimpleGrantedAuthority.class) Collection<? extends GrantedAuthority> authorities,
                      @JsonProperty("userId") Long userId,
                      @JsonProperty("email") String email,
                      @JsonProperty("otpRequired") Boolean otpRequired,
                      @JsonProperty("roleName") String roleName,
                      @JsonProperty("authorityName") String authorityName,
                      @JsonProperty("lastPasswordResetDate") Date lastPasswordResetDate) {
        super(username, password, authorities);
        this.userId = userId;
        this.email = email;
        this.otpRequired = otpRequired;
        this.roleName = roleName;
        this.authorityName = authorityName;
        this.lastPasswordResetDate = lastPasswordResetDate;
    }

    public static DomainUserDetails fromUser(User user, Collection<? extends GrantedAuthority> authorities) {
        Role role = user.getRole();
        Authority authority = user.getAuthority();
        return new DomainUserDetails(user.getUsername(),
                                     user.getPassword(),
                                     authorities,
                                     user.getId(),
                                     user.getEmail(),
                                     user.getIsOtpRequired(),
                                     role != null ? role.getName() : null,
                                     authority != null ? authority.getName() : null,
                                     user.getLastPasswordResetDate());
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public Boolean isOtpRequired() {
        return otpRequired;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getAuthorityName() {
        return authorityName;
    }

    public Date getLastPasswordResetDate() {
        return lastPasswordResetDate;
    }
}