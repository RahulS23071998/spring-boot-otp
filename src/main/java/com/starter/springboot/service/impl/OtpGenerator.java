package com.starter.springboot.service.impl;

import com.starter.springboot.constants.OtpConstants;
import com.starter.springboot.dto.OtpValidationResult;
import com.starter.springboot.service.IOtpGenerator;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Description;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Description(value = "Service for generating and validating OTP.")
@Service
public class OtpGenerator implements IOtpGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(OtpGenerator.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final StringRedisTemplate redisTemplate;
    private final OtpProperties otpProperties;

    public OtpGenerator(StringRedisTemplate redisTemplate, OtpProperties otpProperties) {
        this.redisTemplate = redisTemplate;
        this.otpProperties = otpProperties;
    }

    @Override
    public Integer generateOTP(String key) {
        int otp = 100000 + SECURE_RANDOM.nextInt(900000);
        String hashedOtp = DigestUtils.sha256Hex(String.valueOf(otp));
        String redisKey = OtpConstants.OTP_REDIS_KEY_PREFIX + key;
        String statusKey = redisKey + OtpConstants.STATUS_KEY_SUFFIX;
        String failureKey = redisKey + OtpConstants.FAILURE_KEY_SUFFIX;
        try {
            redisTemplate.opsForValue().set(redisKey, hashedOtp, otpProperties.getExpiryMinutes(), TimeUnit.MINUTES);
            redisTemplate.opsForValue().set(statusKey, OtpConstants.STATUS_ACTIVE, otpProperties.getExpiryMinutes(), TimeUnit.MINUTES);
            redisTemplate.opsForValue().set(failureKey, "0", otpProperties.getExpiryMinutes(), TimeUnit.MINUTES);
        } catch (Exception e) {
            LOGGER.error("Failed to store OTP for key {}: {}", key, e.getMessage());
            return -1;
        }
        return otp;
    }

    @Override
    public OtpValidationResult validateOtpStatus(String key, int otpNumber) {
        String redisKey = OtpConstants.OTP_REDIS_KEY_PREFIX + key;
        String statusKey = redisKey + OtpConstants.STATUS_KEY_SUFFIX;
        String failureKey = redisKey + OtpConstants.FAILURE_KEY_SUFFIX;

        String status = redisTemplate.opsForValue().get(statusKey);
        if (OtpConstants.STATUS_LOCKED.equals(status)) {
            LOGGER.warn("OTP validation blocked for key {} due to lock state", key);
            return OtpValidationResult.locked();
        }
        if (!OtpConstants.STATUS_ACTIVE.equals(status)) {
            LOGGER.warn("Attempt to validate OTP for key {} but status is {}", key, status);
            return OtpValidationResult.invalid();
        }

        String storedHash = redisTemplate.opsForValue().get(redisKey);
        if (Objects.isNull(storedHash)) {
            return OtpValidationResult.invalid();
        }

        String inputHash = DigestUtils.sha256Hex(String.valueOf(otpNumber));
        if (storedHash.equals(inputHash)) {
            redisTemplate.delete(redisKey); // remove OTP after successful validation from redis cache
            redisTemplate.delete(statusKey);
            redisTemplate.delete(failureKey);
            return OtpValidationResult.success();
        }

        Long failures = redisTemplate.opsForValue().increment(failureKey, 1);
        redisTemplate.expire(failureKey, otpProperties.getExpiryMinutes(), TimeUnit.MINUTES);
        if (Objects.nonNull(failures) && failures >= otpProperties.getMaxAttempts()) {
            redisTemplate.opsForValue().set(statusKey, OtpConstants.STATUS_LOCKED, otpProperties.getExpiryMinutes(), TimeUnit.MINUTES);
            redisTemplate.expire(statusKey, otpProperties.getExpiryMinutes(), TimeUnit.MINUTES);
            LOGGER.warn("OTP locked for key {} after {} failed attempts", key, failures);
            return OtpValidationResult.locked();
        }
        return OtpValidationResult.invalid();
    }

    @Override
    public void clearOTPFromCache(String key) {
        String redisKey = OtpConstants.OTP_REDIS_KEY_PREFIX + key;
        redisTemplate.delete(redisKey);
        redisTemplate.delete(redisKey + OtpConstants.STATUS_KEY_SUFFIX);
        redisTemplate.delete(redisKey + OtpConstants.FAILURE_KEY_SUFFIX);
    }
}
