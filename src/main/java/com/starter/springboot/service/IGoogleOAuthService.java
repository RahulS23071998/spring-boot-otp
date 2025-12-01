package com.starter.springboot.service;

import java.util.Map;

public interface IGoogleOAuthService {

    Map<String, Object> verifyAndExtractUserInfo(String idTokenString);
}
