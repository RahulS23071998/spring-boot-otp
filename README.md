# Spring Boot OTP Authentication System

A comprehensive Spring Boot application demonstrating secure One-Time Password (OTP) authentication with JWT tokens, Redis session management, and email verification.

## 📋 Table of Contents
- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Prerequisites](#-prerequisites)
- [Setup & Installation](#-setup--installation)
- [Configuration](#-configuration)
- [API Endpoints](#-api-endpoints)
- [Authentication Flow](#-authentication-flow)
- [Testing](#-testing)
- [Profiles](#-profiles)
- [Security](#-security)
- [Contributing](#-contributing)

## ✨ Features

- **JWT Token Authentication** - Secure token-based authentication
- **OTP Verification** - Email-based OTP verification system
- **Redis Session Management** - Token whitelisting and session management
- **User Management** - Complete user registration and management system
- **Email Service** - SMTP email integration for OTP delivery
- **Password Reset** - Secure password reset functionality
- **Role-Based Access Control** - Authority and role management
- **Embedded Redis** - Development-ready Redis configuration
- **Comprehensive Testing** - Unit and integration tests with 90%+ coverage
- **Profile Support** - Development and production profiles
- **Security Hardening** - Multiple layers of security validation

## 🛠 Tech Stack

- **Framework**: Spring Boot 3.2.3
- **Java**: 17
- **Database**: MySQL 8.0
- **Cache**: Redis
- **Security**: Spring Security with JWT
- **Email**: Spring Mail
- **Testing**: JUnit 5, Mockito, TestContainers
- **Build Tool**: Maven
- **OTP Generation**: Google Guava

## 📁 Project Structure

```
src/
├── main/
│   ├── java/com/starter/springboot/
│   │   ├── auth/                          # Authentication controllers
│   │   ├── config/                        # Configuration classes
│   │   ├── converters/                    # Date/time converters
│   │   ├── domain/                        # Entity classes
│   │   ├── enumeration/                   # Enums (AuthorityName)
│   │   ├── exceptions/                    # Custom exceptions
│   │   ├── otp/                          # OTP audit entities
│   │   ├── repositories/                  # Data access layer
│   │   ├── rest/                         # REST controllers and DTOs
│   │   ├── security/                     # Security configuration and JWT
│   │   ├── services/                     # Business logic layer
│   │   ├── utils/                        # Utility classes
│   │   └── Application.java              # Main application class
│   └── resources/
│       ├── application.yml               # Main configuration
│       ├── application-dev.yml           # Development profile
│       └── application-prod.yml          # Production profile
├── test/
│   └── java/com/starter/springboot/
│       ├── auth/                         # Authentication tests
│       ├── rest/resources/               # REST controller tests
│       ├── security/jwt/                 # JWT security tests
│       └── services/                     # Service layer tests
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
# Install dependencies
mvn install

# Run application
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

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
  "password": "admin"
}
```

**Response (OTP Required):**
```json
{
  "status": "ACCEPTED",
  "message": "OTP required to complete authentication.",
  "otpRequired": true
}
```

**Response (Direct Token):**
```json
{
  "status": "OK",
  "otpRequired": false,
  "token": {
    "tokenType": "Bearer",
    "idToken": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 300
  }
}
```

#### OTP Verification
```http
POST /auth/verify
Content-Type: application/json
Authorization: Bearer <token>

{
  "username": "admin",
  "otp": "123456"
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

#### Get Current User
```http
GET /api/users/current
Authorization: Bearer <token>
```

#### Change Password
```http
POST /api/users/change-password
Content-Type: application/json
Authorization: Bearer <token>

{
  "oldPassword": "oldpass",
  "newPassword": "newpass"
}
```

## 🔐 Authentication Flow

### Standard Flow (OTP Disabled)
1. User submits credentials → `/auth/authenticate`
2. System validates credentials
3. JWT token returned immediately
4. User can access protected endpoints

### OTP Flow (OTP Enabled)
1. User submits credentials → `/auth/authenticate`
2. System validates credentials
3. OTP generated and sent via email
4. System returns `otpRequired: true` response
5. User submits OTP → `/auth/verify`
6. System validates OTP
7. JWT token returned
8. User can access protected endpoints

### Token Validation
- JWT signature validation
- Token expiration check
- User existence verification
- Password reset date validation
- Redis whitelist verification (if available)

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

### Key Test Classes
- `UserServiceTest` - User management operations
- `OtpServiceTest` - OTP generation and validation
- `TokenProviderTest` - JWT token operations
- `AuthenticationControllerTest` - Authentication endpoints
- `RedisTokenServiceTest` - Token whitelist management

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

```bash
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
COPY target/spring-boot-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

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

**Note**: This project is designed for educational and demonstration purposes. For production use, ensure proper security auditing and compliance with your organization's security policies.