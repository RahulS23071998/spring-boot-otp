package com.starter.springboot.security.jwt;

import com.starter.springboot.constants.ApplicationConstants;
import io.jsonwebtoken.ExpiredJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filters incoming requests and installs a Spring Security principal if a header corresponding to a valid user is
 * found.
 */
public class JWTFilter extends GenericFilterBean {

    private final Logger log = LoggerFactory.getLogger(JWTFilter.class);

    private final ITokenProvider tokenProvider;

    public JWTFilter(ITokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    /**
     * Method for filtering JWT Token.
     *
     * @param servletRequest - Http request
     * @param servletResponse - Http response
     * @param filterChain - filter chain
     * @throws IOException - Input/Output exception
     * @throws ServletException - Servlet exception
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
        throws IOException, ServletException
    {
        try
        {
            HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
            String requestUri = httpServletRequest.getRequestURI();
            
            if (requestUri.startsWith("/auth/authenticate") || 
                requestUri.startsWith("/auth/verify") || 
                requestUri.startsWith("/auth/refresh") || 
                requestUri.startsWith("/auth/google")) {
                log.debug("Skipping JWT validation for auth endpoint: {}", requestUri);
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            }
            
            String jwt = resolveToken(httpServletRequest);
            if (StringUtils.hasText(jwt))
            {
                if (this.tokenProvider.validateToken(jwt))
                {
                    Authentication authentication = this.tokenProvider.getAuthentication(jwt);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
            filterChain.doFilter(servletRequest, servletResponse);
        }
        catch (ExpiredJwtException eje) {
            log.info("Security exception for user {} - {}", eje.getClaims().getSubject(), eje.getMessage());
            ((HttpServletResponse) servletResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    /**
     * Method for resolving token
     *
     * @param request - Http request
     * @return Token string | null
     */
    private String resolveToken(HttpServletRequest request)
    {
        String bearerToken = request.getHeader(JWTConfigurer.AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(ApplicationConstants.BEARER_PREFIX)){
            String token = bearerToken.substring(ApplicationConstants.BEARER_PREFIX.length(), bearerToken.length());
            log.info("Extracted Bearer Token from header: {}", token);
            return token;
        }
        String jwt = request.getParameter(JWTConfigurer.AUTHORIZATION_TOKEN);
        if (StringUtils.hasText(jwt)) {
            log.info("Extracted Token from parameter: {}", jwt);
            return jwt;
        }
        log.info("No token found in request");
        return null;
    }
}
