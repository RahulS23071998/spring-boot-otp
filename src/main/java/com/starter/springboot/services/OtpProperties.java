package com.starter.springboot.services;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "otp")
public class OtpProperties {

    private int expiryMinutes;
    private int maxAttempts;
    private int attemptWindowMinutes;

    public int getExpiryMinutes() { return expiryMinutes; }
    public void setExpiryMinutes(int expiryMinutes) { this.expiryMinutes = expiryMinutes; }

    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

    public int getAttemptWindowMinutes() { return attemptWindowMinutes; }
    public void setAttemptWindowMinutes(int attemptWindowMinutes) { this.attemptWindowMinutes = attemptWindowMinutes; }
}
