package com.starter.springboot.exception;

public class UserAlreadyExistsException extends RuntimeException {
    private final Long userId;

    public UserAlreadyExistsException(String message, Long userId) {
        super(message);
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}
