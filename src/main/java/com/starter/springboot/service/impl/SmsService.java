package com.starter.springboot.service.impl;

import com.starter.springboot.service.ISmsService;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class SmsService implements ISmsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmsService.class);

    @Value("${twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${twilio.auth-token:}")
    private String twilioAuthToken;

    @Value("${twilio.phone-number:}")
    private String twilioPhoneNumber;

    @Override
    public Boolean sendSms(String phoneNumber, String messageBody) {
        try {
            if (twilioAccountSid == null || twilioAccountSid.isEmpty() ||
                twilioAuthToken == null || twilioAuthToken.isEmpty()) {
                LOGGER.warn("Twilio credentials not configured. Skipping SMS.");
                return false;
            }

            Twilio.init(twilioAccountSid, twilioAuthToken);
            Message message = Message.creator(
                    new PhoneNumber(phoneNumber),
                    new PhoneNumber(twilioPhoneNumber),
                    messageBody)
                    .create();

            LOGGER.info("SMS sent successfully. SID: {}", message.getSid());
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to send SMS: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Async
    public CompletableFuture<Boolean> sendSmsAsync(String phoneNumber, String message) {
        return CompletableFuture.completedFuture(sendSms(phoneNumber, message));
    }
}
