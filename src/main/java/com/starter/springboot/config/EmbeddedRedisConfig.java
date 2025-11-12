package com.starter.springboot.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import redis.embedded.RedisServer;

import java.io.IOException;

/**
 * Configuration to start embedded Redis server in development and production profiles.
 * This is excluded from test profiles where TestRedisConfiguration is used instead.
 */
@Configuration
@Profile("!test")  // Exclude from test profile
public class EmbeddedRedisConfig {

    @Value("${embedded.redis.enabled:true}")
    private boolean embeddedRedisEnabled;

    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() throws IOException {
        if (!embeddedRedisEnabled) {
            return;
        }
        redisServer = new RedisServer(6379);
        redisServer.start();
    }

    @PreDestroy
    public void stopRedis() {
        if (redisServer != null) {
            redisServer.stop();
        }
    }
}
