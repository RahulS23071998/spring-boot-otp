package com.starter.springboot.security.jwt;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing the JWT token payload returned to the client.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JWTToken {

    private String idToken;

    private String tokenType;

    private Long expiresIn;

    public JWTToken(String idToken, String tokenType, Long expiresIn) {
        this.idToken = idToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
    }

    public JWTToken() {
    }

    public static JWTToken bearerToken(String idToken, Long expiresIn) {
        return new JWTToken(idToken, "Bearer", expiresIn);
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
}
