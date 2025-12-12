package com.starter.springboot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OAuthRedirectDTO(
    @JsonProperty("status") String status,
    @JsonProperty("username") String username,
    @JsonProperty("message") String message,
    @JsonProperty("temporary_token") String temporaryToken
) {
}
