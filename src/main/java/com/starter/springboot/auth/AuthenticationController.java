package com.starter.springboot.auth;


import com.starter.springboot.constants.ApplicationConstants;
import com.starter.springboot.constants.OtpConstants;
import com.starter.springboot.constants.SecurityConstants;
import com.starter.springboot.exceptions.OtpRequiredException;
import com.starter.springboot.rest.dto.AuthResponseDTO;
import com.starter.springboot.rest.dto.LoginDTO;
import com.starter.springboot.rest.dto.VerifyTokenRequestDTO;
import com.starter.springboot.security.jwt.JWTToken;
import com.starter.springboot.security.jwt.TokenCreationResponse;
import com.starter.springboot.security.jwt.TokenProvider;
import com.starter.springboot.services.OtpService;
import com.starter.springboot.services.dto.OtpValidationResult;
import com.starter.springboot.services.dto.OtpValidationStatus;
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
@RequestMapping(ApplicationConstants.AUTH_ENDPOINT)
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

    @PostMapping(value = ApplicationConstants.AUTHENTICATE_ENDPOINT)
    public ResponseEntity<AuthResponseDTO> authorize(@Valid @RequestBody LoginDTO loginDTO) {
        LOGGER.info("Authentication attempt for user: {}", loginDTO.getUsername());

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
            loginDTO.getUsername(), loginDTO.getPassword()
        );
        try {
            Authentication authentication = this.authenticationManager.authenticate(authenticationToken);
            TokenCreationResponse createResponse = tokenProvider.createToken(authentication, loginDTO.getRememberMe());

            SecurityContextHolder.getContext().setAuthentication(authentication);

            LOGGER.info("Authentication completed for user: {}", loginDTO.getUsername());
            AuthResponseDTO response = AuthResponseDTO.fromTokenCreation(loginDTO.getUsername(), createResponse)
                .withContext(loginDTO.getRememberMe(), loginDTO.getClientId(), loginDTO.getDeviceId());
            return ResponseEntity
                .status(createResponse.status())
                .body(response);
        } catch (OtpRequiredException ex) {
            LOGGER.info("OTP required for user: {}", loginDTO.getUsername());
            throw ex;
        } catch (BadCredentialsException badCredentialsException) {
            LOGGER.warn("Authentication failed for user: {} due to bad credentials", loginDTO.getUsername());
            AuthResponseDTO body = AuthResponseDTO.failed(loginDTO.getUsername(), SecurityConstants.INVALID_CREDENTIALS_MESSAGE).withContext(loginDTO.getRememberMe(), loginDTO.getClientId(), loginDTO.getDeviceId());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        } catch (AuthenticationException exception) {
            LOGGER.warn("Authentication failed for user: {}: {}", loginDTO.getUsername(), exception.getMessage());
            AuthResponseDTO body = AuthResponseDTO.failed(loginDTO.getUsername(), exception.getMessage()).withContext(loginDTO.getRememberMe(), loginDTO.getClientId(), loginDTO.getDeviceId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
        }
    }

    @PostMapping(value = ApplicationConstants.VERIFY_ENDPOINT)
    public ResponseEntity<AuthResponseDTO> verifyOtp(@Valid @RequestBody VerifyTokenRequestDTO verifyTokenRequest) {
        String username = verifyTokenRequest.getUsername();
        Integer otp = verifyTokenRequest.getOtp();
        Boolean rememberMe = verifyTokenRequest.getRememberMe();

        OtpValidationResult validationResult = otpService.validateOTP(username, otp);
        if (!validationResult.isSuccess()) {
            LOGGER.warn("OTP validation failed for user: {} with status {}", username, validationResult.getStatus());
            HttpStatus status = validationResult.getStatus() == OtpValidationStatus.LOCKED ? HttpStatus.LOCKED : HttpStatus.UNAUTHORIZED;
            String message = validationResult.getStatus() == OtpValidationStatus.LOCKED
                ? OtpConstants.LOCKED_OTP_MESSAGE
                : OtpConstants.INVALID_OTP_MESSAGE;
            return ResponseEntity.status(status)
                .body(AuthResponseDTO.failed(username, message)
                    .withContext(rememberMe, verifyTokenRequest.getClientId(), verifyTokenRequest.getDeviceId()));
        }

        JWTToken token = tokenProvider.createTokenAfterVerifiedOtp(username, rememberMe);
        AuthResponseDTO response = AuthResponseDTO.success(username, token, rememberMe)
            .withContext(rememberMe, verifyTokenRequest.getClientId(), verifyTokenRequest.getDeviceId());

        LOGGER.info("OTP verified successfully for user: {}", username);
        return ResponseEntity.ok(response);
    }
}
