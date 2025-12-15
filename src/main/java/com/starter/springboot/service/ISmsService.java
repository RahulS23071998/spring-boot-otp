package com.starter.springboot.service;

import java.util.concurrent.CompletableFuture;

public interface ISmsService {
    Boolean sendSms(String phoneNumber, String message);
    CompletableFuture<Boolean> sendSmsAsync(String phoneNumber, String message);
}
