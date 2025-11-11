package com.starter.springboot.config;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

@Component
public class AppInfoContributor implements InfoContributor {

    private final AppInfoProperties appInfoProperties;

    public AppInfoContributor(AppInfoProperties appInfoProperties) {
        this.appInfoProperties = appInfoProperties;
    }

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("app", appInfoProperties.getApp());
        builder.withDetail("system", appInfoProperties.getSystem());
        builder.withDetail("contact", appInfoProperties.getContact());
        builder.withDetail("license", appInfoProperties.getLicense());
    }
}