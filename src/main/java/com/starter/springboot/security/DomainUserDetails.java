package com.starter.springboot.security;

import com.starter.springboot.domain.Authority;
import com.starter.springboot.domain.Role;
import com.starter.springboot.domain.User;
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

    private DomainUserDetails(User user, Collection<? extends GrantedAuthority> authorities) {
        super(user.getUsername(), user.getPassword(), authorities);
        this.userId = user.getId();
        this.email = user.getEmail();
        this.otpRequired = user.getIsOtpRequired();
        Role role = user.getRole();
        this.roleName = role != null ? role.getName() : null;
        Authority authority = user.getAuthority();
        this.authorityName = authority != null ? authority.getName() : null;
        this.lastPasswordResetDate = user.getLastPasswordResetDate();
    }

    public static DomainUserDetails fromUser(User user, Collection<? extends GrantedAuthority> authorities) {
        return new DomainUserDetails(user, authorities);
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