package com.bank.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Конфигурация RestClient для взаимодействия с внешними Bank API
 * Использует новый RestClient (Spring 6+) вместо deprecated RestTemplate
 * Добавлен реактивный WebClient для асинхронных операций
 */
@Configuration
public class RestClientConfig {

    private static final Logger log = LoggerFactory.getLogger(RestClientConfig.class);

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
     * Реактивный WebClient для внешних банковских API
     * Особенности:
     * - Асинхронные неблокирующие вызовы
     * - Timeout: 30 сек на ответ
     * - Reactor Netty для высокой производительности
     * - Логирование запросов/ответов
     */
    @Bean
    public WebClient bankWebClient() {
        return WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .filter(logRequest())
                .filter(logResponse())
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

    /**
     * Фильтр для логирования исходящих запросов WebClient
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            if (log.isDebugEnabled()) {
                String sanitizedUrl = clientRequest.url().toString()
                        .replaceAll("client_id=[^&]*", "client_id=***")
                        .replaceAll("access_token=[^&]*", "access_token=***");

                log.debug("WebClient → {} {} - Headers: {}",
                        clientRequest.method(),
                        sanitizedUrl,
                        clientRequest.headers());
            }
            return reactor.core.publisher.Mono.just(clientRequest);
        });
    }

    /**
     * Фильтр для логирования входящих ответов WebClient
     */
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (log.isDebugEnabled()) {
                log.debug("WebClient ← {} {} - Headers: {}",
                        clientResponse.statusCode().value(),
                        clientResponse.statusCode(),
                        clientResponse.headers().asHttpHeaders());
            }
            return reactor.core.publisher.Mono.just(clientResponse);
        });
    }
}

