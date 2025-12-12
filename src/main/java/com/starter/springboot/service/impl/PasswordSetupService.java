package com.starter.springboot.service.impl;

import com.starter.springboot.constants.EmailConstants;
import com.starter.springboot.dto.EmailDTO;
import com.starter.springboot.dto.OtpGenerationResult;
import com.starter.springboot.dto.SetPasswordDTO;
import com.starter.springboot.dto.SetPasswordResponseDTO;
import com.starter.springboot.entity.User;
import com.starter.springboot.repository.UserRepository;
import com.starter.springboot.service.IEmailService;
import com.starter.springboot.service.IPasswordSetupService;
import com.starter.springboot.service.IOtpRateLimiter;
import com.starter.springboot.service.ITemporaryPasswordTokenService;
import com.starter.springboot.service.PasswordValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class PasswordSetupService implements IPasswordSetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordSetupService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidationService passwordValidationService;
    private final IEmailService emailService;
    private final ITemporaryPasswordTokenService temporaryPasswordTokenService;
    private final IOtpRateLimiter otpRateLimiter;

    public PasswordSetupService(UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                PasswordValidationService passwordValidationService,
                                IEmailService emailService,
                                ITemporaryPasswordTokenService temporaryPasswordTokenService,
                                IOtpRateLimiter otpRateLimiter) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordValidationService = passwordValidationService;
        this.emailService = emailService;
        this.temporaryPasswordTokenService = temporaryPasswordTokenService;
        this.otpRateLimiter = otpRateLimiter;
    }

    @Override
    @Transactional
    public ResponseEntity<SetPasswordResponseDTO> handlePasswordSetWithAuthentication(SetPasswordDTO request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            LOGGER.warn("Unauthorized attempt to set password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(SetPasswordResponseDTO.failed("User not authenticated"));
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            LOGGER.warn("User not found: {}", username);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(SetPasswordResponseDTO.failed("User not found", username));
        }

        return updateUserPasswordWithValidation(user, request.getPassword());
    }

    @Override
    @Transactional
    public ResponseEntity<SetPasswordResponseDTO> handlePasswordSetWithTemporaryToken(SetPasswordDTO request) {
        if (!StringUtils.hasText(request.getTemporaryToken())) {
            LOGGER.warn("Temporary token not provided in set password request");
            return ResponseEntity.badRequest()
                .body(SetPasswordResponseDTO.failed("Temporary token is required"));
        }
        
        try {
            String username = temporaryPasswordTokenService.getUsernameFromToken(request.getTemporaryToken());
            if (!StringUtils.hasText(username)) {
                LOGGER.warn("Invalid or expired temporary token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(SetPasswordResponseDTO.failed("Temporary token expired or invalid"));
            }
            OtpGenerationResult rateLimitCheck = otpRateLimiter.checkAndIncrementAttempts("password-set:" + username);
            if (rateLimitCheck != null) {
                LOGGER.warn("Rate limit exceeded for password set attempts");
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(SetPasswordResponseDTO.failed("Too many password reset attempts. Please try again later"));
            }

            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                LOGGER.warn("User not found for temporary token validation: {}", username);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(SetPasswordResponseDTO.failed("User not found"));
            }

            if (!temporaryPasswordTokenService.validateTemporaryToken(username, request.getTemporaryToken())) {
                LOGGER.warn("Invalid temporary token for user: {}", username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(SetPasswordResponseDTO.failed("Temporary token expired or invalid", username));
            }

            ResponseEntity<SetPasswordResponseDTO> response = updateUserPasswordWithValidation(user, request.getPassword());
            if (response.getStatusCode() == HttpStatus.OK) {
                temporaryPasswordTokenService.invalidateTemporaryToken(username);
                otpRateLimiter.resetAttempts("password-set:" + username);
            }
            return response;

        } catch (Exception e) {
            LOGGER.error("Error processing password set with temporary token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(SetPasswordResponseDTO.failed("Failed to set password. Please try again."));
        }
    }

    @Override
    public ResponseEntity<SetPasswordResponseDTO> updateUserPasswordWithValidation(User user, String newPassword) {
        String username = user.getUsername();

        if (Boolean.TRUE.equals(user.getPasswordSet())) {
            LOGGER.warn("User {} already has password set", username);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(SetPasswordResponseDTO.failed("Password already set", username));
        }

        PasswordValidationService.PasswordValidationResult validationResult = 
            passwordValidationService.validate(newPassword);
        
        if (!validationResult.valid()) {
            LOGGER.warn("Password validation failed for user {}: {}", username, validationResult.message());
            return ResponseEntity.badRequest()
                .body(SetPasswordResponseDTO.failed(validationResult.message(), username));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordSet(Boolean.TRUE);
        userRepository.save(user);

        sendPasswordSetNotification(user);

        LOGGER.info("Password set successfully for user: {}", username);
        return ResponseEntity.ok(SetPasswordResponseDTO.success(username));
    }

    @Override
    public void sendPasswordSetNotification(User user) {
        try {
            EmailDTO emailDTO = new EmailDTO();
            emailDTO.setRecipients(List.of(user.getEmail()));
            emailDTO.setSubject(EmailConstants.PASSWORD_SET_SUBJECT);

            String content = String.format(EmailConstants.PASSWORD_SET_CONTENT_TEMPLATE,
                    (user.getFirstName() != null ? user.getFirstName() : user.getUsername()));

            String wrapped = String.format(EmailConstants.EMAIL_WRAPPER_TEMPLATE,
                    EmailConstants.PASSWORD_SET_SUBJECT, content);

            emailDTO.setBody(wrapped);
            emailDTO.setHtml(Boolean.TRUE);

            emailService.sendHtmlMessageAsync(emailDTO)
                    .thenAccept(sent -> {
                        if (Boolean.TRUE.equals(sent)) {
                            LOGGER.info("Password set notification sent to: {}", user.getEmail());
                        } else {
                            LOGGER.warn("Password set notification to {} was not sent successfully", user.getEmail());
                        }
                    }).exceptionally(ex -> {
                        LOGGER.error("Error sending password set notification to {}: {}", user.getEmail(), ex.getMessage());
                        return null;
                    });

        } catch (Exception e) {
            LOGGER.warn("Failed to send password set notification for user {}: {}", user.getUsername(), e.getMessage());
        }
    }

    /**
     * Send an HTML activation email containing a link with the temporary token.
     * This uses the IEmailService.sendHtmlMessage implementation.
     */
    @Override
    public void sendPasswordActivationNotification(User user, String temporaryToken) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            LOGGER.debug("Skipping password activation email because user or email is missing");
            return;
        }

        try {
            EmailDTO emailDTO = new EmailDTO();
            emailDTO.setRecipients(List.of(user.getEmail()));
            emailDTO.setSubject(EmailConstants.PASSWORD_ACTIVATION_SUBJECT);

            String url = String.format(EmailConstants.ACTIVATION_URL_TEMPLATE, temporaryToken);

            String content = String.format(EmailConstants.ACTIVATION_CONTENT_TEMPLATE,
                    (user.getFirstName() != null ? user.getFirstName() : user.getUsername()), url, url);

            String wrapped = String.format(EmailConstants.EMAIL_WRAPPER_TEMPLATE,
                    EmailConstants.PASSWORD_ACTIVATION_SUBJECT, content);

            emailDTO.setBody(wrapped);
            emailDTO.setHtml(Boolean.TRUE);

            LOGGER.debug("Initiating async password activation email to {}", user.getEmail());

            // Send asynchronously to avoid blocking the request flow
            try {
                emailService.sendHtmlMessageAsync(emailDTO)
                    .thenAccept(sent -> {
                        if (Boolean.TRUE.equals(sent)) {
                            LOGGER.info("Sent password activation email to {}", user.getEmail());
                        } else {
                            LOGGER.warn("Password activation email to {} was not sent successfully", user.getEmail());
                        }
                    })
                    .exceptionally(ex -> {
                        LOGGER.error("Error while sending password activation email to {}: {}", user.getEmail(), ex.getMessage());
                        return null;
                    });
            } catch (Exception ex) {
                LOGGER.error("Failed to initiate async password activation email for {}: {}", user.getEmail(), ex.getMessage());
            }

        } catch (Exception e) {
            LOGGER.error("Failed to prepare password activation email for {}: {}", user.getEmail(), e.getMessage());
        }
    }
}
