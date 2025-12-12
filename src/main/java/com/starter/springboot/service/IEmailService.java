package com.starter.springboot.service;

import com.starter.springboot.dto.EmailDTO;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for Email service operations.
 * Provides contract for sending email messages.
 */
public interface IEmailService {

    /**
     * Method for sending simple e-mail message.
     * @param emailDTO - data to be sent.
     */
    Boolean sendSimpleMessage(EmailDTO emailDTO);

    /**
     * Asynchronously send a simple e-mail message while reusing synchronous implementation.
     * @param emailDTO - data to be sent.
     */
    CompletableFuture<Boolean> sendSimpleMessageAsync(EmailDTO emailDTO);

    /**
     * Send an HTML e-mail message (synchronous).
     */
    Boolean sendHtmlMessage(EmailDTO emailDTO);

    /**
     * Send an HTML e-mail message asynchronously.
     */
    CompletableFuture<Boolean> sendHtmlMessageAsync(EmailDTO emailDTO);
}