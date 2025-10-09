package com.starter.springboot.services;

import com.starter.springboot.rest.dto.EmailDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Description;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Description(value = "Service responsible for handling OTP related functionality.")
@Service
public class OtpService {

    private final Logger LOGGER = LoggerFactory.getLogger(OtpService.class);

    private final OtpGenerator otpGenerator;
    private final EmailService emailService;
    private final UserService userService;
    private final OtpProperties otpProperties;
    private final StringRedisTemplate redisTemplate;

    /**
     * Constructor dependency injector
     * @param otpGenerator - otpGenerator dependency
     * @param emailService - email service dependency
     * @param userService - user service dependency
     */
    public OtpService(OtpGenerator otpGenerator, EmailService emailService, UserService userService, OtpProperties otpProperties, StringRedisTemplate redisTemplate)
    {
        this.otpGenerator = otpGenerator;
        this.emailService = emailService;
        this.userService = userService;
        this.otpProperties = otpProperties;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Method for generate OTP number
     *
     * @param key - provided key (username in this case)
     * @return boolean value (true|false)
     */
    public Boolean generateOtp(String key)
    {

        String attemptsKey = key + ":attempts";
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey, 1);
        if (attempts == 1) {
            redisTemplate.expire(attemptsKey, otpProperties.getAttemptWindowMinutes(), TimeUnit.MINUTES);
        }
        if (attempts > otpProperties.getMaxAttempts()) {
            LOGGER.warn("OTP request limit exceeded for key: {}", key);
            return false;
        }


        Integer otpValue = otpGenerator.generateOTP(key);
        if (otpValue == -1)
        {
            LOGGER.error("OTP generator returned error code for key: {}", key);
            return  false;
        }

        LOGGER.debug("Generated OTP for key: {}", key);

        String userEmail = userService.findEmailByUsername(key);
        if (userEmail == null || userEmail.isBlank()) {
            LOGGER.error("No email found for username: {}", key);
            return false;
        }

        List<String> recipients = new ArrayList<>();
        recipients.add(userEmail);

        EmailDTO emailDTO = new EmailDTO();
        emailDTO.setSubject("Spring Boot OTP Password.");
        emailDTO.setBody("OTP Password: " + otpValue);
        emailDTO.setRecipients(recipients);

        Boolean sent = emailService.sendSimpleMessage(emailDTO);
        if (!sent) {
            LOGGER.error("Failed to send OTP email to user: {}", key);
        }
        return sent;
    }

    /**
     * Method for validating provided OTP
     *
     * @param key - provided key
     * @param otpNumber - provided OTP number
     * @return boolean value (true|false)
     */
    public Boolean validateOTP(String key, Integer otpNumber)
    {
        if (otpNumber == null) {
            LOGGER.warn("Attempt to validate OTP with null value for key: {}", key);
            return false;
        }

        Integer cacheOTP = otpGenerator.getOPTByKey(key);
        if (cacheOTP != null && cacheOTP.equals(otpNumber))
        {
            otpGenerator.clearOTPFromCache(key);
            return true;
        }
        LOGGER.warn("Invalid OTP for key: {}", key);
        return false;
    }
}
