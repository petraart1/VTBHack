package com.bank.config;

import com.bank.config.BankingConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

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

    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        try {
            // Проверяем доступность Redis
            LettuceConnectionFactory lettuceFactory = (LettuceConnectionFactory) connectionFactory;
            lettuceFactory.getConnection().ping();
            log.info("Redis is available, using Redis cache manager");

            RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(5)); // Default TTL 5 minutes

            return RedisCacheManager.builder(connectionFactory)
                    .cacheDefaults(defaultConfig)
                    .withCacheConfiguration("accounts", defaultConfig.entryTtl(Duration.ofMinutes(5)))
                    .withCacheConfiguration("bankTokens", defaultConfig.entryTtl(Duration.ofMinutes(10)))
                    .withCacheConfiguration("consents", defaultConfig.entryTtl(Duration.ofMinutes(30)))
                    .build();
        } catch (Exception e) {
            log.warn("Redis is not available, falling back to in-memory cache: {}", e.getMessage());
            return fallbackCacheManager();
        }
    }

    /**
     * Fallback cache manager для случаев, когда Redis недоступен
     */
    private CacheManager fallbackCacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        // Создаем in-memory кеши для тех же имен, что используются в коде
        Cache accountsCache = new ConcurrentMapCache("accounts");
        Cache bankTokensCache = new ConcurrentMapCache("bankTokens");
        Cache consentsCache = new ConcurrentMapCache("consents");

        cacheManager.setCaches(Arrays.asList(accountsCache, bankTokensCache, consentsCache));
        return cacheManager;
    }
}