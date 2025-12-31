package com.starter.springboot.dto;

import java.util.List;

public class BulkOtpSendRequest {
    
    private List<String> usernames;

    public BulkOtpSendRequest() {
    }

    public BulkOtpSendRequest(List<String> usernames) {
        this.usernames = usernames;
    }

    public List<String> getUsernames() {
        return usernames;
    }

    public void setUsernames(List<String> usernames) {
        this.usernames = usernames;
    }
}
