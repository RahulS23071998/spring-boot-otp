package com.starter.springboot.controller;

import com.starter.springboot.constants.AdminConstants;
import com.starter.springboot.dto.BulkOtpSendRequest;
import com.starter.springboot.dto.BulkUserImportResponse;
import com.starter.springboot.dto.PaginatedResponse;
import com.starter.springboot.entity.OtpAuditEntry;
import com.starter.springboot.entity.RefreshToken;
import com.starter.springboot.entity.User;
import com.starter.springboot.entity.UserStatus;
import com.starter.springboot.repository.OtpAuditEntryRepository;
import com.starter.springboot.repository.RefreshTokenRepository;
import com.starter.springboot.service.IBulkUserImportService;
import com.starter.springboot.service.IOtpService;
import com.starter.springboot.service.IPaginationService;
import com.starter.springboot.service.IRefreshTokenService;
import com.starter.springboot.service.IUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(AdminConstants.ADMIN_BASE_PATH)
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final IUserService userService;
    private final IRefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpAuditEntryRepository auditEntryRepository;
    private final StringRedisTemplate redisTemplate;
    private final IPaginationService paginationService;
    private final IBulkUserImportService bulkUserImportService;
    private final IOtpService otpService;

    public AdminController(IUserService userService,
                           IRefreshTokenService refreshTokenService,
                           RefreshTokenRepository refreshTokenRepository,
                           OtpAuditEntryRepository auditEntryRepository,
                           StringRedisTemplate redisTemplate,
                           IPaginationService paginationService,
                           IBulkUserImportService bulkUserImportService,
                           IOtpService otpService) {
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditEntryRepository = auditEntryRepository;
        this.redisTemplate = redisTemplate;
        this.paginationService = paginationService;
        this.bulkUserImportService = bulkUserImportService;
        this.otpService = otpService;
    }

    // 1. Lock/Unlock User
    @PostMapping(AdminConstants.USERS_LOCK_ENDPOINT)
    public ResponseEntity<User> lockUser(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.updateStatus(userId, UserStatus.INACTIVE, null));
    }

    @PostMapping(AdminConstants.USERS_UNLOCK_ENDPOINT)
    public ResponseEntity<User> unlockUser(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.updateStatus(userId, UserStatus.ACTIVE, null));
    }

    // 2. Revoke All Tokens for User
    @PostMapping(AdminConstants.USERS_REVOKE_TOKENS_ENDPOINT)
    public ResponseEntity<?> revokeAllTokens(@PathVariable Long userId) {
        refreshTokenService.revokeAllUserRefreshTokens(userId);
        return ResponseEntity.ok().body(Map.of(AdminConstants.MESSAGE_KEY, 
            AdminConstants.REVOKE_TOKENS_SUCCESS_MESSAGE + userId));
    }

    // 3. Inspect Refresh Token
    @PostMapping(AdminConstants.TOKENS_INSPECT_ENDPOINT)
    public ResponseEntity<?> inspectToken(@RequestBody Map<String, String> request) {
        String token = request.get(AdminConstants.TOKEN_PARAM);
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByTokenAndRevokedAtIsNullAndExpiresAtAfter(token, java.time.Instant.now());
        
        if (refreshToken.isPresent()) {
            RefreshToken rt = refreshToken.get();
            return ResponseEntity.ok(Map.of(
                AdminConstants.VALID_KEY, true,
                AdminConstants.USER_ID_KEY, rt.getUserId(),
                AdminConstants.EXPIRES_AT_KEY, rt.getExpiresAt(),
                AdminConstants.CREATED_AT_KEY, rt.getCreatedAt()
            ));
        } else {
            return ResponseEntity.ok(Map.of(AdminConstants.VALID_KEY, false, 
                AdminConstants.MESSAGE_KEY, AdminConstants.TOKEN_INVALID_MESSAGE));
        }
    }

    // 4. Get Current OTP (Dev/Test only)
    @GetMapping(AdminConstants.CURRENT_OTP_ENDPOINT)
    public ResponseEntity<?> getCurrentOtp(@PathVariable String username) {
        String redisKey = AdminConstants.OTP_REDIS_KEY_PREFIX + username;
        String otpHash = redisTemplate.opsForValue().get(redisKey);
        
        if (otpHash != null) {
            return ResponseEntity.ok(Map.of(AdminConstants.USERNAME_KEY, username, 
                AdminConstants.OTP_HASH_KEY, otpHash));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // 5. View Audit Logs
    @GetMapping(AdminConstants.AUDIT_LOGS_ENDPOINT)
    public ResponseEntity<PaginatedResponse<OtpAuditEntry>> getAuditLogs(
            @RequestParam(value = AdminConstants.PAGE_PARAM, defaultValue = "0") int page,
            @RequestParam(value = AdminConstants.SIZE_PARAM, defaultValue = "20") int size,
            @RequestParam(value = AdminConstants.SORT_BY_PARAM, defaultValue = AdminConstants.DEFAULT_SORT_BY) String sortBy,
            @RequestParam(value = AdminConstants.SORT_DIRECTION_PARAM, defaultValue = AdminConstants.DEFAULT_SORT_DIRECTION) String sortDirection) {
        
        Pageable pageable = paginationService.createPageableWithSort(page, size, sortBy, sortDirection);
        Page<OtpAuditEntry> auditLogs = auditEntryRepository.findAll(pageable);
        
        PaginatedResponse<OtpAuditEntry> response = new PaginatedResponse<>(
            auditLogs.getContent(),
            paginationService.getCurrentPage(auditLogs),
            paginationService.getPageSize(auditLogs),
            paginationService.getTotalElements(auditLogs),
            paginationService.getTotalPages(auditLogs),
            paginationService.isFirst(auditLogs),
            paginationService.isLast(auditLogs),
            paginationService.hasNext(auditLogs),
            paginationService.hasPrevious(auditLogs)
        );
        
        return ResponseEntity.ok(response);
    }

    // 6. Bulk User Import from CSV/Excel
    @PostMapping(AdminConstants.BULK_USER_IMPORT_ENDPOINT)
    public ResponseEntity<BulkUserImportResponse> bulkImportUsers(
            @RequestParam(AdminConstants.FILE_PARAM) MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                new BulkUserImportResponse(0, 0, 1, List.of())
            );
        }
        
        BulkUserImportResponse response = bulkUserImportService.importUsersFromFile(file);
        return ResponseEntity.ok(response);
    }

    // 7. Bulk Send OTP to Users
    @PostMapping(AdminConstants.BULK_OTP_SEND_ENDPOINT)
    public ResponseEntity<?> bulkSendOtp(@RequestBody BulkOtpSendRequest request) {
        List<String> usernames = request.getUsernames();
        
        if (usernames == null || usernames.isEmpty()) {
            return ResponseEntity.badRequest().body(
                Map.of(AdminConstants.SUCCESS_KEY, false, AdminConstants.MESSAGE_KEY, 
                    AdminConstants.USERNAMES_EMPTY_MESSAGE)
            );
        }
        
        int successCount = 0;
        int failureCount = 0;
        
        for (String username : usernames) {
            try {
                User user = userService.findUserByUsername(username);
                if (user != null && user.getIsOtpRequired()) {
                    String email = user.getEmail();
                    otpService.generateOtp(username, email);
                    successCount++;
                } else {
                    failureCount++;
                }
            } catch (Exception e) {
                failureCount++;
            }
        }
        
        return ResponseEntity.ok(Map.of(
            AdminConstants.TOTAL_USERS_KEY, usernames.size(),
            AdminConstants.OTP_SENT_KEY, successCount,
            AdminConstants.OTP_FAILED_KEY, failureCount
        ));
    }
}
