package com.starter.springboot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.mail")
public class ProviderConfiguration {

    private String host;
    private Integer port;
    private String username;
    private String password;
    private Boolean debug = Boolean.FALSE;
    private Boolean auth = Boolean.TRUE;
    private Boolean starttlsEnable = Boolean.TRUE;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getDebug() {
        return debug;
    }

    public void setDebug(Boolean debug) {
        this.debug = debug;
    }

    public Boolean getAuth() {
        return auth;
    }

    public void setAuth(Boolean auth) {
        this.auth = auth;
    }

    public Boolean getStarttlsEnable() {
        return starttlsEnable;
    }

    public void setStarttlsEnable(Boolean starttlsEnable) {
        this.starttlsEnable = starttlsEnable;
    }

}
