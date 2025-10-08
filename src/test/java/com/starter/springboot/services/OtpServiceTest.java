package com.starter.springboot.services;

import com.starter.springboot.rest.dto.EmailDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private OtpGenerator otpGenerator;

    @Mock
    private EmailService emailService;

    @Mock
    private UserService userService;

    @Mock
    private Logger logger;

    @Captor
    private ArgumentCaptor<EmailDTO> emailCaptor;

    private OtpService otpService;

    @BeforeEach
    void setUp() {
        otpService = new OtpService(otpGenerator, emailService, userService);
    }

    @Test
    void generateOtpSendsEmailWhenOtpCreated() {
        when(otpGenerator.generateOTP("john.doe")).thenReturn(123456);
        when(userService.findEmailByUsername("john.doe")).thenReturn("john.doe@example.com");
        when(emailService.sendSimpleMessage(any(EmailDTO.class))).thenReturn(Boolean.TRUE);

        Boolean result = otpService.generateOtp("john.doe");

        assertTrue(result);

        verify(otpGenerator).generateOTP("john.doe");
        verify(userService).findEmailByUsername("john.doe");
        verify(emailService).sendSimpleMessage(emailCaptor.capture());

        EmailDTO emailDTO = emailCaptor.getValue();
        assertEquals(Collections.singletonList("john.doe@example.com"), emailDTO.getRecipients());
        assertEquals("Spring Boot OTP Password.", emailDTO.getSubject());
        assertEquals("OTP Password: 123456", emailDTO.getBody());
    }

    @Test
    void generateOtpReturnsFalseWhenGeneratorFails() {
        when(otpGenerator.generateOTP("john.doe")).thenReturn(-1);

        Boolean result = otpService.generateOtp("john.doe");

        assertFalse(result);

        verify(otpGenerator).generateOTP("john.doe");
        verify(emailService, never()).sendSimpleMessage(any());
        verifyNoMoreInteractions(userService);
    }

    @Test
    void validateOtpReturnsTrueWhenOtpMatches() {
        when(otpGenerator.getOPTByKey("john.doe")).thenReturn(123456);

        Boolean result = otpService.validateOTP("john.doe", 123456);

        assertTrue(result);

        verify(otpGenerator).getOPTByKey("john.doe");
        verify(otpGenerator).clearOTPFromCache("john.doe");
    }

    @Test
    void validateOtpReturnsFalseWhenOtpDoesNotMatch() {
        when(otpGenerator.getOPTByKey("john.doe")).thenReturn(111111);

        Boolean result = otpService.validateOTP("john.doe", 123456);

        assertFalse(result);

        verify(otpGenerator).getOPTByKey("john.doe");
        verify(otpGenerator, never()).clearOTPFromCache("john.doe");
    }

    @Test
    void validateOtpReturnsFalseWhenNoOtpPresent() {
        when(otpGenerator.getOPTByKey("john.doe")).thenReturn(null);

        Boolean result = otpService.validateOTP("john.doe", 123456);

        assertFalse(result);

        verify(otpGenerator).getOPTByKey("john.doe");
        verify(otpGenerator, never()).clearOTPFromCache("john.doe");
    }
}