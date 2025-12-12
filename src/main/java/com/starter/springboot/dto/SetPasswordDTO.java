package com.starter.springboot.dto;

import com.starter.springboot.constants.DatabaseConstants;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SetPasswordDTO {

    @NotNull(message = "{validation.notNull}")
    @Size(min = DatabaseConstants.MIN_PASSWORD_LENGTH, max = DatabaseConstants.DTO_PASSWORD_MAX_LENGTH, message = "{validation.password.size}")
    private String password;

    @NotNull(message = "{validation.notNull}")
    @Size(min = DatabaseConstants.MIN_PASSWORD_LENGTH, max = DatabaseConstants.DTO_PASSWORD_MAX_LENGTH, message = "{validation.password.size}")
    private String confirmPassword;

    private String temporaryToken;

    private String username;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getTemporaryToken() {
        return temporaryToken;
    }

    public void setTemporaryToken(String temporaryToken) {
        this.temporaryToken = temporaryToken;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
