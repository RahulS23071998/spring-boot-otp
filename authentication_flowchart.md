# Spring Boot OTP Authentication Flowchart

```mermaid
flowchart TD
    %% Start - Authentication Methods
    A["User Login Request"] --> B{{"Select Auth Method"}}
    B -->|Traditional| B1["POST /authenticate"]
    B -->|OAuth| B2["POST /auth/google"]
    
    %% ========== STANDARD EMAIL/PASSWORD FLOW ==========
    B1 --> C["authorize: Check credentials via OtpAwareAuthenticationProvider"]
    C --> D["Load User: From Redis cache auth:user:{username} 5min or DB"]
    D --> E["Validate Credentials: PasswordEncoder.matches()"]
    E --> F{{"OTP Required?"}}
    F -->|Yes| G["generateOtp: Create & send OTP via OtpService"]
    G --> H["Send OTP Email: Async via IEmailService.sendSimpleMessageAsync()"]
    H --> I["Cache DomainUserDetails: Redis otp:user:{username} 5min"]
    I --> J["Return OTP_PENDING: TokenCreationResponse.pendingOtp()"]
    F -->|No| K["createToken: Generate JWT via TokenProvider"]
    K --> K1["Revoke Old Tokens: refreshTokenService.revokeAllUserRefreshTokens()"]
    K1 --> L["Generate JWT: TokenProvider.generateToken() HS256"]
    L --> M["Register JTI: Redis whitelist via IRedisTokenService.registerJti()"]
    M --> N["Return AUTHENTICATED: TokenCreationResponse.accepted() with JWT"]
    
    %% ========== LOGOUT FLOW ==========
    LogoutReq["Logout Request"] --> LogoutType{{"Logout Type"}}
    LogoutType -->|Current Session| L1["POST /auth/logout"]
    L1 --> L2["Extract JTI from Token"]
    L2 --> L3["Blacklist Token: Redis blacklist"]
    L3 --> L4["Clear Context: SecurityContextHolder.clearContext()"]
    L4 --> L5["Return SUCCESS"]
    
    LogoutType -->|All Sessions| LA1["POST /auth/logout-all-sessions"]
    LA1 --> LA2["Revoke All Refresh Tokens: DB"]
    LA2 --> LA3["Clear Auth Cache: Redis"]
    LA3 --> LA4["Blacklist Current Token"]
    LA4 --> LA5["Return SUCCESS"]

    %% ========== OTP VERIFICATION FLOW ==========
    J --> O["User Enters OTP"]
    O --> P["verifyOtp: POST /auth/verify"]
    P --> Q["validateOTP: Check OTP via OtpService.validateOTP()"]
    Q --> R{{"OTP Valid?"}}
    R -->|Yes| S["createTokenAfterVerifiedOtp: Generate JWT post-OTP"]
    S --> S1["Revoke Old Tokens: refreshTokenService.revokeAllUserRefreshTokens()"]
    S1 --> T["Retrieve Cached: DomainUserDetails from Redis"]
    T --> U["Construct User: Build object from cached data"]
    U --> V["Generate JWT: TokenProvider.generateToken()"]
    V --> W["Register JTI: Redis whitelist"]
    W --> X["Return JWT: JWTToken.bearerToken()"]
    
    %% ========== GOOGLE OAUTH FLOW - NEW USER ==========
    B2 --> B3["Receive Google ID Token"]
    B3 --> B4["verifyAndExtractUserInfo: IGoogleOAuthService.verify()"]
    B4 --> B5{{"Token Valid?"}}
    B5 -->|No| B7["Return UNAUTHORIZED: Invalid Google ID token"]
    B5 -->|Yes| B8["Extract User Info: email, sub, given_name, family_name"]
    B8 --> B9["findOrCreateGoogleOAuthUser: UserService"]
    B9 --> B10{{"User Exists by Google ID?"}}
    B10 -->|Yes| B11["Return Existing User"]
    B10 -->|No, Email Exists| B12["Link Google OAuth to Existing User"]
    B10 -->|New User| B13["Create New Google OAuth User<br/>OTP disabled, passwordSet=false"]
    B11 --> B20{{"Has Password Set?"}}
    B12 --> B20
    B13 --> B20
    
    %% ========== PASSWORD SETUP FLOW (OAuth Users) ==========
    B20 -->|No| B21["Check Rate Limit: password-set:{username}<br/>Max 5 attempts per user"]
    B21 --> B22{{"Rate Limit Exceeded?"}}
    B22 -->|Yes| B23["Return 429: Too many attempts"]
    B22 -->|No| B24["generateTemporaryToken: TemporaryPasswordTokenService"]
    B24 --> B25["Store in Redis: password-reset:{username}<br/>Validity: 15 minutes"]
    B25 --> B26["Return SET_PASSWORD_REQUIRED<br/>with temporary_token to client"]
    B26 --> B27["User Submits Password: POST /auth/set-password"]
    B27 --> B28["validateTemporaryToken: Check token in Redis"]
    B28 --> B29{{"Token Valid?"}}
    B29 -->|No| B30["Return 401: Token expired or invalid"]
    B29 -->|Yes| B31["validatePasswordStrength:<br/>Min 8 chars, uppercase, lowercase, digit, special char"]
    B31 --> B32{{"Password Valid?"}}
    B32 -->|No| B33["Return 400: Validation message"]
    B32 -->|Yes| B34["Check Rate Limit Again: password-set:{username}"]
    B34 --> B35{{"Rate Limit Exceeded?"}}
    B35 -->|Yes| B36["Return 429: Too many attempts"]
    B35 -->|No| B37["updateUserPasswordWithValidation:"]
    B37 --> B38["Encode Password: BCrypt via PasswordEncoder"]
    B38 --> B39["Set passwordSet=true in DB"]
    B39 --> B40["Save User: userRepository.save()"]
    B40 --> B41["invalidateTemporaryToken: Delete from Redis"]
    B41 --> B42["resetAttempts: Clear rate limit counter"]
    B42 --> B43["sendPasswordSetNotification: Email confirmation"]
    B43 --> B44["Return SUCCESS: Password set confirmed"]
    
    %% ========== GOOGLE OAUTH MFA FLOW ==========
    B20 -->|Yes| GM1["Initiate MFA: POST /auth/google-oauth-mfa"]
    GM1 --> GM2{{"Has Persistent Secret?"}}
    GM2 -->|No| GM3["Generate New Secret: TotpService"]
    GM3 --> GM4["Save Secret: DB (Persistent)"]
    GM4 --> GM5["Return MFA_REQUIRED + QR Code"]
    GM2 -->|Yes| GM6["Retrieve Secret: DB (Persistent)"]
    GM6 --> GM7["Return MFA_REQUIRED (No QR Code)"]
    
    GM5 --> GM8["User Scans QR & Enters Code"]
    GM7 --> GM8
    GM8 --> GM9["Verify MFA: POST /auth/google-mfa-verify"]
    GM9 --> GM10["Check Replay: Redis totp:used:{userId}"]
    GM10 --> GM11{{"Code Used?"}}
    GM11 -->|Yes| GM12["Return 401: Replay Detected"]
    GM11 -->|No| GM13["Validate Code: GoogleAuthenticator"]
    GM13 --> GM14{{"Code Valid?"}}
    GM14 -->|No| GM15["Return 401: Invalid Code"]
    GM14 -->|Yes| GM16["Mark Used: Redis (30s TTL)"]
    GM16 --> GM17["Generate JWT Tokens"]
    GM17 --> GM18["Return SUCCESS: JWT + Refresh Token"]

    %% ========== GOOGLE OAUTH FLOW - RETURNING USER (Legacy/Direct) ==========
    %% B20 -->|Yes| B50["createAccessTokenAfterVerifiedOtp: Generate JWT"]
    %% B50 --> B50A["Revoke Old Tokens: refreshTokenService.revokeAllUserRefreshTokens()"]
    %% B50A --> B51["createRefreshToken: Generate refresh token"]
    %% B51 --> B52["Return SUCCESS: JWT + Refresh Token<br/>with remember_me flag"]
    
    %% ========== PASSWORD CHANGE FLOW ==========
    AAA["Password Change Request"] --> BBB["changePassword: PUT /api/users/public/password"]
    BBB --> CCC["Validate Current Password: PasswordEncoder.matches()"]
    CCC --> DDD["Update Password: PasswordEncoder.encode()"]
    DDD --> EEE["Save User: userRepository.save()"]
    EEE --> FFF["Clear Caches:"]
    FFF --> GGG["Clear Auth Cache: Redis delete auth:user:{username}"]
    FFF --> HHH["Clear Token Whitelist: IRedisTokenService.removeWhitelist()"]
    FFF --> III["Revoke Refresh Tokens: refreshTokenService.revokeAllUserRefreshTokens()"]
    III --> JJJ["Return SUCCESS: UserResponseDTO"]
    JJJ --> KKK["Next Login: Forces DB lookup cache cleared"]
    KKK --> D
    
    %% ========== TOKEN REFRESH FLOW ==========
    LLL["Access Token Expired"] --> MMM["refreshToken: POST /auth/refresh"]
    MMM --> NNN["Validate Refresh Token: refreshTokenService.validateRefreshToken()"]
    NNN --> OOO["Get User: userRepository.findById()"]
    OOO --> PPP["Rotate Refresh Token: refreshTokenService.rotateRefreshToken()"]
    PPP --> QQQ["Create New Access Token: TokenProvider.createAccessTokenAfterVerifiedOtp()"]
    QQQ --> RRR["Return New Tokens: JWTToken with access + refresh"]
    RRR --> SSS["Client Updates Tokens"]
    
    %% ========== ERROR PATHS ==========
    E -->|Invalid Credentials| Y["UNAUTHORIZED: Invalid credentials"]
    G -->|Rate Limit Exceeded| Z["RATE_LIMITED: 3 per 10 min<br/>Redis key otp:{username}:rate_limit 15s"]
    G -->|Max Attempts Exceeded| AA["MAX_ATTEMPTS_EXCEEDED: max_attempts_exceeded()<br/>Redis key otp:{username}:attempts"]
    AA --> AB["Send Lockout Email"]
    Q -->|Invalid/Expired OTP| BB["UNAUTHORIZED: Invalid OTP"]
    Q -->|Locked Account| CC["LOCKED: Account locked"]
    CCC -->|Invalid Current Password| KKK["UNAUTHORIZED: Invalid password"]
    B4 -->|Network Error| B17["Network Error During Verification"]
    B17 --> B7
    NNN -->|Invalid Token| TTT["UNAUTHORIZED: Invalid refresh token"]
    B23 --> JJ["FAILURE: Too many attempts"]
    B30 --> JJ
    B33 --> JJ
    B36 --> JJ
    
    %% ========== SUCCESS PATHS ==========
    N --> II["✓ Authentication Complete"]
    X --> II
    B52 --> II
    B44 --> II
    RRR --> II
    
    %% ========== FAILURE PATHS ==========
    Y --> JJ["✗ Authentication Failed"]
    Z --> JJ
    BB --> JJ
    CC --> JJ
    TTT --> JJ
    B7 --> JJ
    
    %% ========== CACHING & SCHEDULING ==========
    FF["Scheduled: OtpAuditRetentionService<br/>@Scheduled purgeExpiredEntries"]
    FF --> GG["Delete Expired: OtpAuditEntryRepository"]
    
    %% ========== TOKEN SPECIFICATIONS ==========
    style II fill:#90EE90
    style JJ fill:#FFB6C6
    style B20 fill:#FFF4B0
    style B22 fill:#FFD700
    style B29 fill:#FFD700
    style B32 fill:#FFD700
    style B35 fill:#FFD700
    style R fill:#FFD700
    style F fill:#FFD700
    style B5 fill:#FFD700
    style B10 fill:#FFD700
```

**Token & Configuration Specifications:**
- **JWT Token**: Expiry 5 minutes, Algorithm HS256, stored in Redis whitelist
- **Refresh Token**: Expiry 7 days, rotated on refresh, revoked on password change
- **OTP**: Expiry 5 minutes, Max 3 attempts per 10 minutes, Rate limit 15s
- **Temporary Token**: Expiry 15 minutes, Rate limit max 5 password set attempts per user
- **User Cache**: 5 minutes (auth:user:{username})
- **OTP Cache**: 5 minutes (otp:user:{username})

## Latest Flows Implemented

### 1. Standard Email/Password Flow
- User submits credentials via `/auth/authenticate`
- `OtpAwareAuthenticationProvider` validates credentials
- If OTP required: Generate & email OTP, return `OTP_PENDING`
- If OTP disabled: Generate & return JWT token immediately
- User accesses protected endpoints with JWT

### 2. OTP-Based Authentication Flow
- After successful credential validation with OTP enabled
- OTP generated (max 3 per 10 min, expires 5 min)
- User enters OTP via `/auth/verify`
- System validates OTP and generates JWT
- User details cached during OTP wait and retrieved after validation

### 3. Google OAuth Flow - New User with Password Setup
- User initiates Google login → `/auth/google`
- System verifies token with Google OAuth service
- New user created with `passwordSet=false`
- Temporary token generated (15 min validity)
- System returns `SET_PASSWORD_REQUIRED` with temporary token
- User must set password via `/auth/set-password`:
  - **Password Validation**: Min 8 chars, uppercase, lowercase, digit, special char
  - **Rate Limiting**: Max 5 attempts per user
  - **Token Validation**: Temporary token must be valid
  - **Processing**: BCrypt encoding, `passwordSet=true`, confirmation email
  - **Token Cleanup**: Temporary token invalidated after success

### 4. Google OAuth Flow - Returning User (MFA Enabled)
- User initiates Google login → `/auth/google-oauth-mfa`
- Token verified, existing user found
- System checks `passwordSet` flag
- If password set:
  - **Check Persistent Secret**: Does user have a stored TOTP secret?
  - **No (First Time)**: Generate new secret, save to DB, return QR Code
  - **Yes (Returning)**: Retrieve secret, return `MFA_REQUIRED` (no QR Code)
- User verifies via `/auth/google-mfa-verify`:
  - **Replay Protection**: Check Redis if code was used in last 30s
  - **Validation**: Verify code against stored secret
  - **Success**: Mark code as used (Redis), issue JWT tokens

### 5. Password Setup with Temporary Token
- Triggered by OAuth new user flow
- **Rate Limit Check** (max 5 per user): `password-set:{username}`
- **Token Generation**: 15-minute validity in Redis (`password-reset:{username}`)
- **Password Submission**: `/auth/set-password` validates:
  - Temporary token exists and is valid
  - Password meets all strength requirements
  - Rate limit not exceeded on attempt
- **Success Flow**: BCrypt encode, set flag, invalidate token, send email
- **Error Handling**: 400 (validation), 401 (token), 429 (rate limit)

### 6. Token Refresh Flow
- When JWT access token expires (5 min)
- Client submits refresh token via `/auth/refresh`
- System validates refresh token (7 day validity)
- Token rotation: Old refresh token revoked, new one issued
- New access token generated and returned
- Seamless token renewal without re-authentication

### 7. Logout Flow
- **Current Session**: `/auth/logout` invalidates current JWT via Redis blacklist
- **All Sessions**: `/auth/logout-all-sessions` revokes all refresh tokens and clears caches

### 8. Password Change Flow
- User changes password: `PUT /api/users/public/password`
- Current password validated
- New password encoded with BCrypt
- **Cache Cleanup**:
  - Clear auth cache: `auth:user:{username}`
  - Clear token whitelist: `IRedisTokenService.removeWhitelist()`
  - Revoke all refresh tokens
- Next login forced to use fresh DB data
- All existing sessions invalidated

### Password Setup Security Features
- **Temporary Token System**: Short-lived tokens (15 min) stored in Redis with automatic expiration
- **Rate Limiting**: Max 5 password set attempts per user prevents brute force attacks
- **Password Strength Validation**: 
  - Minimum 8 characters
  - Must contain uppercase letter
  - Must contain lowercase letter
  - Must contain digit
  - Must contain special character
- **BCrypt Hashing**: Industry-standard password encryption
- **Token Invalidation**: Temporary tokens cleared after successful password set
- **Email Confirmation**: User notified of successful password setup
- **Graceful Error Handling**: Specific HTTP status codes (400, 401, 429)

### Security Features Across All Flows
- **JWT Signature Validation**: HS256 algorithm with secret key
- **Token Expiration**: Access tokens (5 min), Refresh tokens (7 days)
- **Redis Whitelist**: Tokens registered and validated against Redis
- **Token Rotation**: Refresh tokens rotated on use, old tokens revoked
- **Password Change Cache Invalidation**: All caches and tokens cleared on password change
- **OTP Security**: Time-based expiration, max attempt tracking, lockout mechanism
- **Persistent TOTP**: Secrets stored in DB, replay protection via Redis (30s TTL)
- **Rate Limiting**: Multiple layers - OTP (3/10min), Password Set (5/user), OAuth (10/min)
- **Audit Trails**: OTP generation and verification logged for compliance

### Authentication Decision Points (Color Coded Yellow)
1. **OTP Required?** (Flow F) - User OTP setting check
2. **Token Valid?** (Flow B5) - Google OAuth token verification
3. **User Exists?** (Flow B10) - New vs existing user determination
4. **Has Password Set?** (Flow B20) - OAuth user password requirement check
5. **Rate Limit Exceeded?** (Flows B22, B35) - Rate limit checks
6. **Token Valid?** (Flow B29) - Temporary token validation
7. **Password Valid?** (Flow B32) - Password strength validation
8. **OTP Valid?** (Flow R) - OTP verification

### Flow Connections
- Dual authentication entry points: Traditional email/password + Google OAuth
- Password setup flow triggered only for new OAuth users without passwords
- Password change connects back to authentication via cache clearing
- Refresh token flow seamlessly extends access token validity
- Comprehensive error paths with specific HTTP status codes
- All success paths converge at authentication completion
- Scheduled background cleanup of expired OTP audit entries

### System Architecture Highlights
- **Service Layer**: Separated concerns with dedicated services for OTP, OAuth, Password Setup, Tokens
- **Redis Integration**: Multi-purpose caching for user data, tokens, rate limits, temporary tokens
- **Database Persistence**: User profiles, OAuth metadata, audit trails
- **Async Email**: Non-blocking email delivery for OTP and notifications
- **Transaction Management**: ACID compliance for password updates
- **Monitoring & Logging**: Comprehensive logging at all decision points