package com.starter.springboot.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.starter.springboot.service.IGoogleOAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class GoogleOAuthService implements IGoogleOAuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleOAuthService.class);

    @Value("${google.oauth.client-id:}")
    private String googleClientId;

    private GoogleIdTokenVerifier verifier;

    public GoogleIdTokenVerifier getVerifier() {
        if (verifier == null) {
            verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();
        }
        return verifier;
    }

    public Map<String, Object> verifyAndExtractUserInfo(String idTokenString) {
        try {
            if (Objects.isNull(googleClientId) || googleClientId.isEmpty()) {
                throw new IllegalStateException("Google Client ID not configured");
            }

            GoogleIdToken idToken = getVerifier().verify(idTokenString);
            if (Objects.nonNull(idToken)) {
                GoogleIdToken.Payload payload = idToken.getPayload();

                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("email", payload.getEmail());
                userInfo.put("name", payload.get("name"));
                userInfo.put("given_name", payload.get("given_name"));
                userInfo.put("family_name", payload.get("family_name"));
                userInfo.put("picture", payload.get("picture"));
                userInfo.put("sub", payload.getSubject());

                LOGGER.info("Successfully verified Google ID token for user: {}", payload.getEmail());
                return userInfo;
            } else {
                LOGGER.warn("Invalid Google ID token");
                return null;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to verify Google ID token: {}", e.getMessage());
            return null;
        }
    }
}
