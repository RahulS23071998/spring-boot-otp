---
description: Repository Information Overview
alwaysApply: true
---

# Spring Boot OTP Information

## Summary
Spring Boot project demonstrating OTP (One-Time Password) authentication technique. The application provides JWT-based authentication with an optional OTP verification step. It includes email-based OTP delivery and verification functionality.

## Structure
- **src/main/java**: Java source code containing controllers, services, and configuration
- **src/main/resources**: Application configuration, properties, and database migration scripts
- **src/test**: Test classes for the application

## Language & Runtime
**Language**: Java
**Version**: Java 17 (updated from Java 8)
**Build System**: Maven
**Package Manager**: Maven

## Dependencies
**Main Dependencies**:
- Spring Boot 3.2.3 (updated from 1.5.7.RELEASE)
- Spring Security
- Spring Data JPA
- MySQL Connector
- Liquibase 4.23.0
- JWT (jjwt 0.11.5)
- Guava 32.1.2-jre (for OTP functionality)
- Spring Boot Mail
- Spring Boot Validation

**Development Dependencies**:
- Spring Boot Test
- Liquibase Maven Plugin

## Build & Installation
```bash
# Install dependencies and build
mvn install

# Run the application
mvn spring-boot:run

# Package for production
mvn clean
mvn -Pprod package
```

## Database
**Type**: MySQL
**Configuration**: 
- Database name: otp
- Migrations managed by Liquibase
- Schema changes tracked in src/main/resources/liquibase directory
- Using MySQL8Dialect for Hibernate

## Application Configuration
**Profiles**:
- Development (dev) - default
- Production (prod)

**Configuration Files**:
- application.yml - Common configuration
- application-dev.yml - Development-specific settings
- application-prod.yml - Production-specific settings

## Main Components
**Authentication**:
- JWT-based authentication via `/auth/authenticate`
- OTP verification via `/auth/verify`
- Token provider for JWT generation and validation

**OTP System**:
- OTP generation service
- Email-based OTP delivery
- OTP validation functionality
- In-memory OTP cache

**Security**:
- Spring Security configuration
- JWT token filter
- Authentication provider setup

## Testing
**Framework**: Spring Boot Test
**Test Location**: src/test/java
**Test Categories**:
- Configuration tests
- Security tests

## Migration Notes
- Updated from Spring Boot 1.5.7 to Spring Boot 3.2.3
- Migrated from javax.* to jakarta.* packages for Jakarta EE compatibility
- Updated JWT library from jjwt 0.6.0 to jjwt 0.11.5
- Updated database driver and configuration for MySQL 8 compatibility
- Updated Java version from 8 to 17
```