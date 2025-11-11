# Spring Boot OTP Authentication Flowchart

```mermaid
flowchart TD
    %% Start
    A["User Login Request"] --> B["authorize: POST /authenticate"]
    B --> C{"authenticate: Check credentials via OtpAwareAuthenticationProvider"}
    %% Positive Authentication Path
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
    %% OTP Verification Path
    J --> O["User Enters OTP"]
    O --> P["verifyOtp: POST /verify via AuthenticationController"]
    P --> Q["validateOTP: Check OTP via OtpService.validateOTP() against Redis"]
    Q --> R{"Validation Success? OtpValidationStatus.SUCCESS"}
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
    EEE --> FFF["Clear Caches: authCacheService.clearAllCachesForUser()"]
    FFF --> GGG["Clear Auth Cache: Redis delete auth:user:{username}"]
    FFF --> HHH["Clear Token Whitelist: IRedisTokenService.removeWhitelist(userId)"]
    HHH --> III["Return Success: UserResponseDTO"]
    %% Cache Impact on Authentication
    III --> JJJ["Next Login: Forces DB lookup (cache cleared)"]
    JJJ --> D
    %% Negative Paths
    E -->|Invalid Credentials| Y["BadCredentialsException: HttpStatus.UNAUTHORIZED"]
    G -->|Rate Limit Exceeded| Z["rateLimited: OtpGenerationResult.rateLimited() - Redis key otp:{username}:rate_limit (15s)"]
    G -->|Max Attempts Exceeded| AA["maxAttemptsExceeded: OtpGenerationResult.maxAttemptsExceeded() - Redis key otp:{username}:attempts"]
    AA --> AB["Send Lockout Email: Async via IEmailService.sendSimpleMessageAsync()"]
    Q -->|Invalid/Expired OTP| BB["invalid: OtpValidationResult.invalid() - HttpStatus.UNAUTHORIZED"]
    Q -->|Locked Account| CC["locked: OtpValidationResult.locked() - HttpStatus.LOCKED"]
    CCC -->|Invalid Current Password| KKK["BadCredentialsException: HttpStatus.UNAUTHORIZED"]
    %% Caching Details
    D --> DD["User Cache: ObjectMapper.writeValueAsString() in OtpAwareAuthenticationProvider"]
    I --> EE["OTP Cache: ObjectMapper.writeValueAsString() in TokenProvider (with JavaTimeModule & SimpleGrantedAuthorityDeserializer)"]
    FFF --> LL["Cache Clearing: Prevents stale password authentication"]
    %% Scheduling
    FF["Scheduled: purgeExpiredEntries via OtpAuditRetentionService @Scheduled(cron=OtpConstants.OTP_AUDIT_PURGE_CRON)"]
    FF --> GG["Delete Expired: OtpAuditEntryRepository.deleteByExpiresOnBefore()"]
    FF --> HH["Log: SLF4J info/debug"]
    %% End
    N --> II["Success: Authentication completed"]
    X --> II
    III --> II
    Y --> JJ["Failure: Authentication failed"]
    Z --> JJ
    BB --> JJ
    CC --> JJ
    KKK --> JJ
```

## Key Changes Made

### Password Change Integration
- **Added password change flow** (`AAA` → `III`) showing the complete process
- **Cache clearing mechanism** (`FFF` → `HHH`) that clears both auth cache and token whitelist
- **Cache impact** (`III` → `JJJ`) showing how password changes force fresh DB lookups

### Security Improvements
- **Immediate cache invalidation** prevents authentication with old passwords
- **Token whitelist clearing** ensures old JWTs are invalidated
- **Fresh DB lookup** guarantees latest user data is used

### Flow Connections
- Password change success connects back to authentication flow
- Cache clearing prevents stale authentication data
- Error handling for invalid current passwords

This updated flowchart reflects the security improvements made to ensure password changes take effect immediately across the authentication system.