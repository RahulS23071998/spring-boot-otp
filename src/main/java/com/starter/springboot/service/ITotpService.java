package com.starter.springboot.service;

import com.starter.springboot.dto.OtpValidationResult;

public interface ITotpService {

    String generateTotpSecret(String username);

    String generateQrCodeUrl(String username, String secret, String issuer);

    OtpValidationResult validateTotp(String username, String totpCode);

    void saveTotpSecret(String username, String secret);

    String getTotpSecret(String username);

    void revokeTotpSecret(String username);

    void saveTotpSecretPersistent(long userId, String secret);

    String getTotpSecretPersistent(long userId);

    void revokeTotpSecretPersistent(long userId);

    boolean hasTotpSecretPersistent(long userId);

    OtpValidationResult validateTotpPersistent(long userId, String totpCode);

}