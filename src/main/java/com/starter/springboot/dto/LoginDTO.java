package com.starter.springboot.dto;

import com.starter.springboot.constants.DatabaseConstants;
import com.starter.springboot.constants.ValidationConstants;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Login Data Transfer Object
 *
 * @class LoginDTO
 */
public class LoginDTO {

    @Pattern(regexp = ValidationConstants.USERNAME_PATTERN, message = "{validation.username.pattern}")
    @NotNull(message = "{validation.notNull}")
    @Size(min = DatabaseConstants.MIN_USERNAME_LENGTH, max = DatabaseConstants.USERNAME_MAX_LENGTH, message = "{validation.username.size}")
    private String username;

    @NotNull(message = "{validation.notNull}")
    @Size(min = DatabaseConstants.MIN_PASSWORD_LENGTH, max = DatabaseConstants.DTO_PASSWORD_MAX_LENGTH, message = "{validation.password.size}")
    private String password;

    private Boolean rememberMe;

    @Size(max = DatabaseConstants.CLIENT_ID_MAX_LENGTH, message = "{validation.clientId.size}")
    private String clientId;

    @Size(max = DatabaseConstants.DEVICE_ID_MAX_LENGTH, message = "{validation.deviceId.size}")
    private String deviceId;

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

    public Boolean getRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(Boolean rememberMe) {
        this.rememberMe = rememberMe;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
