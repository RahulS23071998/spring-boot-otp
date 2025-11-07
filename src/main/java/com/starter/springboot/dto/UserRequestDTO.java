package com.starter.springboot.dto;

import com.starter.springboot.constants.DatabaseConstants;
import com.starter.springboot.entity.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating or updating user accounts.
 */
public class UserRequestDTO {

    @NotBlank
    @Size(min = DatabaseConstants.MIN_NAME_LENGTH, max = DatabaseConstants.USERNAME_MAX_LENGTH)
    private String username;

    /**
     * Password can be blank when updating without a password change.
     */
    @Size(min = DatabaseConstants.MIN_PASSWORD_LENGTH, max = DatabaseConstants.PASSWORD_MAX_LENGTH)
    private String password;

    @NotBlank
    @Size(min = DatabaseConstants.MIN_NAME_LENGTH, max = DatabaseConstants.FIRST_NAME_MAX_LENGTH)
    private String firstName;

    @NotBlank
    @Size(min = DatabaseConstants.MIN_NAME_LENGTH, max = DatabaseConstants.LAST_NAME_MAX_LENGTH)
    private String lastName;

    @NotBlank
    @Email
    @Size(min = DatabaseConstants.MIN_NAME_LENGTH, max = DatabaseConstants.EMAIL_MAX_LENGTH)
    private String email;

    @NotNull
    private Boolean otpRequired = Boolean.FALSE; // default to false when not explicitly provided

    private UserStatus status;

    private Boolean enabled;

    private Long roleId;

    private Long authorityId;

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

    public Boolean getOtpRequired() {
        return otpRequired;
    }

    public void setOtpRequired(Boolean otpRequired) {
        this.otpRequired = otpRequired;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getAuthorityId() {
        return authorityId;
    }

    public void setAuthorityId(Long authorityId) {
        this.authorityId = authorityId;
    }
}