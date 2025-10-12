package com.starter.springboot.services;

import com.starter.springboot.constants.EmailConstants;
import com.starter.springboot.constants.OtpConstants;
import com.starter.springboot.otp.OtpAuditEntry;
import com.starter.springboot.repositories.OtpAuditEntryRepository;
import com.starter.springboot.rest.dto.EmailDTO;
import com.starter.springboot.services.dto.OtpValidationResult;
import com.starter.springboot.services.dto.OtpValidationStatus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Description;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Description(value = OtpConstants.OTP_SERVICE_DESCRIPTION)
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
    private final OtpAuditEntryRepository otpAuditEntryRepository;

    public OtpService(OtpGenerator otpGenerator,
                       EmailService emailService,
                       UserService userService,
                       OtpProperties otpProperties,
                       StringRedisTemplate redisTemplate,
                       OtpAuditEntryRepository otpAuditEntryRepository) {
        this.otpGenerator = otpGenerator;
        this.emailService = emailService;
        this.userService = userService;
        this.otpProperties = otpProperties;
        this.redisTemplate = redisTemplate;
        this.otpAuditEntryRepository = otpAuditEntryRepository;
    }

    /**
     * Method for generate OTP number
     *
     * @param key - provided key (username in this case)
     * @return boolean value (true|false)
     */
    public Boolean generateOtp(String key)
    {
        String attemptsKey = OtpConstants.OTP_REDIS_KEY_PREFIX + key + OtpConstants.ATTEMPTS_KEY_SUFFIX;

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
            LOGGER.error(EmailConstants.NO_EMAIL_FOR_USERNAME_MESSAGE, key);
            return false;
        }

        List<String> recipients = new ArrayList<>();
        recipients.add(userEmail);

        EmailDTO emailDTO = new EmailDTO();
        emailDTO.setSubject(EmailConstants.OTP_EMAIL_SUBJECT);
        emailDTO.setBody(EmailConstants.OTP_EMAIL_BODY_PREFIX + otpValue);
        emailDTO.setRecipients(recipients);

        Boolean sent = emailService.sendSimpleMessage(emailDTO);
        if (!sent) {
            LOGGER.error(EmailConstants.FAILED_TO_SEND_OTP_EMAIL_MESSAGE, key);
            return false;
        }

        persistAuditEntry(key);
        return true;
    }

    private void persistAuditEntry(String username) {
        OtpAuditEntry auditEntry = new OtpAuditEntry();
        LocalDate issuedOn = LocalDate.now();
        LocalDate expiresOn = issuedOn.plusDays(otpProperties.getExpiryMinutes() / 1440);
        auditEntry.setUsername(username);
        auditEntry.setIssuedOn(issuedOn);
        auditEntry.setExpiresOn(expiresOn);
        auditEntry.setPartnerExpiry(expiresOn.toString());
        otpAuditEntryRepository.save(auditEntry);
    }

    /**
     * Method for validating provided OTP
     *
     * @param key - provided key
     * @param otpNumber - provided OTP number
     * @return validation result
     */
    public OtpValidationResult validateOTP(String key, Integer otpNumber) {
        if (otpNumber == null) {
            return OtpValidationResult.invalid();
        }
        OtpValidationResult result = otpGenerator.validateOtpStatus(key, otpNumber);
        if (result.getStatus() == OtpValidationStatus.SUCCESS) {
            return OtpValidationResult.success();
        }
        if (result.getStatus() == OtpValidationStatus.LOCKED) {
            return OtpValidationResult.locked();
        }
        return OtpValidationResult.invalid();
    }
}
