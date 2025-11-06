package com.starter.springboot.services.impl;

import com.starter.springboot.constants.EmailConstants;
import com.starter.springboot.constants.OtpConstants;
import com.starter.springboot.otp.OtpAuditEntry;
import com.starter.springboot.repositories.OtpAuditEntryRepository;
import com.starter.springboot.rest.dto.EmailDTO;
import com.starter.springboot.services.IEmailService;
import com.starter.springboot.services.IOtpGenerator;
import com.starter.springboot.services.IOtpService;
import com.starter.springboot.services.dto.OtpGenerationResult;
import com.starter.springboot.services.dto.OtpValidationResult;
import com.starter.springboot.services.dto.OtpValidationStatus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Description;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Description(value = OtpConstants.OTP_SERVICE_DESCRIPTION)
@Service
public class OtpService implements IOtpService {

    private final Logger LOGGER = LoggerFactory.getLogger(OtpService.class);

    private final IOtpGenerator otpGenerator;
    private final IEmailService emailService;
    private final OtpProperties otpProperties;
    private final StringRedisTemplate redisTemplate;

    /**
     * Constructor dependency injector
     * @param otpGenerator - otpGenerator dependency
     * @param emailService - email service dependency
     * @param otpProperties - otp properties dependency
     * @param redisTemplate - redis template dependency
     * @param otpAuditEntryRepository - otp audit entry repository dependency
     */
    private final OtpAuditEntryRepository otpAuditEntryRepository;

    public OtpService(IOtpGenerator otpGenerator,
                       IEmailService emailService,
                       OtpProperties otpProperties,
                       StringRedisTemplate redisTemplate,
                       OtpAuditEntryRepository otpAuditEntryRepository) {
        this.otpGenerator = otpGenerator;
        this.emailService = emailService;
        this.otpProperties = otpProperties;
        this.redisTemplate = redisTemplate;
        this.otpAuditEntryRepository = otpAuditEntryRepository;
    }

    /**
     * Method for generate OTP number
     *
     * @param key - provided key (username in this case)
     * @param userEmail - user's email address
     * @return OtpGenerationResult containing success status and message
     */
    @Override
    public OtpGenerationResult generateOtp(String key, String userEmail)
    {
        // Rate limiting: Check if OTP was sent recently (within 15 seconds)
        String rateLimitKey = OtpConstants.OTP_REDIS_KEY_PREFIX + key + OtpConstants.RATE_LIMIT_KEY_SUFFIX;
        String lastSentTime = redisTemplate.opsForValue().get(rateLimitKey);

        if (Objects.nonNull(lastSentTime)) {
            try {
                long lastSentMillis = Long.parseLong(lastSentTime);
                long currentTimeMillis = System.currentTimeMillis();
                long timeDiffSeconds = (currentTimeMillis - lastSentMillis) / 1000;

                if (timeDiffSeconds < OtpConstants.OTP_RATE_LIMIT_SECONDS) {
                    LOGGER.warn("OTP rate limit exceeded for key: {}. Last sent {} seconds ago", key, timeDiffSeconds);
                    return OtpGenerationResult.rateLimited();
                }
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid timestamp format in Redis for key: {}", rateLimitKey);
                // Continue with OTP generation if timestamp is corrupted
            }
        }

        String attemptsKey = OtpConstants.OTP_REDIS_KEY_PREFIX + key + OtpConstants.ATTEMPTS_KEY_SUFFIX;

        Long attempts = redisTemplate.opsForValue().increment(attemptsKey, 1);
        if (attempts == 1) {
            redisTemplate.expire(attemptsKey, otpProperties.getAttemptWindowMinutes(), TimeUnit.MINUTES);
        }
        if (attempts > otpProperties.getMaxAttempts()) {
            LOGGER.warn("OTP request limit exceeded for key: {}", key);
            notifyLockout(key, userEmail);
            return OtpGenerationResult.maxAttemptsExceeded();
        }

        Integer otpValue = otpGenerator.generateOTP(key);
        if (otpValue == -1)
        {
            LOGGER.error("OTP generator returned error code for key: {}", key);
            notifyDeliveryFailure(userEmail, key);
            return OtpGenerationResult.deliveryFailed();
        }

        LOGGER.debug("Generated OTP for key: {}", key);

        if (Objects.isNull(userEmail) || userEmail.isBlank()) {
            LOGGER.error(EmailConstants.NO_EMAIL_FOR_USERNAME_MESSAGE, key);
            notifyDeliveryFailure(null, key);
            return OtpGenerationResult.deliveryFailed();
        }

        List<String> recipients = new ArrayList<>();
        recipients.add(userEmail);

        EmailDTO emailDTO = new EmailDTO();
        emailDTO.setSubject(EmailConstants.OTP_EMAIL_SUBJECT);
        emailDTO.setBody(EmailConstants.OTP_EMAIL_BODY_PREFIX + otpValue);
        emailDTO.setRecipients(recipients);

        final String otpKey = key;
        final String recipientEmail = userEmail;

        try {
            // Send email asynchronously without blocking
            CompletableFuture<Boolean> emailDispatch = emailService.sendSimpleMessageAsync(emailDTO);
            emailDispatch.thenAccept(emailSent -> {
                if (!Boolean.TRUE.equals(emailSent)) {
                    LOGGER.error("Failed to send OTP email to: {}", recipientEmail);
                    notifyDeliveryFailure(recipientEmail, otpKey);
                } else {
                    LOGGER.debug("OTP email sent successfully to: {}", recipientEmail);
                }
            }).exceptionally(ex -> {
                LOGGER.error("Error dispatching OTP email to: {}", userEmail, ex);
                notifyDeliveryFailure(userEmail, key);
                return null;
            });
        } catch (Exception e) {
            LOGGER.error("Error initiating OTP email dispatch to: {}", userEmail, e);
            notifyDeliveryFailure(userEmail, key);
            return OtpGenerationResult.deliveryFailed();
        }

        persistAuditEntry(key);

        // Store timestamp for rate limiting (expires in 15 seconds + buffer)
        redisTemplate.opsForValue().set(rateLimitKey, String.valueOf(System.currentTimeMillis()));
        redisTemplate.expire(rateLimitKey, OtpConstants.OTP_RATE_LIMIT_SECONDS + 5, TimeUnit.SECONDS);

        return OtpGenerationResult.success();
    }

    private void notifyLockout(String key, String userEmail) {
        if (Objects.isNull(userEmail) || userEmail.isBlank()) {
            LOGGER.debug("Skipping lockout notification for key {} due to missing email", key);
            return;
        }
        sendSystemNotification(userEmail, EmailConstants.OTP_LOCKED_EMAIL_SUBJECT, OtpConstants.MAX_ATTEMPTS_EXCEEDED_MESSAGE);
    }

    private void notifyDeliveryFailure(String userEmail, String key) {
        if (Objects.isNull(userEmail) || userEmail.isBlank()) {
            LOGGER.debug("Skipping delivery failure notification for key {} due to missing email", key);
            return;
        }
        sendSystemNotification(userEmail, EmailConstants.OTP_DELIVERY_FAILURE_SUBJECT, String.format(OtpConstants.OTP_DELIVERY_FAILURE_MESSAGE_TEMPLATE, key));
    }

    private void sendSystemNotification(String recipient, String subject, String messageBody) {
        EmailDTO systemEmail = new EmailDTO();
        systemEmail.setSubject(subject);
        systemEmail.setBody(messageBody);
        systemEmail.setRecipients(List.of(recipient));
        try {
            CompletableFuture<Boolean> notificationDispatch = emailService.sendSimpleMessageAsync(systemEmail);
            notificationDispatch.thenAccept(sent -> {
                if (!Boolean.TRUE.equals(sent)) {
                    LOGGER.warn("System notification email '{}' to {} failed", subject, recipient);
                }
            }).exceptionally(ex -> {
                LOGGER.warn("System notification email '{}' to {} failed", subject, recipient, ex);
                return null;
            });
        } catch (Exception ex) {
            LOGGER.warn("Failed to dispatch system notification email '{}' to {}", subject, recipient, ex);
        }
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
    @Override
    public OtpValidationResult validateOTP(String key, Integer otpNumber) {
        if (Objects.isNull(otpNumber)) {
            return OtpValidationResult.invalid();
        }
        OtpValidationResult result = otpGenerator.validateOtpStatus(key, otpNumber);
        if (result.getStatus() == OtpValidationStatus.SUCCESS) {
            // Reset attempts counter on successful validation
            String attemptsKey = OtpConstants.OTP_REDIS_KEY_PREFIX + key + OtpConstants.ATTEMPTS_KEY_SUFFIX;
            redisTemplate.delete(attemptsKey);
            LOGGER.debug("Reset attempts counter for key: {}", key);
            return OtpValidationResult.success();
        }
        if (result.getStatus() == OtpValidationStatus.LOCKED) {
            return OtpValidationResult.locked();
        }
        return OtpValidationResult.invalid();
    }
}
