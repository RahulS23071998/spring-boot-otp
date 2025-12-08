package com.starter.springboot.listener;

import com.starter.springboot.entity.BaseAuditedEntity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CustomAuditingListener {

    @PrePersist
    public void prePersist(BaseAuditedEntity entity) {
        LocalDateTime now = LocalDateTime.now();
        String auditor = getAuthenticatedUsername();
        
        entity.setCreatedDate(now);
        entity.setCreatedBy(auditor);
    }

    @PreUpdate
    public void preUpdate(BaseAuditedEntity entity) {
        LocalDateTime now = LocalDateTime.now();
        String auditor = getAuthenticatedUsername();
        
        entity.setLastModifiedDate(now);
        entity.setLastModifiedBy(auditor);
    }

    private String getAuthenticatedUsername() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .orElse(null);
    }
}
