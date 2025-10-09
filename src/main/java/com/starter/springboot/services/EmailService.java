package com.starter.springboot.services;

import com.starter.springboot.rest.dto.EmailDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class EmailService {

    private final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender emailSender;

    public EmailService(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    /**
     * Method for sending simple e-mail message.
     * @param emailDTO - data to be send.
     */
    public Boolean sendSimpleMessage(EmailDTO emailDTO)
    {
        if (emailDTO.getRecipients() == null || emailDTO.getRecipients().isEmpty()) {
            LOGGER.error("No recipients provided for email with subject: {}", emailDTO.getSubject());
            return false;
        }

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(emailDTO.getRecipients().toArray(new String[0]));
        mailMessage.setSubject(emailDTO.getSubject());
        mailMessage.setText(emailDTO.getBody());

        try
        {
            emailSender.send(mailMessage);
            LOGGER.info("Email successfully sent to: {}", String.join(",", emailDTO.getRecipients()));
            return true;
        }
        catch (Exception e) {
            LOGGER.error("Sending e-mail error: {}", e.getMessage(), e);
            return false;
        }
    }


}
