package com.starter.springboot.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PasswordValidationServiceTest {

    @InjectMocks
    private PasswordValidationService passwordValidationService;

    @Test
    @DisplayName("Should validate valid password")
    void shouldValidateValidPassword() {
        // Given
        String validPassword = "Password123!";

        // When
        PasswordValidationService.PasswordValidationResult result = passwordValidationService.validate(validPassword);

        // Then
        assertTrue(result.valid());
        assertNull(result.message());
    }

    @Test
    @DisplayName("Should fail when password is null")
    void shouldFailWhenPasswordIsNull() {
        // When
        PasswordValidationService.PasswordValidationResult result = passwordValidationService.validate(null);

        // Then
        assertFalse(result.valid());
        assertEquals("Password cannot be empty", result.message());
    }

    @Test
    @DisplayName("Should fail when password is empty")
    void shouldFailWhenPasswordIsEmpty() {
        // When
        PasswordValidationService.PasswordValidationResult result = passwordValidationService.validate("");

        // Then
        assertFalse(result.valid());
        assertEquals("Password cannot be empty", result.message());
    }

    @Test
    @DisplayName("Should fail when password is too short")
    void shouldFailWhenPasswordIsTooShort() {
        // When
        PasswordValidationService.PasswordValidationResult result = passwordValidationService.validate("Pass1!");

        // Then
        assertFalse(result.valid());
        assertTrue(result.message().contains("at least 8 characters"));
    }

    @Test
    @DisplayName("Should fail when password is too long")
    void shouldFailWhenPasswordIsTooLong() {
        // Given
        String longPassword = "Password123!Password123!Password123!Password123!"; // 48 chars

        // When
        PasswordValidationService.PasswordValidationResult result = passwordValidationService.validate(longPassword);

        // Then
        assertFalse(result.valid());
        assertTrue(result.message().contains("not exceed 32 characters"));
    }

    @Test
    @DisplayName("Should fail when password has no lowercase")
    void shouldFailWhenPasswordHasNoLowercase() {
        // When
        PasswordValidationService.PasswordValidationResult result = passwordValidationService.validate("PASSWORD123!");

        // Then
        assertFalse(result.valid());
        assertEquals("Password must contain at least one lowercase letter", result.message());
    }

    @Test
    @DisplayName("Should fail when password has no uppercase")
    void shouldFailWhenPasswordHasNoUppercase() {
        // When
        PasswordValidationService.PasswordValidationResult result = passwordValidationService.validate("password123!");

        // Then
        assertFalse(result.valid());
        assertEquals("Password must contain at least one uppercase letter", result.message());
    }

    @Test
    @DisplayName("Should fail when password has no digit")
    void shouldFailWhenPasswordHasNoDigit() {
        // When
        PasswordValidationService.PasswordValidationResult result = passwordValidationService.validate("Password!");

        // Then
        assertFalse(result.valid());
        assertEquals("Password must contain at least one digit", result.message());
    }

    @Test
    @DisplayName("Should fail when password has no special character")
    void shouldFailWhenPasswordHasNoSpecialCharacter() {
        // When
        PasswordValidationService.PasswordValidationResult result = passwordValidationService.validate("Password123");

        // Then
        assertFalse(result.valid());
        assertEquals("Password must contain at least one special character (@$!%*?&)", result.message());
    }
}
