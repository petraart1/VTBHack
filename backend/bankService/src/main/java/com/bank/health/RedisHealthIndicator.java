package com.bank.health;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Health indicator для мониторинга состояния Redis
 */
@Component
@RequiredArgsConstructor
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisConnectionFactory redisConnectionFactory;

    @Override
    public Health health() {
        try {
            // Проверяем подключение к Redis
            String ping = redisConnectionFactory.getConnection().ping();

            if ("PONG".equals(ping)) {
                return Health.up()
                        .withDetail("status", "UP")
                        .withDetail("ping", ping)
                        .build();
            } else {
                return Health.down()
                        .withDetail("status", "DOWN")
                        .withDetail("ping", ping)
                        .withDetail("error", "Unexpected ping response")
                        .build();
            }

        } catch (Exception e) {
            return Health.down()
                    .withDetail("status", "DOWN")
                    .withDetail("error", e.getMessage())
                    .withDetail("type", e.getClass().getSimpleName())
                    .build();
        }
    }
}
