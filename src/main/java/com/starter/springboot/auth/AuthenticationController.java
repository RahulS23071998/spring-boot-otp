package com.starter.springboot.auth;


import com.starter.springboot.exceptions.OtpRequiredException;
import com.starter.springboot.rest.dto.LoginDTO;
import com.starter.springboot.rest.dto.VerifyTokenRequestDTO;
import com.starter.springboot.security.jwt.JWTToken;
import com.starter.springboot.security.jwt.TokenCreationResponse;
import com.starter.springboot.security.jwt.TokenProvider;
import com.starter.springboot.services.OtpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@Validated
public class AuthenticationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationController.class);

    private final TokenProvider tokenProvider;

    private final OtpService otpService;

    private final AuthenticationManager authenticationManager;

    public AuthenticationController(TokenProvider tokenProvider,
                                    OtpService otpService,
                                    AuthenticationManager authenticationManager) {
        this.tokenProvider = tokenProvider;
        this.otpService = otpService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping(value = "/authenticate")
    public ResponseEntity<JWTToken> authorize(@Valid @RequestBody LoginDTO loginDTO) {
        LOGGER.info("Authentication attempt for user: {}", loginDTO.getUsername());

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
            loginDTO.getUsername(), loginDTO.getPassword()
        );
        try {
            Authentication authentication = this.authenticationManager.authenticate(authenticationToken);
            TokenCreationResponse createResponse = tokenProvider.createToken(authentication, loginDTO.isRememberMe());

            SecurityContextHolder.getContext().setAuthentication(authentication);

            LOGGER.info("Authentication successful for user: {}", loginDTO.getUsername());
            return ResponseEntity.status(createResponse.status()).body(createResponse.token());
        } catch (OtpRequiredException ex) {
            LOGGER.info("OTP required for user: {}", loginDTO.getUsername());
            throw ex;
        } catch (BadCredentialsException badCredentialsException) {
            LOGGER.warn("Authentication failed for user: {} due to bad credentials", loginDTO.getUsername());
            throw badCredentialsException;
        } catch (AuthenticationException exception) {
            LOGGER.error("Authentication failed for user: {}", loginDTO.getUsername(), exception);
            throw exception;
        }
    }

    @PostMapping(value = "/verify")
    public ResponseEntity<JWTToken> verifyOtp(@Valid @RequestBody VerifyTokenRequestDTO verifyTokenRequest) {
        String username = verifyTokenRequest.getUsername();
        Integer otp = verifyTokenRequest.getOtp();
        Boolean rememberMe = verifyTokenRequest.getRememberMe();

        boolean isOtpValid = otpService.validateOTP(username, otp);
        if (!isOtpValid) {
            LOGGER.warn("Invalid OTP submitted for user: {}", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = tokenProvider.createTokenAfterVerifiedOtp(username, rememberMe);
        JWTToken response = new JWTToken(token);

        LOGGER.info("OTP verified successfully for user: {}", username);
        return ResponseEntity.ok(response);
    }
}
