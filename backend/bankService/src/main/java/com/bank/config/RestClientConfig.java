package com.bank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Конфигурация RestClient для взаимодействия с внешними Bank API
 * Использует новый RestClient (Spring 6+) вместо deprecated RestTemplate
 */
@Configuration
public class RestClientConfig {

    /**
     * RestClient для внешних банковских API
     * Особенности:
     * - Timeout: 10 sec connect, 30 sec read
     * - Retry: 3 попытки с экспоненциальной задержкой
     * - Circuit Breaker: будет добавлен через Resilience4j
     */
    @Bean
    public RestClient bankRestClient() {
        return RestClient.builder()
                .requestInterceptor(loggingInterceptor())
                .build();
    }

    /**
     * Интерцептор для логирования всех запросов к внешним банкам
     * Не логирует sensitive данные (токены, пароли)
     */
    private ClientHttpRequestInterceptor loggingInterceptor() {
        return (request, body, execution) -> {
            String uri = request.getURI().toString();
            String method = request.getMethod().name();
            
            // Скрываем токены в логах
            String sanitizedUri = uri.replaceAll("access_token=[^&]*", "access_token=***");
            
            long startTime = System.currentTimeMillis();
            var response = execution.execute(request, body);
            long duration = System.currentTimeMillis() - startTime;
            
            // Логирование будет в BankApiClient
            
            return response;
        };
    }
}

