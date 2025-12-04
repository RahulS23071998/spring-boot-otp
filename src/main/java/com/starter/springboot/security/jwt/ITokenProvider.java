package com.starter.springboot.security.jwt;

import org.springframework.security.core.Authentication;

public interface ITokenProvider {

    TokenCreationResponse createToken(Authentication authentication, Boolean rememberMe);

    JWTToken createAccessTokenAfterVerifiedOtp(String username, Boolean rememberMe);

    JWTToken createTokenAfterVerifiedOtp(String username, Boolean rememberMe);

    Authentication getAuthentication(String token);

    boolean validateToken(String authToken);

    long getRefreshTokenValidityInSeconds();
}
