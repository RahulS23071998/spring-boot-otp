package com.starter.springboot.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;

public class RequestContextUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestContextUtil.class);
    
    private static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String UNKNOWN = "Unknown";
    private static final int IP_ADDRESS_MAX_LENGTH = 45;
    private static final int USER_AGENT_MAX_LENGTH = 500;

    private RequestContextUtil() {
    }

    public static String getClientIpAddress() {
        try {
            HttpServletRequest request = getHttpServletRequest();
            if (Objects.isNull(request)) {
                LOGGER.debug("No HTTP request context found");
                return UNKNOWN;
            }

            String ip = request.getHeader(X_FORWARDED_FOR_HEADER);
            if (Objects.nonNull(ip) && !ip.isEmpty() && !UNKNOWN.equalsIgnoreCase(ip)) {
                ip = ip.split(",")[0].trim();
            } else {
                ip = request.getRemoteAddr();
            }

            if (ip != null && ip.length() > IP_ADDRESS_MAX_LENGTH) {
                ip = ip.substring(0, IP_ADDRESS_MAX_LENGTH);
            }

            LOGGER.debug("Client IP address: {}", ip);
            return ip;
        } catch (Exception e) {
            LOGGER.warn("Failed to get client IP address: {}", e.getMessage());
            return UNKNOWN;
        }
    }

    public static String getUserAgent() {
        try {
            HttpServletRequest request = getHttpServletRequest();
            if (Objects.isNull(request)) {
                LOGGER.debug("No HTTP request context found for user agent");
                return UNKNOWN;
            }

            String userAgent = request.getHeader(USER_AGENT_HEADER);
            if (Objects.isNull(userAgent) || userAgent.isEmpty()) {
                userAgent = UNKNOWN;
            }

            if (userAgent.length() > USER_AGENT_MAX_LENGTH) {
                userAgent = userAgent.substring(0, USER_AGENT_MAX_LENGTH);
            }

            LOGGER.debug("User agent: {}", userAgent);
            return userAgent;
        } catch (Exception e) {
            LOGGER.warn("Failed to get user agent: {}", e.getMessage());
            return UNKNOWN;
        }
    }

    public static String getClientIpAndUserAgent() {
        return String.format("IP: %s | UserAgent: %s", getClientIpAddress(), getUserAgent());
    }

    private static HttpServletRequest getHttpServletRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (Objects.nonNull(attributes)) {
                return attributes.getRequest();
            }
        } catch (Exception e) {
            LOGGER.debug("Unable to get ServletRequestAttributes: {}", e.getMessage());
        }
        return null;
    }
}
