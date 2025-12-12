package com.starter.springboot.config;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;

public class MDCFilter extends GenericFilterBean {

    private static final String REQUEST_ID = "requestId";
    private static final String USER_ID = "userId";
    private static final String REQUEST_URI = "requestUri";
    private static final String REQUEST_METHOD = "requestMethod";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        try {
            HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
            
            String requestId = extractOrGenerateRequestId(httpServletRequest);
            MDC.put(REQUEST_ID, requestId);
            
            MDC.put(REQUEST_URI, httpServletRequest.getRequestURI());
            MDC.put(REQUEST_METHOD, httpServletRequest.getMethod());
            
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                String userId = authentication.getName();

                // Mask email safely if it looks like one
                if (StringUtils.contains(userId, "@")) {
                    String[] parts = userId.split("@");
                    String local = parts[0];
                    String domain = parts[1];

                    // Keep first 2 chars of local part, mask the rest
                    String maskedLocal = StringUtils.rightPad(
                            StringUtils.left(local, Math.min(2, local.length())), // take first 2 chars
                            local.length(), '*'
                    );

                    // Keep only domain name (mask everything after first dot)
                    String maskedDomain = domain.contains(".")
                            ? domain.substring(0, domain.indexOf('.')) + ".***"
                            : "***";

                    userId = maskedLocal + "@" + maskedDomain;
                }

                MDC.put(USER_ID, userId);
            }
            
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            MDC.clear();
        }
    }

    private String extractOrGenerateRequestId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-ID");
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }
        return requestId;
    }
}
