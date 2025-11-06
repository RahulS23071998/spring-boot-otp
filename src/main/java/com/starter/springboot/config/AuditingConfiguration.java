package com.starter.springboot.config;

import java.security.Principal;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Configuration for JPA Auditing.
 * Provides the current user information for audit tracking.
 * 
 * Note: @EnableJpaAuditing is not needed here as Spring Boot's JpaRepositoriesAutoConfiguration
 * already enables it when Liquibase is available. We only need to provide the AuditorAware bean.
 */
@Configuration
public class AuditingConfiguration {

    /**
     * Returns the current user from Security Context.
     * This is used by Spring Data JPA to populate @CreatedBy and @LastModifiedBy fields.
     *
     * @return An Optional containing the current username, or empty if no user is authenticated
     */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Principal::getName);
    }
}