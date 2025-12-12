# Spring Boot OTP Authentication System

A comprehensive Spring Boot application demonstrating secure One-Time Password (OTP) authentication with JWT tokens, Redis session management, and email verification. Recently migrated from Spring Boot 1.5.7 to 3.2.3 with Jakarta EE compatibility.

## 📋 Table of Contents
- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Migration Notes](#-migration-notes)
- [Project Structure](#-project-structure)
- [Prerequisites](#-prerequisites)
- [Setup & Installation](#-setup--installation)
- [Configuration](#-configuration)
- [API Endpoints](#-api-endpoints)
- [Authentication Flow](#-authentication-flow)
- [OTP Audit System](#-otp-audit-system)
- [API Documentation](#-api-documentation)
- [Application Monitoring](#-application-monitoring)
- [Testing](#-testing)
- [Profiles](#-profiles)
- [Security](#-security)
- [Contributing](#-contributing)

## ✨ Features

- **JWT Token Authentication** - Secure token-based authentication with Redis whitelisting
- **OTP Verification** - Email-based OTP verification with rate limiting and audit trails
- **Google OAuth Integration** - Seamless Google OAuth 2.0 authentication with automatic user provisioning
- **OAuth Password Setup** - Secure password setup flow for OAuth users using temporary tokens
- **Temporary Token System** - Short-lived tokens for sensitive operations like password reset
- **Redis Session Management** - Token whitelisting, user caching, and session management
- **User Management** - Complete user registration, activation, and profile management
- **Email Service** - SMTP email integration for OTP delivery and notifications
- **Password Reset** - Secure password reset with OTP verification
- **Role-Based Access Control** - Authority and role-based permissions system
- **Embedded Redis** - Development-ready embedded Redis with production Redis support
- **Comprehensive Auditing** - OTP audit trails and retention policies
- **Rate Limiting** - OTP generation rate limiting and password set attempt limiting
- **Data Validation** - Jakarta validation with custom constraints and password strength validation
- **Exception Handling** - Global exception handling with detailed error responses
- **API Documentation** - Interactive Swagger/OpenAPI documentation
- **Application Monitoring** - Spring Boot Actuator endpoints for health checks and metrics
- **Profile Support** - Development and production environment configurations
- **Security Hardening** - Multiple layers of security validation and CORS protection
- **Comprehensive Testing** - Extensive unit and integration tests (340+ test cases with 90%+ coverage)

## 🛠 Tech Stack

- **Framework**: Spring Boot 3.2.3
- **Java**: 17 (migrated from Java 8)
- **Database**: MySQL 8.0 with Liquibase 4.23.0
- **Cache**: Redis with embedded Redis for development
- **Security**: Spring Security with JWT (jjwt 0.11.5)
- **Email**: Spring Boot Mail
- **OTP Generation**: Google Guava 32.1.2-jre
- **API Documentation**: SpringDoc OpenAPI (Swagger) 2.2.0
- **Monitoring**: Spring Boot Actuator
- **Testing**: JUnit 5, Mockito, TestContainers
- **Build Tool**: Maven
- **Migration**: Jakarta EE (javax.* → jakarta.*)

## 🔄 Migration Notes

This project has been recently updated from Spring Boot 1.5.7.RELEASE to 3.2.3. Key changes include:

### Major Updates
- **Spring Boot**: 1.5.7.RELEASE → 3.2.3
- **Java Version**: 8 → 17
- **JWT Library**: jjwt 0.6.0 → 0.11.5
- **Jakarta EE**: Migrated from javax.* to jakarta.* packages
- **Database**: Enhanced MySQL 8 compatibility
- **Dependencies**: Updated Liquibase (4.23.0), Guava (32.1.2-jre), and other libraries

### Breaking Changes
- **Package Imports**: All `javax.*` imports changed to `jakarta.*`
- **Validation**: `@Valid` and `@NotNull` now use Jakarta validation
- **Servlet API**: Updated to Jakarta Servlet API
- **Security**: Spring Security configuration updated for compatibility

### Configuration Updates
- **Database Dialect**: Now uses `MySQL8Dialect` for Hibernate
- **Redis**: Enhanced Redis integration with embedded Redis for development
- **JWT**: Updated token generation and validation methods

### Migration Benefits
- **Security**: Latest security patches and improvements
- **Performance**: Better performance with updated dependencies
- **Maintenance**: Long-term support and active community
- **Features**: Access to latest Spring Boot features and improvements

## 📁 Project Structure

```
src/
├── main/
│   ├── java/com/starter/springboot/
│   │   ├── config/                        # Configuration classes
│   │   │   ├── AuditingConfiguration.java         # JPA auditing config
│   │   │   ├── EmailConfiguration.java            # Email service config
│   │   │   ├── EmbeddedRedisConfig.java           # Embedded Redis for dev
│   │   │   ├── Http401UnauthorizedEntryPoint.java # Security entry point
│   │   │   ├── ProviderConfiguration.java         # Authentication providers
│   │   │   ├── RedisConfig.java                   # Redis configuration
│   │   │   └── SecurityConfiguration.java         # Spring Security config
│   │   ├── constants/                     # Application constants
│   │   │   ├── ApplicationConstants.java          # App-wide constants
│   │   │   ├── DatabaseConstants.java             # DB-related constants
│   │   │   ├── EmailConstants.java                # Email constants
│   │   │   ├── OtpConstants.java                  # OTP constants
│   │   │   ├── SecurityConstants.java             # Security constants
│   │   │   └── ValidationConstants.java           # Validation constants
│   │   ├── controller/                    # REST controllers
│   │   │   ├── AuthenticationController.java      # Auth endpoints
│   │   │   ├── PublicUserResource.java            # Public user operations
│   │   │   └── UserResource.java                  # Protected user operations
│   │   ├── converter/                     # Data converters
│   │   │   ├── LocalDateToSqlDateConverter.java   # Date conversion
│   │   │   ├── LocalDateToUtilDateConverter.java  # Date conversion
│   │   │   └── StringToDateConverter.java         # String to date conversion
│   │   ├── dto/                          # Data Transfer Objects
│   │   │   ├── AuthResponseDTO.java               # Authentication response
│   │   │   ├── EmailDTO.java                      # Email data
│   │   │   ├── GoogleTokenDTO.java                # Google OAuth token
│   │   │   ├── LoginDTO.java                      # Login request
│   │   │   ├── OAuthRedirectDTO.java              # OAuth redirect response
│   │   │   ├── OtpGenerationResult.java           # OTP generation result
│   │   │   ├── OtpValidationResult.java           # OTP validation result
│   │   │   ├── SetPasswordDTO.java                # Password setup request
│   │   │   ├── SetPasswordResponseDTO.java        # Password setup response
│   │   │   ├── UserRequestDTO.java                # User request data
│   │   │   ├── UserResponseDTO.java               # User response data
│   │   │   └── VerifyTokenRequestDTO.java         # OTP verification request
│   │   ├── entity/                        # JPA entities
│   │   │   ├── Authority.java                     # User authority entity
│   │   │   ├── BaseAuditedEntity.java             # Base audit entity
│   │   │   ├── OtpAuditEntry.java                 # OTP audit entity
│   │   │   ├── Role.java                          # User role entity
│   │   │   ├── User.java                          # User entity
│   │   │   └── UserStatus.java                    # User status enum
│   │   ├── exception/                     # Custom exceptions
│   │   │   ├── OtpRequiredException.java          # OTP required exception
│   │   │   ├── RestExceptionHandler.java          # Global exception handler
│   │   │   └── UserNotActivatedException.java     # User activation exception
│   │   ├── repository/                    # Data repositories
│   │   │   ├── AuthorityRepository.java           # Authority data access
│   │   │   ├── OtpAuditEntryRepository.java       # OTP audit data access
│   │   │   ├── RoleRepository.java                # Role data access
│   │   │   └── UserRepository.java                # User data access
│   │   ├── security/                      # Security components
│   │   │   ├── jwt/                               # JWT implementation
│   │   │   │   ├── JWTConfigurer.java             # JWT configurer
│   │   │   │   ├── JWTFilter.java                 # JWT filter
│   │   │   │   ├── JWTToken.java                  # JWT token model
│   │   │   │   ├── TokenCreationResponse.java     # Token creation response
│   │   │   │   └── TokenProvider.java             # JWT token provider
│   │   │   ├── AuthoritiesConstants.java          # Authority constants
│   │   │   ├── DomainUserDetails.java             # Custom user details
│   │   │   └── OtpAwareAuthenticationProvider.java # OTP-aware auth provider
│   │   ├── service/                       # Service layer
│   │   │   ├── impl/                              # Service implementations
│   │   │   │   ├── EmailService.java              # Email service impl
│   │   │   │   ├── GoogleOAuthService.java        # Google OAuth impl
│   │   │   │   ├── OtpAuditRetentionService.java  # Audit retention impl
│   │   │   │   ├── OtpAuditServiceImpl.java       # Audit service impl
│   │   │   │   ├── OtpGenerator.java              # OTP generator impl
│   │   │   │   ├── OtpNotificationServiceImpl.java # Notification service impl
│   │   │   │   ├── OtpProperties.java             # OTP properties impl
│   │   │   │   ├── OtpRateLimiterImpl.java        # Rate limiter impl
│   │   │   │   ├── OtpService.java                # OTP service impl
│   │   │   │   ├── PasswordSetupService.java      # Password setup impl
│   │   │   │   ├── PasswordValidationService.java # Password validation impl
│   │   │   │   ├── RedisTokenService.java         # Redis token service impl
│   │   │   │   ├── TemporaryPasswordTokenService.java # Temporary token impl
│   │   │   │   └── UserService.java               # User service impl
│   │   │   ├── IEmailService.java                 # Email service interface
│   │   │   ├── IGoogleOAuthService.java           # Google OAuth interface
│   │   │   ├── IOtpAuditRetentionService.java     # Audit retention interface
│   │   │   ├── IOtpAuditService.java              # Audit service interface
│   │   │   ├── IOtpGenerator.java                 # OTP generator interface
│   │   │   ├── IOtpNotificationService.java       # Notification service interface
│   │   │   ├── IOtpProperties.java                # OTP properties interface
│   │   │   ├── IOtpRateLimiter.java               # Rate limiter interface
│   │   │   ├── IOtpService.java                   # OTP service interface
│   │   │   ├── IPasswordSetupService.java         # Password setup interface
│   │   │   ├── IRedisTokenService.java            # Redis token service interface
│   │   │   ├── ITemporaryPasswordTokenService.java # Temporary token interface
│   │   │   ├── IUserService.java                  # User service interface
│   │   │   └── PasswordValidationService.java     # Password validation logic
│   │   └── Application.java               # Main application class
│   └── resources/
│       ├── application.yml               # Main configuration
│       ├── application-dev.yml           # Development profile
│       ├── application-prod.yml          # Production profile
├── test/
│   └── java/com/starter/springboot/
│       ├── config/                       # Configuration tests
│       │   └── SecurityConfigurationTest.java    # Security config tests
│       ├── controller/                   # Controller tests
│       │   ├── AuthenticationControllerTest.java # Auth controller tests (340+ test cases)
│       │   └── PublicUserResourceTest.java       # Public user tests
│       ├── security/                     # Security tests
│       │   ├── jwt/                              # JWT tests
│       │   │   └── TokenProviderTest.java        # Token provider tests
│       │   └── OtpAwareAuthenticationProviderTest.java # Auth provider tests
│       └── service/                      # Service tests
│           ├── EmailServiceTest.java             # Email service tests
│           ├── GoogleOAuthServiceTest.java       # Google OAuth tests
│           ├── OtpAuditRetentionServiceTest.java # Audit retention tests
│           ├── OtpAuditServiceImplTest.java      # Audit service tests
│           ├── OtpGeneratorTest.java             # OTP generator tests
│           ├── OtpNotificationServiceImplTest.java # Notification tests
│           ├── OtpRateLimiterImplTest.java       # Rate limiter tests
│           ├── OtpServiceTest.java               # OTP service tests
│           ├── PasswordChangeAuthorizationServiceTest.java # Auth tests
│           ├── PasswordSetupServiceTest.java     # Password setup tests (13+ cases)
│           ├── RedisTokenServiceTest.java        # Redis token tests
│           ├── RefreshTokenServiceTest.java      # Refresh token tests
│           ├── TemporaryPasswordTokenServiceTest.java # Temp token tests (13+ cases)
│           └── UserServiceTest.java              # User service tests
```

## 📋 Prerequisites

- Java 17+
- Maven 3.6+
- MySQL 8.0+
- Redis Server (optional for development - embedded Redis included)

## 🚀 Setup & Installation

### 1. Clone Repository
```bash
git clone <repository-url>
cd spring-boot-otp
```

### 2. Database Setup
```sql
CREATE DATABASE otp CHARACTER SET utf8 COLLATE utf8_general_ci;
```

### 3. Configure Application Properties
Update `src/main/resources/application-dev.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost/otp?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
    username: your_username
    password: your_password
  
  mail:
    host: smtp.gmail.com
    port: 587
    username: your_email@gmail.com
    password: your_app_password
```

### 4. Build and Run
```bash
# Clean and install dependencies
mvn clean install

# Run application (development profile by default)
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The application will start on `http://localhost:8080`

### 5. Verify Installation
Check the application logs for successful startup. You should see:
- Database connection established
- Liquibase migrations applied
- Embedded Redis started (if using dev profile)
- Application started on port 8080

### 6. Access Application
- **API Base URL**: `http://localhost:8080`
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **Health Check**: `http://localhost:8080/actuator/health`

## ⚙️ Configuration

### JWT Configuration
```yaml
jwt:
  header: Authorization
  secret: your-base64-encoded-secret-key
  expiration: 300  # 5 minutes
```

### OTP Configuration
```yaml
otp:
  expiry-minutes: 5
  max-attempts: 3
  attempt-window-minutes: 10
```

### Redis Configuration
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

## 🔌 API Endpoints

### Authentication Endpoints

#### Login / Get Token
```http
POST /auth/authenticate
Content-Type: application/json

{
  "username": "admin",
  "password": "nimda",
  "rememberMe": true,
  "clientId": "web-portal",
  "deviceId": "device-1234"
}
```

**Response (OTP Required):**
```json
{
  "username": "admin",
  "status": "OTP_PENDING",
  "message": "OTP required to complete authentication.",
  "otp_required": true,
  "issued_at": "2025-11-07T11:36:51.311436800Z",
  "remember_me": true,
  "client_id": "web-portal",
  "device_id": "device-1234"
}
```

**Response (Direct Token - OTP Disabled):**
```json
{
  "username": "admin",
  "status": "AUTHENTICATED",
  "message": "Authentication successful",
  "otp_required": false,
  "token": {
    "tokenType": "Bearer",
    "idToken": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 300
  },
  "issued_at": "2025-11-07T11:36:51.311436800Z",
  "remember_me": true,
  "client_id": "web-portal",
  "device_id": "device-1234"
}
```

#### OTP Verification
```http
POST /auth/verify
Content-Type: application/json

{
  "username": "admin",
  "otp": 513493,
  "rememberMe": true,
  "clientId": "web-portal",
  "deviceId": "device-1234"
}
```

#### Google OAuth Authentication
```http
POST /auth/google
Content-Type: application/json

{
  "idToken": "google-id-token-here",
  "rememberMe": true,
  "clientId": "web-portal",
  "deviceId": "device-1234"
}
```

**Response (New User - Requires Password Setup):**
```json
{
  "status": "SET_PASSWORD_REQUIRED",
  "username": "user@gmail.com",
  "message": "Please set your password to complete registration",
  "temporary_token": "uuid-token-123"
}
```

**Response (Existing User - Already Has Password):**
```json
{
  "status": "SUCCESS",
  "username": "user@gmail.com",
  "message": "Authentication successful",
  "otp_required": false,
  "token": {
    "id_token": "eyJhbGciOiJIUzI1NiIs...",
    "token_type": "Bearer",
    "expires_in": 3600
  }
}
```

#### Set Password (OAuth Users)
```http
POST /auth/set-password
Content-Type: application/json

{
  "password": "NewPassword123!",
  "confirmPassword": "NewPassword123!",
  "temporaryToken": "uuid-token-123"
}
```

**Response:**
```json
{
  "status": "SUCCESS",
  "message": "Password set successfully",
  "username": "user@gmail.com",
  "password_set": true,
  "timestamp": "2025-12-10T13:40:00.000Z"
}
```

### User Management Endpoints

#### Register User
```http
POST /api/public/users
Content-Type: application/json

{
  "username": "newuser",
  "email": "user@example.com",
  "password": "password123"
}
```

#### Activate User
```http
PUT /api/public/{id}/status
Content-Type: application/json

{
  "status": "ACTIVE"
}
```

#### Reset Password
```http
PUT /api/public/password
Content-Type: application/json

{
  "username": "user@example.com"
}
```

#### Get Current User
```http
GET /api/users/current
Authorization: Bearer <token>
```

## 🔐 Authentication Flows

### 1. Standard Email/Password Flow (OTP Disabled)
1. User submits credentials → `/auth/authenticate`
2. System validates credentials
3. JWT token returned immediately
4. User can access protected endpoints

### 2. OTP Flow (OTP Enabled)
1. User submits credentials → `/auth/authenticate`
2. System validates credentials
3. OTP generated and sent via email
4. System returns `otpRequired: true` response
5. User submits OTP → `/auth/verify`
6. System validates OTP
7. JWT token returned
8. User can access protected endpoints

### 3. Google OAuth Flow (New User)
1. User initiates Google login
2. User completes Google OAuth consent
3. Frontend sends Google ID token → `/auth/google`
4. System verifies token with Google
5. New OAuth user created automatically
6. System detects user has no password set
7. System returns `SET_PASSWORD_REQUIRED` with temporary token
8. User sets password → `/auth/set-password`
9. System validates password strength
10. Password set confirmed
11. User completes login with email/password

### 4. Google OAuth Flow (Returning User)
1. User initiates Google login
2. User completes Google OAuth consent
3. Frontend sends Google ID token → `/auth/google`
4. System verifies token with Google
5. Existing OAuth user found
6. System checks if password already set
7. If password set: Returns JWT token directly
8. If no password: Returns `SET_PASSWORD_REQUIRED` response

### 5. Password Setup with Temporary Token
1. User receives temporary token from OAuth flow or password reset
2. User submits new password → `/auth/set-password`
3. System validates temporary token (expires in 15 minutes)
4. System validates password strength and format
5. System checks rate limits (max 5 attempts per user)
6. System updates password with BCrypt hashing
7. System sends confirmation email
8. System invalidates temporary token
9. User can now log in with email and new password

### Token Validation
- JWT signature validation
- Token expiration check
- User existence verification
- Password reset date validation
- Redis whitelist verification (if available)
- Temporary token validation (Redis-based with TTL)

## 📊 OTP Audit System

The application maintains comprehensive audit trails for OTP operations to ensure security and compliance.

### Audit Entry Creation
- **Generation**: Audit entries are created immediately when OTP is generated (during `/auth/authenticate`)
- **Purpose**: Tracks all OTP requests, regardless of verification success
- **Storage**: Persisted to `otp_audit_entries` table in MySQL

### Audit Benefits
- **Security Monitoring**: Track OTP generation patterns and potential abuse
- **Compliance**: Maintain records of authentication attempts
- **Forensics**: Investigate security incidents with complete OTP history
- **Rate Limiting**: Support rate limiting based on audit data

### Audit Data Captured
- Username requesting OTP
- Timestamp of OTP generation
- Client ID and device information
- OTP delivery status
- Verification attempts and outcomes

**Note**: Audit entries are created at generation time, not verification time, to ensure all OTP requests are logged for security purposes.

## 📚 API Documentation

The application provides interactive API documentation using Swagger/OpenAPI.

### Accessing Swagger UI
- **Development**: `http://localhost:8080/swagger-ui.html`
- **API Docs**: `http://localhost:8080/v3/api-docs`

### Features
- **Interactive Testing** - Test API endpoints directly from the browser
- **JWT Authentication** - Built-in support for JWT Bearer token authentication
- **Request/Response Examples** - Complete examples for all endpoints
- **Schema Documentation** - Detailed data models and validation rules

### Security Note
Swagger UI is enabled in development but disabled in production for security reasons.

## 📊 Application Monitoring

Spring Boot Actuator provides production-ready monitoring and management endpoints.

### Health Check Endpoints
```bash
# Application health status
GET /actuator/health

# Application information
GET /actuator/info

# Application metrics
GET /actuator/metrics

# Environment properties (dev only)
GET /actuator/env

# Configuration properties (dev only)
GET /actuator/configprops
```

### Monitoring Features
- **Health Checks** - Database, Redis, and application component status
- **Metrics** - JVM, HTTP requests, and custom business metrics
- **Environment Info** - Configuration properties and system information
- **Thread Dump** - JVM thread analysis (development only)
- **Heap Dump** - Memory analysis (development only)

### Security Configuration
- **Public Access**: `/actuator/health`, `/actuator/info`
- **Admin Only**: Other actuator endpoints require ADMIN role
- **Production**: Limited endpoints exposed for security

## 🧪 Testing

### Run All Tests
```bash
mvn test
```

### Run Specific Test Classes
```bash
mvn test -Dtest=UserServiceTest
mvn test -Dtest=TokenProviderTest
mvn test -Dtest=AuthenticationControllerTest
```

### Test Coverage
The project includes comprehensive test coverage:
- **Unit Tests**: Service layer, utilities, converters
- **Integration Tests**: REST controllers, authentication flow
- **Security Tests**: JWT token validation, OTP verification
- **Repository Tests**: Data access layer testing

### Key Test Classes (340+ Test Cases)
- **OAuth & Password Setup** (26+ cases)
  - `PasswordSetupServiceTest` - Password setup logic with rate limiting (13 cases)
  - `TemporaryPasswordTokenServiceTest` - Temporary token generation and validation (13 cases)
  - `AuthenticationControllerTest` - Google OAuth endpoints (6 cases)
  
- **Core Authentication** (80+ cases)
  - `AuthenticationControllerTest` - All authentication flows and edge cases (340+ total cases)
  - `TokenProviderTest` - JWT token generation and validation
  - `OtpAwareAuthenticationProviderTest` - OTP-aware authentication
  
- **OTP System** (60+ cases)
  - `OtpServiceTest` - OTP generation and verification logic (15 cases)
  - `OtpGeneratorTest` - OTP generation algorithms
  - `OtpRateLimiterImplTest` - Rate limiting and attempt tracking (9 cases)
  - `OtpAuditServiceImplTest` - Audit trail management
  - `OtpNotificationServiceImplTest` - OTP email delivery
  - `OtpAuditRetentionServiceTest` - Audit retention policies
  
- **User Management** (40+ cases)
  - `UserServiceTest` - User creation, activation, and management (25 cases)
  - `PublicUserResourceTest` - Public user endpoints
  - `PasswordChangeAuthorizationServiceTest` - Authorization checks (17 cases)
  
- **Token & Session** (40+ cases)
  - `RedisTokenServiceTest` - Token whitelist and Redis operations (17 cases)
  - `RefreshTokenServiceTest` - Refresh token lifecycle (19 cases)
  
- **Infrastructure** (20+ cases)
  - `EmailServiceTest` - Email sending functionality
  - `SecurityConfigurationTest` - Security configuration validation

## 📁 Profiles

### Development Profile (`dev`)
- MySQL database connection
- Embedded Redis server
- Email configuration for testing
- Debug logging enabled
- DDL auto-update enabled

```bash
mvn spring-boot:run -Dspring.profiles.active=dev
```

### Production Profile (`prod`)
- Optimized for production deployment
- External Redis required
- Production email settings
- Error logging only
- Enhanced security configurations

```bash
# Build for production
mvn clean package -Pprod

# Run production build
java -jar target/spring-boot-1.0-SNAPSHOT.jar --spring.profiles.active=prod
```

## 🔐 Security

### Security Features
- **Password Encryption**: BCrypt hashing
- **JWT Tokens**: Signed with HS256 algorithm
- **Token Blacklisting**: Redis-based token whitelist
- **OTP Security**: Time-based expiration and attempt limiting
- **CORS Protection**: Configured for specific origins
- **Session Management**: Stateless authentication
- **Input Validation**: Jakarta validation annotations

### Security Headers
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `X-XSS-Protection: 1; mode=block`

## 🏗 Production Deployment

### Build for Production
```bash
mvn clean
mvn -Pprod package
```

### Environment Variables
```bash
export DB_URL=jdbc:mysql://prod-db-server/otp
export DB_USERNAME=prod_user
export DB_PASSWORD=prod_password
export REDIS_HOST=prod-redis-server
export JWT_SECRET=your-production-secret
export MAIL_USERNAME=production@email.com
export MAIL_PASSWORD=production-password
```

### Docker Support
```dockerfile
FROM openjdk:17-jre-slim
WORKDIR /app
COPY target/spring-boot-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Health Checks
The application includes health check endpoints:
- `GET /actuator/health` - Overall application health
- `GET /actuator/info` - Application information

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines
- Follow Spring Boot best practices
- Maintain test coverage above 80%
- Use proper logging levels
- Document new API endpoints
- Follow existing code patterns

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

For support and questions:
- Create an issue in the GitHub repository
- Check existing documentation
- Review test cases for usage examples

---

**Note**: This project has been migrated to Spring Boot 3.2.3 and Java 17 for improved security, performance, and long-term maintainability. It includes comprehensive API documentation (Swagger) and monitoring capabilities (Actuator). The application is designed for educational and demonstration purposes. For production use, ensure proper security auditing and compliance with your organization's security policies.

**Migration Status**: ✅ Successfully migrated from Spring Boot 1.5.7 to 3.2.3 with Jakarta EE compatibility.
**Recent Features**: 
- ✅ Added Swagger/OpenAPI documentation and Spring Boot Actuator monitoring
- ✅ Implemented Google OAuth 2.0 integration with automatic user provisioning
- ✅ Added secure password setup flow with temporary tokens for OAuth users
- ✅ Implemented password strength validation with configurable rules
- ✅ Added rate limiting for password set attempts
- ✅ Created 340+ comprehensive test cases covering all authentication flows
- ✅ Full Redis-based temporary token system with TTL management