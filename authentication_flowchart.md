# Spring Boot OTP Authentication Flowchart

```mermaid
flowchart TD
    %% Start
    A["User Login Request"] --> B{{"Select Auth Method"}}
    B -->|Traditional| B1["POST /authenticate"]
    B -->|OAuth| B2["POST /auth/google"]
    
    %% Traditional Authentication Path
    B1 --> C["authorize: Check credentials via OtpAwareAuthenticationProvider"]
    C --> D["Load User: From Redis cache (auth:user:{username}, 5min) or DB"]
    D --> E["Validate Credentials: PasswordEncoder.matches()"]
    E --> F{"OTP Required? Check user.getIsOtpRequired()"}
    F -->|Yes| G["generateOtp: Create & send OTP via OtpService"]
    G --> H["Send OTP Email: Async via IEmailService.sendSimpleMessageAsync()"]
    H --> I["Cache DomainUserDetails: Redis (otp:user:{username}, 5min) via TokenProvider.cacheUserDetailsForOtp()"]
    I --> J["Return pendingOtp: TokenCreationResponse.pendingOtp()"]
    F -->|No| K["createToken: Generate JWT via TokenProvider"]
    K --> L["Generate JWT: TokenProvider.generateToken() with Jwts.builder()"]
    L --> M["Register JTI: Redis whitelist via IRedisTokenService.registerJti()"]
    M --> N["Return accepted: TokenCreationResponse.accepted() with JWT"]
    
    %% Google OAuth Path
    B2 --> B3["Receive Google ID Token"]
    B3 --> B4["verifyAndExtractUserInfo: IGoogleOAuthService.verify token"]
    B4 --> B5{{"Token Valid?"}}
    B5 -->|No| B6["Log Error: LocalizationService"]
    B6 --> B7["Return UNAUTHORIZED: Invalid Google ID token"]
    B5 -->|Yes| B8["Extract User Info: email, given_name, family_name, sub"]
    B8 --> B9["findOrCreateGoogleOAuthUser: UserService"]
    B9 --> B10{{"User Exists by Google ID?"}}
    B10 -->|Yes| B11["Return Existing User"]
    B10 -->|No, Email Exists| B12["Link Google OAuth to Existing User"]
    B10 -->|New User| B13["Create New Google OAuth User (OTP disabled)"]
    B11 --> B14["createAccessTokenAfterVerifiedOtp: Generate JWT"]
    B12 --> B14
    B13 --> B14
    B14 --> B15["createRefreshToken: Generate refresh token"]
    B15 --> B16["Return: JWT + Refresh Token with remember_me flag"]
    
    %% OTP Verification Path
    J --> O["User Enters OTP"]
    O --> P["verifyOtp: POST /verify via AuthenticationController"]
    P --> Q["validateOTP: Check OTP via OtpService.validateOTP() against Redis"]
    Q --> R{{"Validation Success? OtpValidationStatus.SUCCESS"}}
    R -->|Yes| S["createTokenAfterVerifiedOtp: Generate JWT post-OTP via TokenProvider"]
    S --> T["Retrieve Cached: DomainUserDetails via TokenProvider.getCachedUserDetailsForOtp() (ObjectMapper.readValue)"]
    T --> U["Construct User: Build User object from cached data"]
    U --> V["Generate JWT: TokenProvider.generateToken()"]
    V --> W["Register JTI: Redis via IRedisTokenService.registerJti()"]
    W --> X["Return JWT: JWTToken.bearerToken()"]
    %% Password Change Flow
    AAA["Password Change Request"] --> BBB["changePassword: PUT /api/users/public/password"]
    BBB --> CCC["Validate Current Password: PasswordEncoder.matches()"]
    CCC --> DDD["Update Password: PasswordEncoder.encode()"]
    DDD --> EEE["Save User: userRepository.save()"]
    EEE --> FFF["Clear Caches: authCacheService.clearAllCachesForUser() + revokeAllUserRefreshTokens()"]
    FFF --> GGG["Clear Auth Cache: Redis delete auth:user:{username}"]
    FFF --> HHH["Clear Token Whitelist: IRedisTokenService.removeWhitelist(userId)"]
    FFF --> III["Revoke Refresh Tokens: refreshTokenService.revokeAllUserRefreshTokens(userId)"]
    III --> JJJ["Return Success: UserResponseDTO"]
    %% Cache Impact on Authentication
    JJJ --> KKK["Next Login: Forces DB lookup (cache cleared)"]
    KKK --> D
    %% Refresh Token Flow
    LLL["Access Token Expired"] --> MMM["refreshToken: POST /auth/refresh"]
    MMM --> NNN["Validate Refresh Token: refreshTokenService.validateRefreshToken()"]
    NNN --> OOO["Get User: userRepository.findById()"]
    OOO --> PPP["Rotate Refresh Token: refreshTokenService.rotateRefreshToken()"]
    PPP --> QQQ["Create New Access Token: TokenProvider.createAccessTokenAfterVerifiedOtp()"]
    QQQ --> RRR["Return New Tokens: JWTToken with access + refresh tokens"]
    RRR --> SSS["Client Updates Tokens"]
    %% Negative Paths
    E -->|Invalid Credentials| Y["BadCredentialsException: HttpStatus.UNAUTHORIZED<br/>LocalizationService"]
    G -->|Rate Limit Exceeded| Z["rateLimited: OtpGenerationResult.rateLimited() - Redis key otp:{username}:rate_limit (15s)"]
    G -->|Max Attempts Exceeded| AA["maxAttemptsExceeded: OtpGenerationResult.maxAttemptsExceeded() - Redis key otp:{username}:attempts"]
    AA --> AB["Send Lockout Email: Async via IEmailService.sendSimpleMessageAsync()"]
    Q -->|Invalid/Expired OTP| BB["invalid: OtpValidationResult.invalid() - HttpStatus.UNAUTHORIZED<br/>LocalizationService"]
    Q -->|Locked Account| CC["locked: OtpValidationResult.locked() - HttpStatus.LOCKED<br/>LocalizationService"]
    CCC -->|Invalid Current Password| KKK["BadCredentialsException: HttpStatus.UNAUTHORIZED<br/>LocalizationService"]
    B4 -->|Network Error| B17["Network Error During Verification"]
    B17 --> B18["Log Error: LocalizationService"]
    B18 --> B7
    %% Caching Details
    D --> DD["User Cache: ObjectMapper.writeValueAsString() in OtpAwareAuthenticationProvider"]
    I --> EE["OTP Cache: ObjectMapper.writeValueAsString() in TokenProvider (with JavaTimeModule & SimpleGrantedAuthorityDeserializer)"]
    FFF --> LL["Cache Clearing: Prevents stale password authentication"]
    %% Scheduling
    FF["Scheduled: purgeExpiredEntries via OtpAuditRetentionService @Scheduled(cron=OtpConstants.OTP_AUDIT_PURGE_CRON)"]
    FF --> GG["Delete Expired: OtpAuditEntryRepository.deleteByExpiresOnBefore()"]
    FF --> HH["Log: SLF4J info/debug"]
    %% Negative Paths for Refresh
    NNN -->|Invalid Token| TTT["Invalid Refresh Token: HttpStatus.UNAUTHORIZED<br/>LocalizationService"]
    %% End
    N --> II["Success: Authentication completed"]
    X --> II
    B16 --> II
    Y --> JJ["Failure: Authentication failed<br/>LocalizationService Response"]
    Z --> JJ
    BB --> JJ
    CC --> JJ
    TTT --> JJ
    B7 --> JJ
```

## Key Changes Made

### Google OAuth Integration
- **Added OAuth authentication path** (`B2` → `B16`) as alternative to traditional login
- **Token verification** (`B4` → `B5`) via IGoogleOAuthService.verifyAndExtractUserInfo()
- **User management** (`B9` → `B13`) with three scenarios:
  - Existing Google OAuth user authentication
  - Linking Google OAuth to existing email-based account
  - Creating new Google OAuth users (OTP disabled by default)
- **Automatic token generation** for OAuth users without OTP requirement
- **Refresh token support** for OAuth flows with remember_me flag
- **Error handling** for invalid/expired tokens with proper HTTP status codes

### Localization Service Integration
- **Error message localization** across all authentication paths:
  - Invalid credentials (Traditional auth)
  - Invalid/Expired OTP validation
  - Locked accounts
  - Invalid refresh tokens
  - Invalid Google ID tokens
  - Network errors during verification
- **Centralized messaging** via LocalizationService for consistent user experience
- **Multi-language support** for all error responses

### Password Change Integration
- **Added password change flow** (`AAA` → `JJJ`) showing the complete process
- **Enhanced cache clearing** (`FFF` → `III`) that clears auth cache, token whitelist, AND refresh tokens
- **Cache impact** (`JJJ` → `KKK`) showing how password changes force fresh DB lookups

### Refresh Token Implementation
- **Refresh token flow** (`LLL` → `SSS`) for seamless token renewal
- **Token rotation** (`PPP`) for enhanced security - old refresh tokens are revoked when new ones are issued
- **Updated access token creation** (`QQQ`) uses TokenProvider.createAccessTokenAfterVerifiedOtp() to avoid OTP-triggered side effects during refresh
- **Dual token response** - both access and refresh tokens returned on authentication and refresh

### Security Improvements
- **Immediate cache invalidation** prevents authentication with old passwords
- **Token whitelist clearing** ensures old JWTs are invalidated
- **Refresh token revocation** on password change prevents token reuse
- **Token rotation** prevents refresh token replay attacks
- **Fresh DB lookup** guarantees latest user data is used
- **OAuth user isolation** - Google OAuth users exempt from OTP requirements

### Flow Connections
- Dual authentication entry points (Traditional + OAuth)
- Password change success connects back to authentication flow
- Refresh token flow handles expired access tokens seamlessly
- Comprehensive error handling with localization for all authentication scenarios
- Scheduled cleanup for expired tokens and OTP entries

This updated flowchart reflects the complete JWT authentication system with Google OAuth support, refresh tokens, and multi-language error messaging, ensuring both security and enhanced user experience.