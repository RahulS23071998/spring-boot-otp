package com.starter.springboot.service;

import com.starter.springboot.dto.EmailDTO;
import com.starter.springboot.service.impl.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Tests")
class EmailServiceTest {

    @Mock
    private JavaMailSender emailSender;

    @InjectMocks
    private EmailService emailService;

    private EmailDTO emailDTO;

    @BeforeEach
    void setUp() {
        emailDTO = new EmailDTO();
    }

    @Test
    @DisplayName("Should send email successfully with valid data")
    void shouldSendEmailSuccessfullyWithValidData() {
        // Given
        List<String> recipients = Arrays.asList("test@example.com", "user@example.com");
        emailDTO.setRecipients(recipients);
        emailDTO.setSubject("Test Subject");
        emailDTO.setBody("Test Body");

        doNothing().when(emailSender).send(any(SimpleMailMessage.class));

        // When
        Boolean result = emailService.sendSimpleMessage(emailDTO);

        // Then
        assertTrue(result);
        verify(emailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should return false when recipients list is null")
    void shouldReturnFalseWhenRecipientsListIsNull() {
        // Given
        emailDTO.setRecipients(null);
        emailDTO.setSubject("Test Subject");
        emailDTO.setBody("Test Body");

        // When
        Boolean result = emailService.sendSimpleMessage(emailDTO);

        // Then
        assertFalse(result);
        verify(emailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should return false when recipients list is empty")
    void shouldReturnFalseWhenRecipientsListIsEmpty() {
        // Given
        emailDTO.setRecipients(Collections.emptyList());
        emailDTO.setSubject("Test Subject");
        emailDTO.setBody("Test Body");

        // When
        Boolean result = emailService.sendSimpleMessage(emailDTO);

        // Then
        assertFalse(result);
        verify(emailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should return false when email sending throws exception")
    void shouldReturnFalseWhenEmailSendingThrowsException() {
        // Given
        List<String> recipients = List.of("test@example.com");
        emailDTO.setRecipients(recipients);
        emailDTO.setSubject("Test Subject");
        emailDTO.setBody("Test Body");

        doThrow(new MailException("Mail server error") {}).when(emailSender).send(any(SimpleMailMessage.class));

        // When
        Boolean result = emailService.sendSimpleMessage(emailDTO);

        // Then
        assertFalse(result);
        verify(emailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should create correct SimpleMailMessage with single recipient")
    void shouldCreateCorrectSimpleMailMessageWithSingleRecipient() {
        // Given
        List<String> recipients = List.of("test@example.com");
        emailDTO.setRecipients(recipients);
        emailDTO.setSubject("Test Subject");
        emailDTO.setBody("Test Body");

        doNothing().when(emailSender).send(any(SimpleMailMessage.class));

        // When
        emailService.sendSimpleMessage(emailDTO);

        // Then
        verify(emailSender).send(argThat((SimpleMailMessage message) -> Arrays.equals(message.getTo(), new String[]{"test@example.com"}) &&
               "Test Subject".equals(message.getSubject()) &&
               "Test Body".equals(message.getText())));
    }

    @Test
    @DisplayName("Should create correct SimpleMailMessage with multiple recipients")
    void shouldCreateCorrectSimpleMailMessageWithMultipleRecipients() {
        // Given
        List<String> recipients = Arrays.asList("test1@example.com", "test2@example.com", "test3@example.com");
        emailDTO.setRecipients(recipients);
        emailDTO.setSubject("Multiple Recipients Test");
        emailDTO.setBody("Body for multiple recipients");

        doNothing().when(emailSender).send(any(SimpleMailMessage.class));

        // When
        emailService.sendSimpleMessage(emailDTO);

        // Then
        verify(emailSender).send(argThat((SimpleMailMessage message) -> {
            String[] expectedRecipients = {"test1@example.com", "test2@example.com", "test3@example.com"};
            return Arrays.equals(message.getTo(), expectedRecipients) &&
                   "Multiple Recipients Test".equals(message.getSubject()) &&
                   "Body for multiple recipients".equals(message.getText());
        }));
    }

    @Test
    @DisplayName("Should handle null subject")
    void shouldHandleNullSubject() {
        // Given
        List<String> recipients = List.of("test@example.com");
        emailDTO.setRecipients(recipients);
        emailDTO.setSubject(null);
        emailDTO.setBody("Test Body");

        doNothing().when(emailSender).send(any(SimpleMailMessage.class));

        // When
        Boolean result = emailService.sendSimpleMessage(emailDTO);

        // Then
        assertTrue(result);
        verify(emailSender).send(argThat((SimpleMailMessage message) -> message.getSubject() == null &&
               "Test Body".equals(message.getText())));
    }

    @Test
    @DisplayName("Should handle null body")
    void shouldHandleNullBody() {
        // Given
        List<String> recipients = List.of("test@example.com");
        emailDTO.setRecipients(recipients);
        emailDTO.setSubject("Test Subject");
        emailDTO.setBody(null);

        doNothing().when(emailSender).send(any(SimpleMailMessage.class));

        // When
        Boolean result = emailService.sendSimpleMessage(emailDTO);

        // Then
        assertTrue(result);
        verify(emailSender).send(argThat((SimpleMailMessage message) -> "Test Subject".equals(message.getSubject()) &&
               message.getText() == null));
    }

    @Test
    @DisplayName("Should handle empty subject and body")
    void shouldHandleEmptySubjectAndBody() {
        // Given
        List<String> recipients = List.of("test@example.com");
        emailDTO.setRecipients(recipients);
        emailDTO.setSubject("");
        emailDTO.setBody("");

        doNothing().when(emailSender).send(any(SimpleMailMessage.class));

        // When
        Boolean result = emailService.sendSimpleMessage(emailDTO);

        // Then
        assertTrue(result);
        verify(emailSender).send(argThat((SimpleMailMessage message) -> "".equals(message.getSubject()) &&
               "".equals(message.getText())));
    }

    @Test
    @DisplayName("Should handle runtime exception during email sending")
    void shouldHandleRuntimeExceptionDuringEmailSending() {
        // Given
        List<String> recipients = List.of("test@example.com");
        emailDTO.setRecipients(recipients);
        emailDTO.setSubject("Test Subject");
        emailDTO.setBody("Test Body");

        doThrow(new RuntimeException("Unexpected error")).when(emailSender).send(any(SimpleMailMessage.class));

        // When
        Boolean result = emailService.sendSimpleMessage(emailDTO);

        // Then
        assertFalse(result);
        verify(emailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}