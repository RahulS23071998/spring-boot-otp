package com.starter.springboot.services;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Description;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Description(value = "Service for generating and validating OTP.")
@Service
public class OtpGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(OtpGenerator.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final StringRedisTemplate redisTemplate;
    private final OtpProperties otpProperties;

    public OtpGenerator(StringRedisTemplate redisTemplate, OtpProperties otpProperties) {
        this.redisTemplate = redisTemplate;
        this.otpProperties = otpProperties;
    }

    public Integer generateOTP(String key) {
        int otp = 100000 + SECURE_RANDOM.nextInt(900000);
        String hashedOtp = DigestUtils.sha256Hex(String.valueOf(otp));
        String redisKey = "otp:" + key;
        try {
            redisTemplate.opsForValue().set(redisKey, hashedOtp, otpProperties.getExpiryMinutes(), TimeUnit.MINUTES);
        }catch (Exception e) {
            LOGGER.error("Failed to store OTP for key {}: {}", key, e.getMessage());
            return -1;
        }
        return otp;
    }

    public boolean validateOTPBasedOnKey(String key, int otpNumber) {
        String redisKey = "otp:" + key;
        String storedHash = redisTemplate.opsForValue().get(redisKey);

        if (storedHash == null) return false;

        String inputHash = DigestUtils.sha256Hex(String.valueOf(otpNumber));
        if (storedHash.equals(inputHash)) {
            redisTemplate.delete(redisKey); // remove OTP after successful validation from redis cache
            return true;
        }
        return false;
    }

    public void clearOTPFromCache(String key) {
        redisTemplate.delete("otp:" + key);
    }
}
