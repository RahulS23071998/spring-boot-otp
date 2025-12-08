package com.starter.springboot.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoogleOAuthService Tests")
class GoogleOAuthServiceTest {

    @Mock
    private GoogleIdTokenVerifier mockVerifier;

    @Mock
    private GoogleIdToken mockIdToken;

    @Mock
    private GoogleIdToken.Payload mockPayload;

    @InjectMocks
    private GoogleOAuthService googleOAuthService;

    private static final String VALID_TOKEN = "valid-google-id-token";
    private static final String INVALID_TOKEN = "invalid-google-id-token";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_GOOGLE_ID = "google-123456789";
    private static final String TEST_GIVEN_NAME = "John";
    private static final String TEST_FAMILY_NAME = "Doe";
    private static final String TEST_FULL_NAME = "John Doe";
    private static final String TEST_PICTURE = "https://example.com/picture.jpg";
    private static final String GOOGLE_CLIENT_ID = "test-client-id.apps.googleusercontent.com";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(googleOAuthService, "googleClientId", GOOGLE_CLIENT_ID);
        ReflectionTestUtils.setField(googleOAuthService, "verifier", mockVerifier);
    }

    @Test
    @DisplayName("Should successfully verify valid Google ID token and extract user info")
    void testVerifyAndExtractUserInfo_WithValidToken_ReturnsUserInfo() throws Exception {
        when(mockVerifier.verify(VALID_TOKEN)).thenReturn(mockIdToken);
        when(mockIdToken.getPayload()).thenReturn(mockPayload);
        when(mockPayload.getEmail()).thenReturn(TEST_EMAIL);
        when(mockPayload.getSubject()).thenReturn(TEST_GOOGLE_ID);
        when(mockPayload.get("name")).thenReturn(TEST_FULL_NAME);
        when(mockPayload.get("given_name")).thenReturn(TEST_GIVEN_NAME);
        when(mockPayload.get("family_name")).thenReturn(TEST_FAMILY_NAME);
        when(mockPayload.get("picture")).thenReturn(TEST_PICTURE);

        Map<String, Object> result = googleOAuthService.verifyAndExtractUserInfo(VALID_TOKEN);

        assertNotNull(result);
        assertEquals(TEST_EMAIL, result.get("email"));
        assertEquals(TEST_GOOGLE_ID, result.get("sub"));
        assertEquals(TEST_FULL_NAME, result.get("name"));
        assertEquals(TEST_GIVEN_NAME, result.get("given_name"));
        assertEquals(TEST_FAMILY_NAME, result.get("family_name"));
        assertEquals(TEST_PICTURE, result.get("picture"));
    }

    @Test
    @DisplayName("Should return null when Google ID token verification fails")
    void testVerifyAndExtractUserInfo_WithInvalidToken_ReturnsNull() throws Exception {
        when(mockVerifier.verify(INVALID_TOKEN)).thenReturn(null);

        Map<String, Object> result = googleOAuthService.verifyAndExtractUserInfo(INVALID_TOKEN);

        assertNull(result);
    }

    @Test
    @DisplayName("Should return null when exception occurs during token verification")
    void testVerifyAndExtractUserInfo_WithException_ReturnsNull() throws Exception {
        when(mockVerifier.verify(VALID_TOKEN)).thenThrow(new RuntimeException("Token verification failed"));

        Map<String, Object> result = googleOAuthService.verifyAndExtractUserInfo(VALID_TOKEN);

        assertNull(result);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when Google Client ID is not configured")
    void testVerifyAndExtractUserInfo_WithoutClientId_ThrowsException() {
        ReflectionTestUtils.setField(googleOAuthService, "googleClientId", "");
        ReflectionTestUtils.setField(googleOAuthService, "verifier", mockVerifier);

        Map<String, Object> result = googleOAuthService.verifyAndExtractUserInfo(VALID_TOKEN);

        assertNull(result);
    }

    @Test
    @DisplayName("Should throw IllegalStateException when Google Client ID is null")
    void testVerifyAndExtractUserInfo_WithNullClientId_ThrowsException() {
        ReflectionTestUtils.setField(googleOAuthService, "googleClientId", null);
        ReflectionTestUtils.setField(googleOAuthService, "verifier", mockVerifier);

        Map<String, Object> result = googleOAuthService.verifyAndExtractUserInfo(VALID_TOKEN);

        assertNull(result);
    }

    @Test
    @DisplayName("Should handle user info with null optional fields")
    void testVerifyAndExtractUserInfo_WithNullOptionalFields_ReturnsUserInfo() throws Exception {
        when(mockVerifier.verify(VALID_TOKEN)).thenReturn(mockIdToken);
        when(mockIdToken.getPayload()).thenReturn(mockPayload);
        when(mockPayload.getEmail()).thenReturn(TEST_EMAIL);
        when(mockPayload.getSubject()).thenReturn(TEST_GOOGLE_ID);
        when(mockPayload.get("name")).thenReturn(null);
        when(mockPayload.get("given_name")).thenReturn(null);
        when(mockPayload.get("family_name")).thenReturn(null);
        when(mockPayload.get("picture")).thenReturn(null);

        Map<String, Object> result = googleOAuthService.verifyAndExtractUserInfo(VALID_TOKEN);

        assertNotNull(result);
        assertEquals(TEST_EMAIL, result.get("email"));
        assertEquals(TEST_GOOGLE_ID, result.get("sub"));
        assertNull(result.get("name"));
        assertNull(result.get("given_name"));
        assertNull(result.get("family_name"));
        assertNull(result.get("picture"));
    }

    @Test
    @DisplayName("Should extract user info with partial data")
    void testVerifyAndExtractUserInfo_WithPartialUserInfo_ReturnsAvailableData() throws Exception {
        when(mockVerifier.verify(VALID_TOKEN)).thenReturn(mockIdToken);
        when(mockIdToken.getPayload()).thenReturn(mockPayload);
        when(mockPayload.getEmail()).thenReturn(TEST_EMAIL);
        when(mockPayload.getSubject()).thenReturn(TEST_GOOGLE_ID);
        when(mockPayload.get("name")).thenReturn(TEST_FULL_NAME);
        when(mockPayload.get("given_name")).thenReturn(TEST_GIVEN_NAME);
        when(mockPayload.get("family_name")).thenReturn(null);
        when(mockPayload.get("picture")).thenReturn(TEST_PICTURE);

        Map<String, Object> result = googleOAuthService.verifyAndExtractUserInfo(VALID_TOKEN);

        assertNotNull(result);
        assertEquals(TEST_EMAIL, result.get("email"));
        assertEquals(TEST_GOOGLE_ID, result.get("sub"));
        assertEquals(TEST_FULL_NAME, result.get("name"));
        assertEquals(TEST_GIVEN_NAME, result.get("given_name"));
        assertNull(result.get("family_name"));
        assertEquals(TEST_PICTURE, result.get("picture"));
    }

    @Test
    @DisplayName("Should handle IO exception during token verification")
    void testVerifyAndExtractUserInfo_WithIOException_ReturnsNull() throws Exception {
        when(mockVerifier.verify(VALID_TOKEN)).thenThrow(new java.io.IOException("Network error"));

        Map<String, Object> result = googleOAuthService.verifyAndExtractUserInfo(VALID_TOKEN);

        assertNull(result);
    }

    @Test
    @DisplayName("Should extract minimal user info with only email and subject")
    void testVerifyAndExtractUserInfo_WithMinimalInfo_ReturnsUserInfo() throws Exception {
        when(mockVerifier.verify(VALID_TOKEN)).thenReturn(mockIdToken);
        when(mockIdToken.getPayload()).thenReturn(mockPayload);
        when(mockPayload.getEmail()).thenReturn(TEST_EMAIL);
        when(mockPayload.getSubject()).thenReturn(TEST_GOOGLE_ID);
        when(mockPayload.get("name")).thenReturn(null);
        when(mockPayload.get("given_name")).thenReturn(null);
        when(mockPayload.get("family_name")).thenReturn(null);
        when(mockPayload.get("picture")).thenReturn(null);
        when(mockPayload.getEmailVerified()).thenReturn(true);

        Map<String, Object> result = googleOAuthService.verifyAndExtractUserInfo(VALID_TOKEN);

        assertNotNull(result);
        assertEquals(7, result.size());
        assertEquals(TEST_EMAIL, result.get("email"));
        assertEquals(TEST_GOOGLE_ID, result.get("sub"));
    }
}
