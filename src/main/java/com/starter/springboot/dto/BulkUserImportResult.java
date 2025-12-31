package com.starter.springboot.dto;

public class BulkUserImportResult {
    
    private int rowNumber;
    private String username;
    private boolean success;
    private String message;
    private Long userId;

    public BulkUserImportResult(int rowNumber, String username, boolean success, String message, Long userId) {
        this.rowNumber = rowNumber;
        this.username = username;
        this.success = success;
        this.message = message;
        this.userId = userId;
    }

    public BulkUserImportResult(int rowNumber, String username, boolean success, String message) {
        this(rowNumber, username, success, message, null);
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
