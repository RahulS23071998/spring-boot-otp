package com.starter.springboot.dto;

import com.starter.springboot.constants.DatabaseConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class VerifyTokenRequestDTO {

    @NotNull(message = "{validation.notNull}")
    @NotBlank(message = "{validation.notBlank}")
    @Size(min = DatabaseConstants.MIN_NAME_LENGTH, max = DatabaseConstants.USERNAME_MAX_LENGTH, message = "{validation.username.size}")
    private String username;

    @NotNull(message = "{validation.otp.required}")
    private Integer otp;

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

    public Integer getOtp() {
        return otp;
    }

    public void setOtp(Integer otp) {
        this.otp = otp;
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
