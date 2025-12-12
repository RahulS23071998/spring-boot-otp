package com.starter.springboot.service;

public interface ITemporaryPasswordTokenService {

    String generateTemporaryToken(String username);

    boolean validateTemporaryToken(String username, String token);

    void invalidateTemporaryToken(String username);

    String getUsernameFromToken(String token);
}
