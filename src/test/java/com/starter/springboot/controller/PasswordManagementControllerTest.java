package com.starter.springboot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.starter.springboot.dto.SetPasswordDTO;
import com.starter.springboot.dto.SetPasswordResponseDTO;
import com.starter.springboot.service.IPasswordSetupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordManagementController Tests")
class PasswordManagementControllerTest {

    @Mock
    private IPasswordSetupService passwordSetupService;

    @InjectMocks
    private PasswordManagementController passwordManagementController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private SetPasswordDTO validSetPasswordDTO;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(passwordManagementController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

        validSetPasswordDTO = new SetPasswordDTO();
        validSetPasswordDTO.setUsername("diona.smith");
        validSetPasswordDTO.setPassword("NewSecurePass123!");
        validSetPasswordDTO.setConfirmPassword("NewSecurePass123!");
        validSetPasswordDTO.setTemporaryToken("valid-temp-token");
    }

    @Test
    @DisplayName("Should successfully set password with temporary token")
    void testSetPasswordWithTemporaryToken() throws Exception {
        // Arrange
        when(passwordSetupService.handlePasswordSetWithTemporaryToken(any(SetPasswordDTO.class)))
                .thenReturn(ResponseEntity.ok(SetPasswordResponseDTO.success("Password set successfully. You can now login with your email and password.")));

        // Act & Assert
        mockMvc.perform(post("/auth/set-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validSetPasswordDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.message", is("Password set successfully. You can now login with your email and password.")));

        verify(passwordSetupService).handlePasswordSetWithTemporaryToken(any(SetPasswordDTO.class));
    }

    @Test
    @DisplayName("Should successfully set password with authentication")
    void testSetPasswordWithAuthentication() throws Exception {
        // Arrange
        validSetPasswordDTO.setTemporaryToken(null);
        
        when(passwordSetupService.handlePasswordSetWithAuthentication(any(SetPasswordDTO.class)))
                .thenReturn(ResponseEntity.ok(SetPasswordResponseDTO.success("Password set successfully. You can now login with your email and password.")));

        // Act & Assert
        mockMvc.perform(post("/auth/set-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validSetPasswordDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.message", is("Password set successfully. You can now login with your email and password.")));

        verify(passwordSetupService).handlePasswordSetWithAuthentication(any(SetPasswordDTO.class));
    }

    @Test
    @DisplayName("Should return BAD_REQUEST when passwords do not match")
    void testSetPasswordMismatch() throws Exception {
        // Arrange
        validSetPasswordDTO.setConfirmPassword("DifferentPass123!");

        // Act & Assert
        mockMvc.perform(post("/auth/set-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validSetPasswordDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("FAILED")))
                .andExpect(jsonPath("$.message", is("Passwords do not match")));
    }
}
