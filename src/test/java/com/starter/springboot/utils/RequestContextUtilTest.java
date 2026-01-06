package com.starter.springboot.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequestContextUtil Tests")
class RequestContextUtilTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private ServletRequestAttributes attributes;

    @BeforeEach
    void setUp() {
        RequestContextHolder.setRequestAttributes(attributes);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Should return client IP from X-Forwarded-For header")
    void shouldReturnIpFromXForwardedForHeader() {
        when(attributes.getRequest()).thenReturn(request);
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");

        String ip = RequestContextUtil.getClientIpAddress();

        assertEquals("192.168.1.1", ip);
    }

    @Test
    @DisplayName("Should return client IP from remote address when header is missing")
    void shouldReturnIpFromRemoteAddrWhenHeaderMissing() {
        when(attributes.getRequest()).thenReturn(request);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        String ip = RequestContextUtil.getClientIpAddress();

        assertEquals("127.0.0.1", ip);
    }

    @Test
    @DisplayName("Should return User-Agent from header")
    void shouldReturnUserAgentFromHeader() {
        String testUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";
        when(attributes.getRequest()).thenReturn(request);
        when(request.getHeader("User-Agent")).thenReturn(testUserAgent);

        String userAgent = RequestContextUtil.getUserAgent();

        assertEquals(testUserAgent, userAgent);
    }

    @Test
    @DisplayName("Should return Unknown when no request context exists")
    void shouldReturnUnknownWhenNoContext() {
        RequestContextHolder.resetRequestAttributes();

        assertEquals("Unknown", RequestContextUtil.getClientIpAddress());
        assertEquals("Unknown", RequestContextUtil.getUserAgent());
    }

    @Test
    @DisplayName("Should truncate long User-Agent")
    void shouldTruncateLongUserAgent() {
        StringBuilder longUA = new StringBuilder();
        for (int i = 0; i < 60; i++) {
            longUA.append("Mozilla/5.0 ");
        }
        String uaString = longUA.toString();
        
        when(attributes.getRequest()).thenReturn(request);
        when(request.getHeader("User-Agent")).thenReturn(uaString);

        String userAgent = RequestContextUtil.getUserAgent();

        assertEquals(500, userAgent.length());
        assertEquals(uaString.substring(0, 500), userAgent);
    }

    @Test
    @DisplayName("Should return combined IP and User-Agent")
    void shouldReturnCombinedIpAndUserAgent() {
        when(attributes.getRequest()).thenReturn(request);
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4");
        when(request.getHeader("User-Agent")).thenReturn("TestAgent");

        String combined = RequestContextUtil.getClientIpAndUserAgent();

        assertEquals("IP: 1.2.3.4 | UserAgent: TestAgent", combined);
    }
}
