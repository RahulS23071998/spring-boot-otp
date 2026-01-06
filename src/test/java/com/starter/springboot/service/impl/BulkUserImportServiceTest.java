package com.starter.springboot.service.impl;

import com.starter.springboot.constants.AdminConstants;
import com.starter.springboot.constants.SecurityConstants;
import com.starter.springboot.dto.BulkUserImportResponse;
import com.starter.springboot.dto.BulkUserImportResult;
import com.starter.springboot.entity.Role;
import com.starter.springboot.entity.User;
import com.starter.springboot.exception.UserAlreadyExistsException;
import com.starter.springboot.repository.RoleRepository;
import com.starter.springboot.service.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BulkUserImportServiceTest {

    @Mock
    private IUserService userService;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private BulkUserImportService bulkUserImportService;

    private Role testRole;

    @BeforeEach
    void setUp() {
        testRole = new Role();
        testRole.setId(1L);
        testRole.setName(SecurityConstants.USER_AUTHORITY);
    }

    @Test
    @DisplayName("Should successfully import users from CSV")
    void shouldSuccessfullyImportUsersFromCsv() throws IOException {
        // Given
        String csvContent = "username,password,email,firstName,lastName,otpRequired\n" +
                "user1,pass1,user1@example.com,John,Doe,true\n" +
                "user2,pass2,user2@example.com,Jane,Smith,false";
        
        MockMultipartFile file = new MockMultipartFile("file", "users.csv", "text/csv", csvContent.getBytes());
        
        when(roleRepository.findByName(SecurityConstants.USER_AUTHORITY)).thenReturn(Optional.of(testRole));
        when(userService.createUser(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(System.currentTimeMillis()); // Mock an ID assignment
            return user;
        });

        // When
        BulkUserImportResponse response = bulkUserImportService.importUsersFromFile(file);

        // Then
        assertNotNull(response);
        assertEquals(2, response.getTotalRows());
        assertEquals(2, response.getSuccessCount());
        assertEquals(0, response.getFailureCount());
        assertEquals(2, response.getResults().size());
        
        verify(userService, times(2)).createUser(any(User.class));
    }

    @Test
    @DisplayName("Should handle UserAlreadyExistsException during CSV import")
    void shouldHandleUserAlreadyExistsExceptionDuringCsvImport() throws IOException {
        // Given
        String csvContent = "username,password,email,firstName,lastName,otpRequired\n" +
                "user1,pass1,user1@example.com,John,Doe,true";
        
        MockMultipartFile file = new MockMultipartFile("file", "users.csv", "text/csv", csvContent.getBytes());
        
        when(roleRepository.findByName(SecurityConstants.USER_AUTHORITY)).thenReturn(Optional.of(testRole));
        when(userService.createUser(any(User.class))).thenThrow(new UserAlreadyExistsException("User exists", 1L));

        // When
        BulkUserImportResponse response = bulkUserImportService.importUsersFromFile(file);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getTotalRows());
        assertEquals(0, response.getSuccessCount());
        assertEquals(1, response.getFailureCount());
        
        BulkUserImportResult result = response.getResults().get(0);
        assertFalse(result.isSuccess());
        assertEquals(1L, result.getUserId());
        assertEquals(AdminConstants.USER_ALREADY_EXISTS_ADMIN_MESSAGE, result.getMessage());
    }

    @Test
    @DisplayName("Should handle validation errors during CSV import")
    void shouldHandleValidationErrorsDuringCsvImport() throws IOException {
        // Given
        String csvContent = "username,password,email,firstName,lastName,otpRequired\n" +
                ",pass1,user1@example.com,John,Doe,true"; // Empty username
        
        MockMultipartFile file = new MockMultipartFile("file", "users.csv", "text/csv", csvContent.getBytes());
        
        when(roleRepository.findByName(SecurityConstants.USER_AUTHORITY)).thenReturn(Optional.of(testRole));

        // When
        BulkUserImportResponse response = bulkUserImportService.importUsersFromFile(file);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getTotalRows());
        assertEquals(0, response.getSuccessCount());
        assertEquals(1, response.getFailureCount());
        
        BulkUserImportResult result = response.getResults().get(0);
        assertFalse(result.isSuccess());
        assertEquals(AdminConstants.USERNAME_REQUIRED_MESSAGE, result.getMessage());
    }

    @Test
    @DisplayName("Should throw error for unsupported file format")
    void shouldThrowErrorForUnsupportedFileFormat() {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "users.txt", "text/plain", "content".getBytes());
        
        when(roleRepository.findByName(SecurityConstants.USER_AUTHORITY)).thenReturn(Optional.of(testRole));

        // When
        BulkUserImportResponse response = bulkUserImportService.importUsersFromFile(file);

        // Then
        assertNotNull(response);
        assertEquals(0, response.getTotalRows());
        assertEquals(1, response.getFailureCount());
        assertTrue(response.getResults().get(0).getMessage().contains(AdminConstants.UNSUPPORTED_FILE_FORMAT_MESSAGE));
    }
}
