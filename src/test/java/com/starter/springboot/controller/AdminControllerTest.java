package com.starter.springboot.controller;

import com.starter.springboot.constants.AdminConstants;
import com.starter.springboot.dto.BulkOtpSendRequest;
import com.starter.springboot.dto.BulkUserImportResponse;
import com.starter.springboot.dto.BulkUserImportResult;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private IUserService userService;

    @Mock
    private IRefreshTokenService refreshTokenService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private OtpAuditEntryRepository auditEntryRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private IPaginationService paginationService;

    @Mock
    private IBulkUserImportService bulkUserImportService;

    @Mock
    private IOtpService otpService;

    @Mock
    private Executor bulkTaskExecutor;

    @InjectMocks
    private AdminController adminController;

    private OtpAuditEntry auditEntry;
    private User user;

    @BeforeEach
    void setUp() {
        auditEntry = new OtpAuditEntry();
        auditEntry.setId(1L);
        auditEntry.setUsername("testuser");

        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setStatus(UserStatus.ACTIVE);

        // Make the executor run synchronously for tests
        lenient().doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(bulkTaskExecutor).execute(any(Runnable.class));
    }

    @Test
    void lockUser_ShouldLockUserAndReturnOk() {
        // Arrange
        User lockedUser = new User();
        lockedUser.setId(1L);
        lockedUser.setStatus(UserStatus.INACTIVE);

        when(userService.updateStatus(eq(1L), eq(UserStatus.INACTIVE), isNull())).thenReturn(lockedUser);

        // Act
        ResponseEntity<User> response = adminController.lockUser(1L);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(UserStatus.INACTIVE);
        verify(userService).updateStatus(eq(1L), eq(UserStatus.INACTIVE), isNull());
    }

    @Test
    void unlockUser_ShouldUnlockUserAndReturnOk() {
        // Arrange
        User unlockedUser = new User();
        unlockedUser.setId(1L);
        unlockedUser.setStatus(UserStatus.ACTIVE);

        when(userService.updateStatus(eq(1L), eq(UserStatus.ACTIVE), isNull())).thenReturn(unlockedUser);

        // Act
        ResponseEntity<User> response = adminController.unlockUser(1L);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(userService).updateStatus(eq(1L), eq(UserStatus.ACTIVE), isNull());
    }

    @Test
    void revokeAllTokens_ShouldRevokeAndReturnOk() {
        // Act
        ResponseEntity<?> response = adminController.revokeAllTokens(1L);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry(AdminConstants.MESSAGE_KEY, AdminConstants.REVOKE_TOKENS_SUCCESS_MESSAGE + 1);
        verify(refreshTokenService).revokeAllUserRefreshTokens(1L);
    }

    @Test
    void inspectToken_ShouldReturnValidInfo_WhenTokenIsValid() {
        // Arrange
        String token = "valid-token";
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(1L);
        refreshToken.setExpiresAt(Instant.now().plusSeconds(3600));
        refreshToken.setCreatedAt(Instant.now());

        when(refreshTokenRepository.findByTokenAndRevokedAtIsNullAndExpiresAtAfter(eq(token), any(Instant.class)))
                .thenReturn(Optional.of(refreshToken));

        // Act
        ResponseEntity<?> response = adminController.inspectToken(Map.of(AdminConstants.TOKEN_PARAM, token));

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry(AdminConstants.VALID_KEY, true);
        assertThat(body).containsEntry(AdminConstants.USER_ID_KEY, 1L);
    }

    @Test
    void inspectToken_ShouldReturnInvalid_WhenTokenIsNotFound() {
        // Arrange
        String token = "invalid-token";
        when(refreshTokenRepository.findByTokenAndRevokedAtIsNullAndExpiresAtAfter(eq(token), any(Instant.class)))
                .thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = adminController.inspectToken(Map.of(AdminConstants.TOKEN_PARAM, token));

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry(AdminConstants.VALID_KEY, false);
        assertThat(body).containsKey(AdminConstants.MESSAGE_KEY);
    }

    @Test
    void getCurrentOtp_ShouldReturnOtpHash_WhenFound() {
        // Arrange
        String username = "testuser";
        String otpHash = "hashed-otp";
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(AdminConstants.OTP_REDIS_KEY_PREFIX + username)).thenReturn(otpHash);

        // Act
        ResponseEntity<?> response = adminController.getCurrentOtp(username);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry(AdminConstants.USERNAME_KEY, username);
        assertThat(body).containsEntry(AdminConstants.OTP_HASH_KEY, otpHash);
    }

    @Test
    void getCurrentOtp_ShouldReturnNotFound_WhenNotFound() {
        // Arrange
        String username = "testuser";
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("OTP:" + username)).thenReturn(null);

        // Act
        ResponseEntity<?> response = adminController.getCurrentOtp(username);

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(404);
    }

    @Test
    void getAuditLogs_ShouldReturnPaginatedResponse() {
        // Arrange
        List<OtpAuditEntry> entries = Collections.singletonList(auditEntry);
        Page<OtpAuditEntry> page = new PageImpl<>(entries);
        
        when(paginationService.createPageableWithSort(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(Pageable.unpaged());
        when(auditEntryRepository.findAll(any(Pageable.class))).thenReturn(page);
        
        when(paginationService.getCurrentPage(any())).thenReturn(0);
        when(paginationService.getPageSize(any())).thenReturn(20);
        when(paginationService.getTotalElements(any())).thenReturn(1L);
        when(paginationService.getTotalPages(any())).thenReturn(1);
        when(paginationService.isFirst(any())).thenReturn(true);
        when(paginationService.isLast(any())).thenReturn(true);
        when(paginationService.hasNext(any())).thenReturn(false);
        when(paginationService.hasPrevious(any())).thenReturn(false);

        // Act
        ResponseEntity<PaginatedResponse<OtpAuditEntry>> response = adminController.getAuditLogs(0, 20, "id", "asc");

        // Assert
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getTotalElements()).isEqualTo(1L);
        
        verify(paginationService).createPageableWithSort(0, 20, "id", "asc");
        verify(auditEntryRepository).findAll(any(Pageable.class));
    }

    @Test
    void bulkImportUsers_ShouldImportUsersAndReturnResponse() {
        MultipartFile file = new MockMultipartFile(
            "file",
            "users.csv",
            "text/csv",
            "username,password,email,firstName,lastName,otpRequired".getBytes(StandardCharsets.UTF_8)
        );

        BulkUserImportResult result1 = new BulkUserImportResult(2, "user1", true, 
            AdminConstants.USER_CREATED_SUCCESS_MESSAGE, 1L);
        BulkUserImportResponse importResponse = new BulkUserImportResponse(1, 1, 0, List.of(result1));

        when(bulkUserImportService.importUsersFromFile(file)).thenReturn(importResponse);

        ResponseEntity<BulkUserImportResponse> response = adminController.bulkImportUsers(file);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTotalRows()).isEqualTo(1);
        assertThat(response.getBody().getSuccessCount()).isEqualTo(1);
        assertThat(response.getBody().getFailureCount()).isEqualTo(0);
        assertThat(response.getBody().getResults()).hasSize(1);
        verify(bulkUserImportService).importUsersFromFile(file);
    }

    @Test
    void bulkImportUsers_ShouldReturnBadRequest_WhenFileIsEmpty() {
        MultipartFile emptyFile = new MockMultipartFile(
            "file",
            "users.csv",
            "text/csv",
            new byte[0]
        );

        ResponseEntity<BulkUserImportResponse> response = adminController.bulkImportUsers(emptyFile);

        assertThat(response.getStatusCodeValue()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTotalRows()).isEqualTo(0);
        assertThat(response.getBody().getFailureCount()).isEqualTo(1);
    }

    @Test
    void bulkImportUsers_ShouldHandleMultipleRows_MixedSuccessAndFailure() {
        MultipartFile file = new MockMultipartFile(
            "file",
            "users.csv",
            "text/csv",
            "username,password,email,firstName,lastName,otpRequired".getBytes(StandardCharsets.UTF_8)
        );

        BulkUserImportResult successResult = new BulkUserImportResult(2, "user1", true, 
            AdminConstants.USER_CREATED_SUCCESS_MESSAGE, 1L);
        BulkUserImportResult failureResult = new BulkUserImportResult(3, "user2", false, 
            AdminConstants.USER_ALREADY_EXISTS_ADMIN_MESSAGE);
        BulkUserImportResponse importResponse = new BulkUserImportResponse(2, 1, 1, 
            List.of(successResult, failureResult));

        when(bulkUserImportService.importUsersFromFile(file)).thenReturn(importResponse);

        ResponseEntity<BulkUserImportResponse> response = adminController.bulkImportUsers(file);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTotalRows()).isEqualTo(2);
        assertThat(response.getBody().getSuccessCount()).isEqualTo(1);
        assertThat(response.getBody().getFailureCount()).isEqualTo(1);
        assertThat(response.getBody().getResults()).hasSize(2);
        assertThat(response.getBody().getResults().get(0).isSuccess()).isTrue();
        assertThat(response.getBody().getResults().get(1).isSuccess()).isFalse();
    }

    @Test
    void bulkSendOtp_ShouldSendOtpToMultipleUsers() {
        List<String> usernames = List.of("user1", "user2");
        BulkOtpSendRequest request = new BulkOtpSendRequest(usernames);

        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("user1");
        user1.setEmail("user1@example.com");
        user1.setIsOtpRequired(true);

        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");
        user2.setIsOtpRequired(true);

        when(userService.findUserByUsername("user1")).thenReturn(user1);
        when(userService.findUserByUsername("user2")).thenReturn(user2);

        ResponseEntity<?> response = adminController.bulkSendOtp(request);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry(AdminConstants.TOTAL_USERS_KEY, 2);
        assertThat(body).containsEntry(AdminConstants.OTP_SENT_KEY, 2);
        assertThat(body).containsEntry(AdminConstants.OTP_FAILED_KEY, 0);
        verify(otpService, times(2)).generateOtp(anyString(), anyString());
    }

    @Test
    void bulkSendOtp_ShouldReturnBadRequest_WhenUsernamesEmpty() {
        BulkOtpSendRequest request = new BulkOtpSendRequest(Collections.emptyList());

        ResponseEntity<?> response = adminController.bulkSendOtp(request);

        assertThat(response.getStatusCodeValue()).isEqualTo(400);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry(AdminConstants.SUCCESS_KEY, false);
        assertThat(body).containsEntry(AdminConstants.MESSAGE_KEY, AdminConstants.USERNAMES_EMPTY_MESSAGE);
    }

    @Test
    void bulkSendOtp_ShouldReturnBadRequest_WhenUsernamesNull() {
        BulkOtpSendRequest request = new BulkOtpSendRequest(null);

        ResponseEntity<?> response = adminController.bulkSendOtp(request);

        assertThat(response.getStatusCodeValue()).isEqualTo(400);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry(AdminConstants.SUCCESS_KEY, false);
        assertThat(body).containsEntry(AdminConstants.MESSAGE_KEY, AdminConstants.USERNAMES_EMPTY_MESSAGE);
    }

    @Test
    void bulkSendOtp_ShouldHandleFailedOtpSend_WhenUserNotFound() {
        List<String> usernames = List.of("user1", "user2");
        BulkOtpSendRequest request = new BulkOtpSendRequest(usernames);

        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("user1");
        user1.setEmail("user1@example.com");
        user1.setIsOtpRequired(true);

        when(userService.findUserByUsername("user1")).thenReturn(user1);
        when(userService.findUserByUsername("user2")).thenReturn(null);

        ResponseEntity<?> response = adminController.bulkSendOtp(request);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry(AdminConstants.TOTAL_USERS_KEY, 2);
        assertThat(body).containsEntry(AdminConstants.OTP_SENT_KEY, 1);
        assertThat(body).containsEntry(AdminConstants.OTP_FAILED_KEY, 1);
        verify(otpService, times(1)).generateOtp(anyString(), anyString());
    }

    @Test
    void bulkSendOtp_ShouldSkipUsers_WhenOtpNotRequired() {
        List<String> usernames = List.of("user1", "user2");
        BulkOtpSendRequest request = new BulkOtpSendRequest(usernames);

        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("user1");
        user1.setEmail("user1@example.com");
        user1.setIsOtpRequired(true);

        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");
        user2.setIsOtpRequired(false);

        when(userService.findUserByUsername("user1")).thenReturn(user1);
        when(userService.findUserByUsername("user2")).thenReturn(user2);

        ResponseEntity<?> response = adminController.bulkSendOtp(request);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry(AdminConstants.TOTAL_USERS_KEY, 2);
        assertThat(body).containsEntry(AdminConstants.OTP_SENT_KEY, 1);
        assertThat(body).containsEntry(AdminConstants.OTP_FAILED_KEY, 1);
        verify(otpService, times(1)).generateOtp(anyString(), anyString());
    }

    @Test
    void bulkSendOtp_ShouldHandleException_WhenOtpGenerationFails() {
        List<String> usernames = List.of("user1");
        BulkOtpSendRequest request = new BulkOtpSendRequest(usernames);

        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("user1");
        user1.setEmail("user1@example.com");
        user1.setIsOtpRequired(true);

        when(userService.findUserByUsername("user1")).thenReturn(user1);
        when(otpService.generateOtp(anyString(), anyString())).thenThrow(new RuntimeException("OTP generation failed"));

        ResponseEntity<?> response = adminController.bulkSendOtp(request);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry(AdminConstants.TOTAL_USERS_KEY, 1);
        assertThat(body).containsEntry(AdminConstants.OTP_SENT_KEY, 0);
        assertThat(body).containsEntry(AdminConstants.OTP_FAILED_KEY, 1);
    }

    @Test
    void getAllUsers_ShouldReturnPaginatedUsers() {
        Page<User> page = new PageImpl<>(List.of(user));
        when(paginationService.createPageableWithSort(anyInt(), anyInt(), anyString(), anyString())).thenReturn(Pageable.unpaged());
        when(userService.getAllUsers(any(Pageable.class))).thenReturn(page);
        
        ResponseEntity<PaginatedResponse<User>> response = adminController.getAllUsers(0, 20, "id", "asc");
        
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody().getContent()).hasSize(1);
    }

    @Test
    void getUser_ShouldReturnUser() {
        when(userService.getUserById(1L)).thenReturn(user);
        
        ResponseEntity<User> response = adminController.getUser(1L);
        
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody().getUsername()).isEqualTo("testuser");
    }

    @Test
    void updateUser_ShouldReturnUpdatedUser() {
        User updates = new User();
        updates.setFirstName("Updated");
        when(userService.updateUser(eq(1L), any(User.class))).thenReturn(user);
        
        ResponseEntity<User> response = adminController.updateUser(1L, updates);
        
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        verify(userService).updateUser(eq(1L), any(User.class));
    }

    @Test
    void deleteUser_ShouldReturnOk() {
        ResponseEntity<?> response = adminController.deleteUser(1L);
        
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        verify(userService).deleteUser(1L);
    }

    @Test
    void resetUserPassword_ShouldReturnOk() {
        ResponseEntity<?> response = adminController.resetUserPassword(1L, Map.of("newPassword", "newPass"));
        
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        verify(userService).resetUserPassword(1L, "newPass");
    }

    @Test
    void getUsersByStatus_ShouldReturnPaginatedUsers() {
        Page<User> page = new PageImpl<>(List.of(user));
        when(paginationService.createPageable(anyInt(), anyInt())).thenReturn(Pageable.unpaged());
        when(userService.getUsersByStatus(eq(UserStatus.ACTIVE), any(Pageable.class))).thenReturn(page);
        
        ResponseEntity<PaginatedResponse<User>> response = adminController.getUsersByStatus(UserStatus.ACTIVE, 0, 20);
        
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody().getContent()).hasSize(1);
    }

    @Test
    void exportUsersToCSV_ShouldReturnCSVData() {
        byte[] csvData = "id,username\n1,testuser".getBytes();
        when(userService.exportUsersToCSV()).thenReturn(csvData);
        
        ResponseEntity<?> response = adminController.exportUsersToCSV();
        
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("text/csv");
        assertThat(response.getBody()).isEqualTo(csvData);
    }

    @Test
    void exportUsersToExcel_ShouldReturnExcelData() {
        byte[] excelData = new byte[]{1, 2, 3};
        when(userService.exportUsersToExcel()).thenReturn(excelData);
        
        ResponseEntity<?> response = adminController.exportUsersToExcel();
        
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(response.getBody()).isEqualTo(excelData);
    }

    @Test
    void getUserStatistics_ShouldReturnStats() {
        when(userService.getTotalUsers()).thenReturn(10L);
        when(userService.getUserCountByStatus(UserStatus.ACTIVE)).thenReturn(8L);
        when(userService.getUserCountByStatus(UserStatus.INACTIVE)).thenReturn(2L);
        when(userService.getOtpRequiredUserCount()).thenReturn(5L);
        when(userService.getEmailVerifiedUserCount()).thenReturn(7L);
        
        ResponseEntity<?> response = adminController.getUserStatistics();
        
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("totalUsers")).isEqualTo(10L);
        assertThat(body.get("activeUsers")).isEqualTo(8L);
    }
}
