package com.starter.springboot.service.impl;

import com.starter.springboot.constants.EmailConstants;
import com.starter.springboot.dto.EmailDTO;
import com.starter.springboot.service.IEmailService;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
public class EmailService implements IEmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender emailSender;

    public EmailService(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    /**
     * Method for sending simple e-mail message.
     * @param emailDTO - data to be sent.
     */
    @Override
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
    @Override
    @Async
    public CompletableFuture<Boolean> sendSimpleMessageAsync(EmailDTO emailDTO) {
        return CompletableFuture.completedFuture(sendSimpleMessage(emailDTO));
    }

    @Override
    public Boolean sendHtmlMessage(EmailDTO emailDTO) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            // multipart = true
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(emailDTO.getRecipients().toArray(new String[0]));
            helper.setSubject(emailDTO.getSubject());

            String html = emailDTO.getBody() == null ? "" : emailDTO.getBody();
            // Create a simple plain-text fallback by stripping tags (best-effort)
            String plain = html.replaceAll("\\<[^>]*\\>", "");

            // Set both plain-text and HTML parts so clients properly render HTML
            helper.setText(plain, html);

            if (emailDTO.getCcList() != null) {
                helper.setCc(emailDTO.getCcList().toArray(new String[0]));
            }
            if (emailDTO.getBccList() != null) {
                helper.setBcc(emailDTO.getBccList().toArray(new String[0]));
            }

            emailSender.send(message);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to send HTML email: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Asynchronously send an HTML e-mail message while reusing synchronous implementation.
     * @param emailDTO - data to be sent.
     */
    @Override
    @Async
    public CompletableFuture<Boolean> sendHtmlMessageAsync(EmailDTO emailDTO) {
        try {
            boolean sent = sendHtmlMessage(emailDTO);
            return CompletableFuture.completedFuture(sent);
        } catch (Exception e) {
            LOGGER.error("sendHtmlMessageAsync failed: {}", e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }
}
