package com.starter.springboot.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.ExternalDocumentation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI configuration for API documentation.
 * Provides interactive API documentation with JWT authentication support.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Spring Boot OTP Authentication API")
                        .version("1.0.0")
                        .description("Secure One-Time Password (OTP) authentication system with JWT tokens. " +
                                "This API provides comprehensive OTP-based authentication with JWT tokens, " +
                                "email verification, rate limiting, and audit logging.")
                        .termsOfService("https://example.com/terms")
                        .contact(new Contact()
                                .name("Spring Boot OTP Team")
                                .email("support@springboot-otp.com")
                                .url("https://github.com/your-org/spring-boot-otp"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .externalDocs(new ExternalDocumentation()
                        .url("https://github.com/RahulS23071998/spring-boot-otp")
                        .description("Spring Boot OTP GitHub Repository"))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development server - Local environment for testing and development"),
                        new Server()
                                .url("https://api.springboot-otp.com")
                                .description("Production server - Live environment"),
                        new Server()
                                .url("https://staging.springboot-otp.com")
                                .description("Staging server - Pre-production testing environment")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT Authorization header using the Bearer scheme. " +
                                                "Example: \"Authorization: Bearer {token}\"\n\n" +
                                                "**How to obtain a token:**\n" +
                                                "1. Call POST /auth/authenticate with valid credentials\n" +
                                                "2. Verify OTP via POST /auth/verify with the OTP code\n" +
                                                "3. Use the returned JWT token in subsequent requests")))
                .security(List.of(new SecurityRequirement().addList("bearerAuth")));
    }
}