package com.starter.springboot.dto;

import com.starter.springboot.constants.DatabaseConstants;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class GoogleTokenDTO {

    @NotNull(message = "{validation.notNull}")
    private String idToken;

    private Boolean rememberMe;

    @Size(max = DatabaseConstants.CLIENT_ID_MAX_LENGTH, message = "{validation.clientId.size}")
    private String clientId;

    @Size(max = DatabaseConstants.DEVICE_ID_MAX_LENGTH, message = "{validation.deviceId.size}")
    private String deviceId;

    public GoogleTokenDTO() {
    }

    public GoogleTokenDTO(String idToken) {
        this.idToken = idToken;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
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
