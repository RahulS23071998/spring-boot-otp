package com.starter.springboot.service.impl;

import com.starter.springboot.service.IOtpProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "otp")
public class OtpProperties implements IOtpProperties {

    private int expiryMinutes;
    private int maxAttempts;
    private int attemptWindowMinutes;
    private AuditPurgeProperties auditPurge = new AuditPurgeProperties();

    public int getExpiryMinutes() { return expiryMinutes; }
    public void setExpiryMinutes(int expiryMinutes) { this.expiryMinutes = expiryMinutes; }

    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

    public int getAttemptWindowMinutes() { return attemptWindowMinutes; }
    public void setAttemptWindowMinutes(int attemptWindowMinutes) { this.attemptWindowMinutes = attemptWindowMinutes; }

    public AuditPurgeProperties getAuditPurge() {
        return auditPurge;
    }

    public void setAuditPurge(AuditPurgeProperties auditPurge) {
        this.auditPurge = auditPurge;
    }

    public static class AuditPurgeProperties {
        private boolean enabled = true;
        private int retentionDays = 90;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getRetentionDays() {
            return retentionDays;
        }

        public void setRetentionDays(int retentionDays) {
            this.retentionDays = retentionDays;
        }
    }
}
