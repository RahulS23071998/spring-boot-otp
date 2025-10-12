package com.starter.springboot.config;

import com.starter.springboot.constants.EmailConstants;
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
        properties.put(EmailConstants.MAIL_TRANSPORT_PROTOCOL_KEY, EmailConstants.SMTP_PROTOCOL);
        properties.put(EmailConstants.MAIL_SMTP_AUTH_KEY, providerConfiguration.getAuth().toString());
        properties.put(EmailConstants.MAIL_SMTP_STARTTLS_ENABLE_KEY, providerConfiguration.getStarttlsEnable().toString());
        properties.put(EmailConstants.MAIL_DEBUG_KEY, providerConfiguration.getDebug().toString());

        return javaMailSender;
    }

}
