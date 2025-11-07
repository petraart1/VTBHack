package com.bank.service;

import com.bank.config.BankProperties;
import com.bank.dto.common.BankCredentials;
import com.bank.dto.response.BankTokenResponseDto;
import com.bank.exception.BankApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис для управления OAuth2 токенами внешних банков
 * Особенности:
 * - Кеширование токенов в памяти (Redis cache)
 * - Автоматическое обновление за 60 сек до истечения
 * - Thread-safe через ConcurrentHashMap
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TokenService {

    private final BankProperties bankProperties;
    private final RestClient bankRestClient;
    
    /**
     * Хранилище токенов в памяти
     * Key: userId + "_" + bankId
     * Value: TokenInfo
     */
    private final Map<String, TokenInfo> tokenCache = new ConcurrentHashMap<>();

    /**
     * Получение access token для пользователя и банка
     * Если токен отсутствует или истек - запрашивает новый
     * 
     * @param userId ID пользователя
     * @param credentials Учетные данные для банка
     * @return Access token
     */
    @Cacheable(value = "bankTokens", key = "#userId + '_' + #credentials.bankId")
    public String getAccessToken(UUID userId, BankCredentials credentials) {
        String cacheKey = generateCacheKey(userId, credentials.bankId());
        
        TokenInfo cachedToken = tokenCache.get(cacheKey);
        
        // Проверяем наличие и валидность токена
        if (cachedToken != null && !isTokenExpired(cachedToken)) {
            log.debug("using cached token for user={}, bank={}", userId, credentials.bankId());
            return cachedToken.accessToken;
        }
        
        // Токен отсутствует или истек - запрашиваем новый
        log.info("requesting new access token for user={}, bank={}, clientId={}", userId, credentials.bankId(), credentials.clientId());
        return requestNewToken(userId, credentials);
    }

    /**
     * Запрос нового токена у внешнего банка
     * Open Banking Russia: POST /auth/bank-token?client_id=XXX&client_secret=YYY
     */
    private String requestNewToken(UUID userId, BankCredentials credentials) {
        BankProperties.BankConfig bankConfig = getBankConfig(credentials.bankId());
        
        // Извлекаем teamXXX из clientId (teamXXX-Y -> teamXXX)
        String teamId = extractTeamIdFromClientId(credentials.clientId());

        // Формируем URL с query параметрами для получения bank-token
        String tokenUrl = bankConfig.getBaseUrl() + bankConfig.getTokenEndpoint() 
                + "?client_id=" + teamId
                + "&client_secret=" + credentials.password();
        
        try {
            BankTokenResponseDto response = bankRestClient.post()
                    .uri(tokenUrl)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), 
                            (request, responseEntity) -> {
                                throw new BankApiException(
                                        "Failed to obtain token from bank: " + responseEntity.getStatusCode(),
                                        responseEntity.getStatusCode().value(),
                                        credentials.bankId()
                                );
                            })
                    .body(BankTokenResponseDto.class);
            
            if (response == null || response.accessToken() == null) {
                throw new BankApiException(
                        "Empty token response from bank",
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        credentials.bankId()
                );
            }
            
            // Сохраняем токен в кеше
            TokenInfo tokenInfo = new TokenInfo(
                    response.accessToken(),
                    response.refreshToken(),
                    Instant.now().plusSeconds(response.expiresIn())
            );
            
            String cacheKey = generateCacheKey(userId, credentials.bankId());
            tokenCache.put(cacheKey, tokenInfo);
            
            log.info("successfully obtained access token for user={}, bank={}, expires_in={}s",
                    userId, credentials.bankId(), response.expiresIn());
            
            return response.accessToken();
            
        } catch (RestClientException e) {
            log.error("failed to request token from bank={}: {}", credentials.bankId(), e.getMessage());
            throw new BankApiException(
                    "Failed to request token from bank: " + e.getMessage(),
                    e,
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    credentials.bankId()
            );
        }
    }

    /**
     * Проверка истечения токена
     * Токен считается истекшим за 60 сек до фактического истечения
     */
    private boolean isTokenExpired(TokenInfo tokenInfo) {
        return Instant.now().plusSeconds(60).isAfter(tokenInfo.expiresAt);
    }

    /**
     * Генерация ключа кеша
     */
    private String generateCacheKey(UUID userId, String bankId) {
        return userId.toString() + "_" + bankId;
    }

    /**
     * Получение конфигурации банка
     */
    private BankProperties.BankConfig getBankConfig(String bankId) {
        BankProperties.BankConfig config = bankProperties.getConfigs().get(bankId);
        if (config == null) {
            throw new IllegalArgumentException("Unknown bank: " + bankId);
        }
        return config;
    }

    /**
     * Очистка токена из кеша (при logout или ошибке авторизации)
     */
    public void evictToken(UUID userId, String bankId) {
        String cacheKey = generateCacheKey(userId, bankId);
        tokenCache.remove(cacheKey);
        log.info("evicted token for user={}, bank={}", userId, bankId);
    }

    /**
     * Внутренний класс для хранения информации о токене
     */
    private record TokenInfo(
            String accessToken,
            String refreshToken,
            Instant expiresAt
    ) {
    }

    /**
     * Извлекает teamXXX из clientId формата teamXXX-Y
     * Например: "team200-1" -> "team200"
     */
    private String extractTeamIdFromClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId is required");
        }

        // Ищем последний дефис и берем всё до него
        int lastDashIndex = clientId.lastIndexOf('-');
        if (lastDashIndex == -1) {
            // Если дефиса нет, возвращаем как есть (уже teamXXX формат)
            return clientId;
        }

        return clientId.substring(0, lastDashIndex);
    }
}

