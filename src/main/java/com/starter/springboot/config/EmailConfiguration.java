package com.starter.springboot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class EmailConfiguration {

    private final ProviderConfiguration providerConfiguration;

    public EmailConfiguration(ProviderConfiguration providerConfiguration) {
        this.providerConfiguration = providerConfiguration;
    }

    @Bean
    public JavaMailSender mailSender()
    {
        JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();
        javaMailSender.setHost(providerConfiguration.getHost());
        javaMailSender.setPort(providerConfiguration.getPort());

        javaMailSender.setUsername(providerConfiguration.getUsername());
        javaMailSender.setPassword(providerConfiguration.getPassword());

        Properties properties = javaMailSender.getJavaMailProperties();
        properties.put("mail.transport.protocol", "smtp");
        properties.put("mail.smtp.auth", providerConfiguration.getAuth().toString());
        properties.put("mail.smtp.starttls.enable", providerConfiguration.getStarttlsEnable().toString());
        properties.put("mail.debug", providerConfiguration.getDebug().toString());

        return javaMailSender;
    }

}
