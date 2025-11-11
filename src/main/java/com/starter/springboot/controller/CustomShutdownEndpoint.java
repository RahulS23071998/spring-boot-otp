package com.starter.springboot.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
@Endpoint(id = "shutdown")
public class CustomShutdownEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(CustomShutdownEndpoint.class);

    private final ConfigurableApplicationContext applicationContext;

    public CustomShutdownEndpoint(ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @WriteOperation
    public Map<String, String> shutdown() {
        logger.info("Custom actuator shutdown endpoint called - initiating graceful shutdown");

        // Create custom response
        Map<String, String> response = Map.of(
            "message", "Spring Boot OTP Application is shutting down gracefully...",
            "status", "SHUTTING_DOWN",
            "timestamp", Instant.now().toString()
        );

        // Initiate shutdown in a separate thread to allow response to be sent
        new Thread(() -> {
            try {
                Thread.sleep(1000); // Small delay to ensure response is sent
                logger.info("Closing application context...");
                applicationContext.close();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Shutdown thread interrupted", e);
            }
        }).start();

        return response;
    }
}