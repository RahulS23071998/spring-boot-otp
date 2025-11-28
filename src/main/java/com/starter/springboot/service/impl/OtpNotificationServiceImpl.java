package com.starter.springboot.service.impl;

import com.starter.springboot.constants.EmailConstants;
import com.starter.springboot.constants.OtpConstants;
import com.starter.springboot.dto.EmailDTO;
import com.starter.springboot.service.IEmailService;
import com.starter.springboot.service.IOtpNotificationService;
import com.starter.springboot.service.LocalizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of OTP notification service.
 */
@Service
public class OtpNotificationServiceImpl implements IOtpNotificationService {

    private final Logger LOGGER = LoggerFactory.getLogger(OtpNotificationServiceImpl.class);

    private final IEmailService emailService;

    private final LocalizationService localizationService;

    public OtpNotificationServiceImpl(IEmailService emailService, LocalizationService localizationService) {
        this.emailService = emailService;
        this.localizationService = localizationService;
    }

    @Override
    public void notifyLockout(String key, String userEmail) {
        if (Objects.isNull(userEmail) || userEmail.isBlank()) {
            LOGGER.debug("Skipping lockout notification for key {} due to missing email", key);
            return;
        }
        sendSystemNotification(userEmail, EmailConstants.OTP_LOCKED_EMAIL_SUBJECT, localizationService.getMessage("auth.max_attempts_exceeded"));
    }

    @Override
    public void notifyDeliveryFailure(String userEmail, String key) {
        if (Objects.isNull(userEmail) || userEmail.isBlank()) {
            LOGGER.debug("Skipping delivery failure notification for key {} due to missing email", key);
            return;
        }
        sendSystemNotification(userEmail, EmailConstants.OTP_DELIVERY_FAILURE_SUBJECT, localizationService.getMessage("auth.otp_delivery_failure", key));
    }

    @Override
    public CompletableFuture<Boolean> sendOtpEmailAsync(String key, String userEmail, Integer otpValue) {
        if (Objects.isNull(userEmail) || userEmail.isBlank()) {
            LOGGER.error(EmailConstants.NO_EMAIL_FOR_USERNAME_MESSAGE, key);
            return CompletableFuture.completedFuture(false);
        }

        List<String> recipients = List.of(userEmail);
        EmailDTO emailDTO = new EmailDTO();
        emailDTO.setSubject(EmailConstants.OTP_EMAIL_SUBJECT);
        emailDTO.setBody(EmailConstants.OTP_EMAIL_BODY_PREFIX + otpValue);
        emailDTO.setRecipients(recipients);

        try {
            return emailService.sendSimpleMessageAsync(emailDTO);
        } catch (Exception e) {
            LOGGER.error("Error initiating OTP email dispatch to: {}", userEmail, e);
            return CompletableFuture.completedFuture(false);
        }
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
}