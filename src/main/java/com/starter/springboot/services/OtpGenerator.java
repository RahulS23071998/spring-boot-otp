package com.starter.springboot.services;

import org.springframework.context.annotation.Description;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Description(value = "Service for generating and validating OTP.")
@Service
public class OtpGenerator {


    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final StringRedisTemplate redisTemplate;
    private final OtpProperties otpProperties;

    public OtpGenerator(StringRedisTemplate redisTemplate, OtpProperties otpProperties) {
        this.redisTemplate = redisTemplate;
        this.otpProperties = otpProperties;
    }

    public Integer generateOTP(String key) {
        int otp = 100000 + SECURE_RANDOM.nextInt(900000);
        redisTemplate.opsForValue().set(key, String.valueOf(otp), otpProperties.getExpiryMinutes(), TimeUnit.MINUTES);
        return otp;
    }

    public Integer getOPTByKey(String key) {
        String otpStr = redisTemplate.opsForValue().get(key);
        if (otpStr == null) return null;
        try {
            return Integer.parseInt(otpStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void clearOTPFromCache(String key) {
        redisTemplate.delete(key);
    }
}
