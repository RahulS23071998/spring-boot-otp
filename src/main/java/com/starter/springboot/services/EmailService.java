package com.starter.springboot.services;

import com.starter.springboot.constants.EmailConstants;
import com.starter.springboot.rest.dto.EmailDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
public class EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender emailSender;

    public EmailService(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    /**
     * Method for sending simple e-mail message.
     * @param emailDTO - data to be sent.
     */
    public Boolean sendSimpleMessage(EmailDTO emailDTO)
    {
        if (Objects.isNull(emailDTO.getRecipients()) || emailDTO.getRecipients().isEmpty()) {
            LOGGER.error(EmailConstants.NO_RECIPIENTS_PROVIDED_MESSAGE, emailDTO.getSubject());
            return false;
        }

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(emailDTO.getRecipients().toArray(new String[0]));
        mailMessage.setSubject(emailDTO.getSubject());
        mailMessage.setText(emailDTO.getBody());

        try
        {
            emailSender.send(mailMessage);
            LOGGER.info(EmailConstants.EMAIL_SENT_SUCCESS_MESSAGE, String.join(",", emailDTO.getRecipients()));
            return true;
        }
        catch (Exception e) {
            LOGGER.error(EmailConstants.EMAIL_SENDING_ERROR_MESSAGE, e.getMessage());
            return false;
        }
    }

    /**
     * Asynchronously send a simple e-mail message while reusing synchronous implementation.
     * @param emailDTO - data to be sent.
     */
    @Async
    public CompletableFuture<Boolean> sendSimpleMessageAsync(EmailDTO emailDTO) {
        return CompletableFuture.completedFuture(sendSimpleMessage(emailDTO));
    }
}
