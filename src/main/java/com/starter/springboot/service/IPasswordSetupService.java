package com.starter.springboot.service;

import com.starter.springboot.dto.SetPasswordDTO;
import com.starter.springboot.dto.SetPasswordResponseDTO;
import com.starter.springboot.entity.User;
import org.springframework.http.ResponseEntity;

public interface IPasswordSetupService {

    ResponseEntity<SetPasswordResponseDTO> handlePasswordSetWithAuthentication(SetPasswordDTO request);

    ResponseEntity<SetPasswordResponseDTO> handlePasswordSetWithTemporaryToken(SetPasswordDTO request);

    ResponseEntity<SetPasswordResponseDTO> updateUserPasswordWithValidation(User user, String newPassword);

    void sendPasswordSetNotification(User user);

    void sendPasswordActivationNotification(User user, String temporaryToken);
}
