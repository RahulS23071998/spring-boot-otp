package com.starter.springboot.domain;

import java.util.Date;

import com.starter.springboot.constants.DatabaseConstants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


@Entity
@Table(name = DatabaseConstants.USER_TABLE)
public class User extends BaseAuditedEntity {

    @Id
    @Column(name = DatabaseConstants.USER_ID_COLUMN)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = DatabaseConstants.USERNAME_COLUMN, length = DatabaseConstants.USERNAME_MAX_LENGTH, unique = true)
    @NotNull
    @Size(min = 4, max = DatabaseConstants.USERNAME_MAX_LENGTH)
    private String username;

    @Column(name = DatabaseConstants.PASSWORD_COLUMN, length = DatabaseConstants.PASSWORD_MAX_LENGTH)
    @NotNull
    @Size(min = 4, max = DatabaseConstants.PASSWORD_MAX_LENGTH)
    private String password;

    @Column(name = DatabaseConstants.FIRST_NAME_COLUMN, length = DatabaseConstants.FIRST_NAME_MAX_LENGTH)
    @NotNull
    @Size(min = 4, max = DatabaseConstants.FIRST_NAME_MAX_LENGTH)
    private String firstName;

    @Column(name = DatabaseConstants.LAST_NAME_COLUMN, length = DatabaseConstants.LAST_NAME_MAX_LENGTH)
    @NotNull
    @Size(min = 4, max = DatabaseConstants.LAST_NAME_MAX_LENGTH)
    private String lastName;

    @Column(name = DatabaseConstants.EMAIL_COLUMN, length = DatabaseConstants.EMAIL_MAX_LENGTH)
    @NotNull
    @Size(min = 4, max = DatabaseConstants.EMAIL_MAX_LENGTH)
    private String email;

    @Column(name = DatabaseConstants.ENABLED_COLUMN)
    @NotNull
    private Boolean enabled;

    @Enumerated(EnumType.STRING)
    @Column(name = DatabaseConstants.STATUS_COLUMN, length = DatabaseConstants.STATUS_MAX_LENGTH)
    @NotNull
    private UserStatus status;

    @Column(name = DatabaseConstants.LAST_PASSWORD_RESET_DATE_COLUMN)
    @NotNull
    private Date lastPasswordResetDate;

    @Column(name = DatabaseConstants.IS_OTP_REQUIRED_COLUMN)
    private Boolean isOtpRequired;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = DatabaseConstants.ROLE_ID_COLUMN)
    private Role role;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = DatabaseConstants.AUTHORITY_ID_COLUMN)
    private Authority authority;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public Date getLastPasswordResetDate() {
        return lastPasswordResetDate;
    }

    public void setLastPasswordResetDate(Date lastPasswordResetDate) {
        this.lastPasswordResetDate = lastPasswordResetDate;
    }

    public Boolean getIsOtpRequired() {
        return isOtpRequired;
    }

    public void setIsOtpRequired(Boolean isOtpRequired) {
        this.isOtpRequired = isOtpRequired;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Authority getAuthority() {
        return authority;
    }

    public void setAuthority(Authority authority) {
        this.authority = authority;
    }
}