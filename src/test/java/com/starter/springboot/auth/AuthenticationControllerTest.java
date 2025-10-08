package com.starter.springboot.auth;

import com.starter.springboot.rest.dto.LoginDTO;
import com.starter.springboot.rest.dto.VerifyTokenRequestDTO;
import com.starter.springboot.security.jwt.JWTToken;
import com.starter.springboot.security.jwt.TokenProvider;
import com.starter.springboot.services.OtpService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private OtpService otpService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private Authentication authentication;

    private AuthenticationController authenticationController;

    @BeforeEach
    void setUp() {
        authenticationController = new AuthenticationController(tokenProvider, otpService, authenticationManager);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authorizeReturnsTokenWhenAuthenticationSucceeds() {
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsername("john.doe");
        loginDTO.setPassword("strong-password");
        loginDTO.setRememberMe(Boolean.TRUE);

        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(tokenProvider.createToken(authentication, Boolean.TRUE)).thenReturn("jwt-token");

        ResponseEntity<JWTToken> response = authenticationController.authorize(loginDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("jwt-token", response.getBody().getIdToken());
        assertSame(authentication, SecurityContextHolder.getContext().getAuthentication());

        verify(authenticationManager).authenticate(argThat(auth -> {
            if (!(auth instanceof UsernamePasswordAuthenticationToken)) {
                return false;
            }
            UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) auth;
            return Objects.equals("john.doe", token.getPrincipal())
                && Objects.equals("strong-password", token.getCredentials());
        }));
        verify(tokenProvider).createToken(authentication, Boolean.TRUE);
    }

    @Test
    void authorizeReturnsUnauthorizedWhenAuthenticationFails() {
        when(authenticationManager.authenticate(any(Authentication.class)))
            .thenThrow(new BadCredentialsException("Bad credentials"));

        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsername("john.doe");
        loginDTO.setPassword("wrong-password");
        loginDTO.setRememberMe(Boolean.FALSE);

        ResponseEntity<JWTToken> response = authenticationController.authorize(loginDTO);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(tokenProvider, never()).createToken(any(Authentication.class), anyBoolean());
    }

    @Test
    void verifyOtpReturnsTokenWhenOtpValid() {
        when(otpService.validateOTP("john.doe", 123456)).thenReturn(Boolean.TRUE);
        when(tokenProvider.createTokenAfterVerifiedOtp("john.doe", Boolean.TRUE)).thenReturn("verified-token");

        VerifyTokenRequestDTO request = new VerifyTokenRequestDTO();
        request.setUsername("john.doe");
        request.setOtp(123456);
        request.setRememberMe(Boolean.TRUE);

        ResponseEntity<JWTToken> response = authenticationController.verifyOtp(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("verified-token", response.getBody().getIdToken());

        verify(otpService).validateOTP("john.doe", 123456);
        verify(tokenProvider).createTokenAfterVerifiedOtp("john.doe", Boolean.TRUE);
    }

    @Test
    void verifyOtpReturnsUnauthorizedWhenOtpInvalid() {
        when(otpService.validateOTP("john.doe", 654321)).thenReturn(Boolean.FALSE);

        VerifyTokenRequestDTO request = new VerifyTokenRequestDTO();
        request.setUsername("john.doe");
        request.setOtp(654321);
        request.setRememberMe(Boolean.FALSE);

        ResponseEntity<JWTToken> response = authenticationController.verifyOtp(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());

        verify(otpService).validateOTP("john.doe", 654321);
        verify(tokenProvider, never()).createTokenAfterVerifiedOtp(any(), anyBoolean());
    }
}