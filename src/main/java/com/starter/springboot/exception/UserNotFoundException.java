package com.starter.springboot.exception;

public class UserNotFoundException extends RuntimeException {
    
    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public static UserNotFoundException ofId(Long userId) {
        return new UserNotFoundException("User not found with id: " + userId);
    }

    public static UserNotFoundException ofUsername(String username) {
        return new UserNotFoundException("User not found with username: " + username);
    }
}
