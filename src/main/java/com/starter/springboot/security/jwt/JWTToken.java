package com.starter.springboot.security.jwt;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.starter.springboot.constants.ApplicationConstants;

/**
 * DTO representing the JWT token payload returned to the client.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JWTToken {

    private String idToken;

    private String refreshToken;

    private String tokenType;

    private Long expiresIn;

    private Long refreshTokenExpiresIn;

    public JWTToken(String idToken, String refreshToken, String tokenType, Long expiresIn, Long refreshTokenExpiresIn) {
        this.idToken = idToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.refreshTokenExpiresIn = refreshTokenExpiresIn;
    }

    public JWTToken(String idToken, String tokenType, Long expiresIn) {
        this.idToken = idToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
    }

    public JWTToken() {
    }

    public static JWTToken bearerToken(String idToken, Long expiresIn) {
        return new JWTToken(idToken, ApplicationConstants.BEARER_TOKEN_TYPE, expiresIn);
    }

    public static JWTToken bearerTokenWithRefresh(String idToken, String refreshToken, Long expiresIn, Long refreshTokenExpiresIn) {
        return new JWTToken(idToken, refreshToken, ApplicationConstants.BEARER_TOKEN_TYPE, expiresIn, refreshTokenExpiresIn);
    }

    @JsonProperty("id_token")
    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    @JsonProperty("token_type")
    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    @JsonProperty("expires_in")
    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    @JsonProperty("refresh_token")
    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    @JsonProperty("refresh_token_expires_in")
    public Long getRefreshTokenExpiresIn() {
        return refreshTokenExpiresIn;
    }

    public void setRefreshTokenExpiresIn(Long refreshTokenExpiresIn) {
        this.refreshTokenExpiresIn = refreshTokenExpiresIn;
    }
}
