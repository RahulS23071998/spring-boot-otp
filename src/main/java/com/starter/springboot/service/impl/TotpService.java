package com.starter.springboot.service.impl;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.starter.springboot.dto.OtpValidationResult;
import com.starter.springboot.entity.User;
import com.starter.springboot.repository.UserRepository;
import com.starter.springboot.service.ITotpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
public class TotpService implements ITotpService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TotpService.class);
    private static final String TOTP_SECRET_PREFIX = "totp:secret:";
    private static final long TOTP_EXPIRY_HOURS = 24;
    private final GoogleAuthenticator authenticator;
    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;

    public TotpService(StringRedisTemplate redisTemplate, UserRepository userRepository) {
        this.redisTemplate = redisTemplate;
        this.userRepository = userRepository;
        this.authenticator = new GoogleAuthenticator();
    }

    @Override
    public String generateTotpSecret(String username) {
        GoogleAuthenticatorKey key = authenticator.createCredentials();
        return key.getKey();
    }

    @Override
    public String generateQrCodeUrl(String username, String secret, String issuer) {
        try {
            String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);
            String encodedIssuer = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
            return String.format(
                    "otpauth://totp/%s:%%s?secret=%s&issuer=%s",
                    encodedIssuer, secret, encodedIssuer
            ).formatted(encodedUsername);
        } catch (Exception e) {
            LOGGER.error("Failed to generate QR code URL: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public OtpValidationResult validateTotp(String username, String totpCode) {
        try {
            String secret = getTotpSecret(username);
            if (secret == null) {
                LOGGER.warn("TOTP secret not found for user: {}", username);
                return OtpValidationResult.invalid();
            }

            int code = Integer.parseInt(totpCode.trim());
            if (authenticator.authorize(secret, code)) {
                LOGGER.info("TOTP validation successful for user: {}", username);
                return OtpValidationResult.success();
            }

            LOGGER.warn("TOTP validation failed for user: {}", username);
            return OtpValidationResult.invalid();

        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid TOTP code format for user: {}", username);
            return OtpValidationResult.invalid();
        } catch (Exception e) {
            LOGGER.error("TOTP validation error for user {}: {}", username, e.getMessage());
            return OtpValidationResult.invalid();
        }
    }

    @Override
    public void saveTotpSecret(String username, String secret) {
        try {
            String key = TOTP_SECRET_PREFIX + username;
            redisTemplate.opsForValue().set(key, secret, TOTP_EXPIRY_HOURS, TimeUnit.HOURS);
            LOGGER.info("TOTP secret saved for user: {}", username);
        } catch (Exception e) {
            LOGGER.error("Failed to save TOTP secret for user {}: {}", username, e.getMessage());
        }
    }

    @Override
    public String getTotpSecret(String username) {
        try {
            String key = TOTP_SECRET_PREFIX + username;
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            LOGGER.error("Failed to retrieve TOTP secret for user {}: {}", username, e.getMessage());
            return null;
        }
    }

    @Override
    public void revokeTotpSecret(String username) {
        try {
            String key = TOTP_SECRET_PREFIX + username;
            redisTemplate.delete(key);
            LOGGER.info("TOTP secret revoked for user: {}", username);
        } catch (Exception e) {
            LOGGER.error("Failed to revoke TOTP secret for user {}: {}", username, e.getMessage());
        }
    }

    @Override
    public void saveTotpSecretPersistent(long userId, String secret) {
        try {
            User user = userRepository.findById(userId).orElseThrow(() -> 
                new IllegalArgumentException("User not found with ID: " + userId));
            user.setTotpSecret(secret);
            userRepository.save(user);
            LOGGER.info("TOTP secret saved persistently for user ID: {}", userId);
        } catch (Exception e) {
            LOGGER.error("Failed to save TOTP secret persistently for user ID {}: {}", userId, e.getMessage());
        }
    }

    @Override
    public String getTotpSecretPersistent(long userId) {
        try {
            return userRepository.findById(userId)
                .map(User::getTotpSecret)
                .orElse(null);
        } catch (Exception e) {
            LOGGER.error("Failed to retrieve TOTP secret persistently for user ID {}: {}", userId, e.getMessage());
            return null;
        }
    }

    @Override
    public void revokeTotpSecretPersistent(long userId) {
        try {
            User user = userRepository.findById(userId).orElseThrow(() -> 
                new IllegalArgumentException("User not found with ID: " + userId));
            user.setTotpSecret(null);
            userRepository.save(user);
            LOGGER.info("TOTP secret revoked persistently for user ID: {}", userId);
        } catch (Exception e) {
            LOGGER.error("Failed to revoke TOTP secret persistently for user ID {}: {}", userId, e.getMessage());
        }
    }

    @Override
    public boolean hasTotpSecretPersistent(long userId) {
        try {
            return userRepository.findById(userId)
                .map(user -> user.getTotpSecret() != null)
                .orElse(false);
        } catch (Exception e) {
            LOGGER.error("Failed to check TOTP secret existence for user ID {}: {}", userId, e.getMessage());
            return false;
        }
    }

    private static final String TOTP_USED_PREFIX = "totp:used:";
    private static final long TOTP_USED_TTL_SECONDS = 30;

    @Override
    public OtpValidationResult validateTotpPersistent(long userId, String totpCode) {
        try {
            // Check if code was already used recently
            String usedKey = TOTP_USED_PREFIX + userId;
            String lastUsedCode = redisTemplate.opsForValue().get(usedKey);
            
            if (totpCode.equals(lastUsedCode)) {
                LOGGER.warn("TOTP code replay attempt detected for user ID: {}", userId);
                return OtpValidationResult.invalid();
            }

            String secret = getTotpSecretPersistent(userId);
            if (secret == null) {
                LOGGER.warn("TOTP secret not found for user ID: {}", userId);
                return OtpValidationResult.invalid();
            }

            int code = Integer.parseInt(totpCode.trim());
            if (authenticator.authorize(secret, code)) {
                // Mark code as used
                redisTemplate.opsForValue().set(usedKey, totpCode, TOTP_USED_TTL_SECONDS, TimeUnit.SECONDS);
                
                LOGGER.info("TOTP validation successful for user ID: {}", userId);
                return OtpValidationResult.success();
            }

            LOGGER.warn("TOTP validation failed for user ID: {}", userId);
            return OtpValidationResult.invalid();

        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid TOTP code format for user ID: {}", userId);
            return OtpValidationResult.invalid();
        } catch (Exception e) {
            LOGGER.error("TOTP validation error for user ID {}: {}", userId, e.getMessage());
            return OtpValidationResult.invalid();
        }
    }
}