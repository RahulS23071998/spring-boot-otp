package com.starter.springboot.service.impl;

import com.starter.springboot.dto.OtpValidationResult;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TotpServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private GoogleAuthenticator googleAuthenticator;

    @InjectMocks
    private TotpService totpService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(totpService, "authenticator", googleAuthenticator);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Should generate TOTP secret successfully")
    void generateTotpSecret_Success() {
        GoogleAuthenticatorKey key = mock(GoogleAuthenticatorKey.class);
        when(key.getKey()).thenReturn("secret-key");
        when(googleAuthenticator.createCredentials()).thenReturn(key);

        String secret = totpService.generateTotpSecret("user");

        assertEquals("secret-key", secret);
        verify(googleAuthenticator).createCredentials();
    }

    @Test
    @DisplayName("Should generate QR code URL successfully")
    void generateQrCodeUrl_Success() {
        String username = "user@example.com";
        String secret = "secret-key";
        String issuer = "MyApp";

        String url = totpService.generateQrCodeUrl(username, secret, issuer);

        assertNotNull(url);
        assertTrue(url.contains("otpauth://totp/MyApp:user%40example.com"));
        assertTrue(url.contains("secret=secret-key"));
        assertTrue(url.contains("issuer=MyApp"));
    }

    @Test
    @DisplayName("Should return null when generating QR code URL fails")
    void generateQrCodeUrl_Failure() {
        String url = totpService.generateQrCodeUrl(null, "secret", "issuer");
        assertNull(url);
    }

    @Test
    @DisplayName("Should validate TOTP successfully")
    void validateTotp_Success() {
        String username = "user";
        String totpCode = "123456";
        String secret = "secret-key";

        when(valueOperations.get("totp:secret:" + username)).thenReturn(secret);
        when(googleAuthenticator.authorize(secret, 123456)).thenReturn(true);

        OtpValidationResult result = totpService.validateTotp(username, totpCode);

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Should fail validation when secret not found")
    void validateTotp_NoSecret() {
        String username = "user";
        String totpCode = "123456";

        when(valueOperations.get("totp:secret:" + username)).thenReturn(null);

        OtpValidationResult result = totpService.validateTotp(username, totpCode);

        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("Should fail validation when code is invalid")
    void validateTotp_InvalidCode() {
        String username = "user";
        String totpCode = "123456";
        String secret = "secret-key";

        when(valueOperations.get("totp:secret:" + username)).thenReturn(secret);
        when(googleAuthenticator.authorize(secret, 123456)).thenReturn(false);

        OtpValidationResult result = totpService.validateTotp(username, totpCode);

        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("Should fail validation when code format is invalid")
    void validateTotp_InvalidFormat() {
        String username = "user";
        String totpCode = "invalid";
        String secret = "secret-key";

        when(valueOperations.get("totp:secret:" + username)).thenReturn(secret);

        OtpValidationResult result = totpService.validateTotp(username, totpCode);

        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("Should save TOTP secret successfully")
    void saveTotpSecret_Success() {
        String username = "user";
        String secret = "secret-key";

        totpService.saveTotpSecret(username, secret);

        verify(valueOperations).set(eq("totp:secret:" + username), eq(secret), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    @DisplayName("Should handle exception when saving TOTP secret")
    void saveTotpSecret_Exception() {
        String username = "user";
        String secret = "secret-key";

        doThrow(new RuntimeException("Redis error")).when(valueOperations).set(anyString(), anyString(), anyLong(), any());

        assertDoesNotThrow(() -> totpService.saveTotpSecret(username, secret));
    }

    @Test
    @DisplayName("Should get TOTP secret successfully")
    void getTotpSecret_Success() {
        String username = "user";
        String secret = "secret-key";

        when(valueOperations.get("totp:secret:" + username)).thenReturn(secret);

        String result = totpService.getTotpSecret(username);

        assertEquals(secret, result);
    }

    @Test
    @DisplayName("Should return null when getting TOTP secret fails")
    void getTotpSecret_Exception() {
        String username = "user";

        when(valueOperations.get("totp:secret:" + username)).thenThrow(new RuntimeException("Redis error"));

        String result = totpService.getTotpSecret(username);

        assertNull(result);
    }

    @Test
    @DisplayName("Should revoke TOTP secret successfully")
    void revokeTotpSecret_Success() {
        String username = "user";

        totpService.revokeTotpSecret(username);

        verify(redisTemplate).delete("totp:secret:" + username);
    }

    @Test
    @DisplayName("Should handle exception when revoking TOTP secret")
    void revokeTotpSecret_Exception() {
        String username = "user";

        doThrow(new RuntimeException("Redis error")).when(redisTemplate).delete(anyString());

        assertDoesNotThrow(() -> totpService.revokeTotpSecret(username));
    }
}