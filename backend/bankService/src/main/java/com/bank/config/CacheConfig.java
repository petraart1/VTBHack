package com.bank.config;

import com.bank.config.BankingConstants;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;

import java.time.Duration;
import java.util.Arrays;

@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    @Value("${spring.redis.timeout:2000}")
    private int redisTimeout;

    @Value("${spring.redis.database:0}")
    private int redisDatabase;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        log.info("Configuring Redis connection: {}:{} (timeout: {}ms, database: {})",
                redisHost, redisPort, redisTimeout, redisDatabase);

        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);
        redisConfig.setDatabase(redisDatabase);

        if (!redisPassword.isEmpty()) {
            redisConfig.setPassword(RedisPassword.of(redisPassword));
        }

        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(redisTimeout))
                .shutdownTimeout(Duration.ofMillis(100))
                .build();

        return new LettuceConnectionFactory(redisConfig, clientConfig);
    }

    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Пытаемся подключиться к Redis с retry
        int maxRetries = 3;
        int retryDelay = 2000; // 2 секунды

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
        try {
                log.info("Attempting to connect to Redis (attempt {}/{})", attempt, maxRetries);

                // Проверяем подключение
            LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) connectionFactory;
                String pingResult = lettuceFactory.getConnection().ping();

                if ("PONG".equals(pingResult)) {
                    log.info("✅ Redis connection successful, using Redis cache manager");

            RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                            .entryTtl(Duration.ofMinutes(5)) // Default TTL 5 minutes
                            .disableCachingNullValues(); // Не кешировать null значения

            return RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(defaultConfig)
                    .withCacheConfiguration("accounts", defaultConfig.entryTtl(Duration.ofMinutes(5)))
                    .withCacheConfiguration("bankTokens", defaultConfig.entryTtl(Duration.ofMinutes(10)))
                    .withCacheConfiguration("consents", defaultConfig.entryTtl(Duration.ofMinutes(30)))
                    .build();
                } else {
                    throw new RuntimeException("Redis ping returned: " + pingResult);
                }

        } catch (Exception e) {
                log.warn("❌ Redis connection attempt {}/{} failed: {}", attempt, maxRetries, e.getMessage());

                if (attempt == maxRetries) {
                    log.warn("🚨 All Redis connection attempts failed, falling back to in-memory cache");
            return fallbackCacheManager();
        }

                // Ждем перед следующей попыткой
                try {
                    Thread.sleep(retryDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // Fallback в случае прерывания
        return fallbackCacheManager();
    }

    /**
     * Fallback cache manager для случаев, когда Redis недоступен
     * Использует Caffeine Cache с TTL для предотвращения memory leaks
     */
    private CacheManager fallbackCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // Настраиваем TTL такой же как в Redis (5 минут по умолчанию)
        cacheManager.setCaffeine(
            Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5)) // TTL 5 минут
                .maximumSize(1000) // Ограничение размера для предотвращения OOM
        );

        // Устанавливаем имена кешей
        cacheManager.setCacheNames(Arrays.asList("accounts", "bankTokens", "consents"));

        log.info("🚨 Using in-memory Caffeine cache with TTL=5min and maxSize=1000 (fallback mode)");
        return cacheManager;
    }
}