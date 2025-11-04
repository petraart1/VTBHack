package com.bank.service;

import com.bank.config.BankCredentials;
import com.bank.config.BankProperties;
import com.bank.dto.bank.ConsentRequestDto;
import com.bank.dto.bank.ConsentResponseDto;
import com.bank.exception.BankApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис для управления согласиями на доступ к счетам
 * OpenBanking Russia: Account Consents API
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ConsentService {

    private final RestClient bankRestClient;
    private final BankProperties bankProperties;
    private final TokenService tokenService;

    // Кеш согласий: userId_bankId -> consent_id
        private final Map<String, CachedConsent> consentCache = new ConcurrentHashMap<>();

    /**
     * Получает consent_id для доступа к счетам.
     * Если согласие отсутствует - создаёт новое.
     *
     * @param userId      ID пользователя
     * @param credentials Учетные данные банка
     * @param clientId    Предопределенный client_id (team210-1 до team210-10)
     * @return Consent ID
     */
    public String getConsentId(UUID userId, BankCredentials credentials, String clientId) {
        String cacheKey = generateCacheKey(userId, credentials.bankId(), clientId);
        CachedConsent cachedConsent = consentCache.get(cacheKey);

        if (cachedConsent != null && !cachedConsent.isExpired()) {
            log.debug("using cached consent for user={}, bank={}, client_id={}, consent_id={}",                                                                               
                    userId, credentials.bankId(), clientId, cachedConsent.consentId());
            return cachedConsent.consentId();
        }

        // Согласие отсутствует или истекло - создаём новое
        log.info("creating new consent for user={}, bank={}, client_id={}", userId, credentials.bankId(), clientId);
        String consentId = createConsent(userId, credentials, clientId);
        
        // Кешируем на 89 дней (согласия обычно валидны 90 дней)
        consentCache.put(cacheKey, new CachedConsent(
                consentId, 
                Instant.now().plusSeconds(89 * 24 * 60 * 60)
        ));
        
        return consentId;
    }
    
    /**
     * Получает consent_id для доступа к счетам (устаревший метод, используйте с clientId).
     * @deprecated Используйте getConsentId(UUID, BankCredentials, String clientId)
     */
    @Deprecated
    public String getConsentId(UUID userId, BankCredentials credentials) {
        // Используем дефолтный client_id для обратной совместимости
        // Но лучше передавать client_id явно
        String defaultClientId = credentials.username() + "-1";
        return getConsentId(userId, credentials, defaultClientId);
    }

    /**
     * Создание согласия через OpenBanking API (публичный метод для контроллера)
     * POST /account-consents/request
     * 
     * @param userId ID пользователя
     * @param credentials Учетные данные банка
     * @param clientId Предопределенный client_id (team210-1 до team210-10)
     * @return ConsentResponseDto с информацией о созданном согласии
     */
    public ConsentResponseDto createConsentPublic(UUID userId, BankCredentials credentials, String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId is required. Must be in format: team210-1 to team210-10");
        }
        
        // Валидация формата client_id
        if (!clientId.matches("^team\\d+-\\d+$")) {
            throw new IllegalArgumentException("Invalid clientId format. Must be like: team210-1");
        }
        
        BankProperties.BankConfig bankConfig = getBankConfig(credentials.bankId());                                                                             
        String accessToken = tokenService.getAccessToken(userId, credentials);

        ConsentRequestDto requestDto = ConsentRequestDto.createDefault(clientId, credentials.username());

        String url = bankConfig.getBaseUrl() + "/account-consents/request";

        log.info("creating consent for client_id={}, bank={}, url={}", clientId, credentials.bankId(), url);

        try {
            ConsentResponseDto response = bankRestClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("X-Requesting-Bank", credentials.username())
                    .header("X-Request-ID", UUID.randomUUID().toString())
                    .body(requestDto)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            (request, responseEntity) -> {
                                throw new BankApiException(
                                        "Failed to create consent: " + responseEntity.getStatusCode(),
                                        responseEntity.getStatusCode().value(),
                                        credentials.bankId()
                                );
                            })
                    .body(ConsentResponseDto.class);

            if (response == null || response.consentId() == null) {
                throw new BankApiException(
                        "Empty consent response from bank",
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        credentials.bankId()
                );
            }

            log.info("consent created: consent_id={}, status={}, auto_approved={}",
                    response.consentId(), response.status(), response.autoApproved());

                                                            // Кешируем согласие, если оно успешно создано
            if ("approved".equalsIgnoreCase(response.status())) {
                String cacheKey = generateCacheKey(userId, credentials.bankId(), clientId);                                                                               
                consentCache.put(cacheKey, new CachedConsent(
                        response.consentId(),
                        Instant.now().plusSeconds(89 * 24 * 60 * 60)
                ));
            }

            return response;

        } catch (RestClientException e) {
            log.error("failed to create consent for bank={}: {}", credentials.bankId(), e.getMessage());
            throw new BankApiException(
                    "Failed to create consent: " + e.getMessage(),
                    e,
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    credentials.bankId()
            );
        }
    }

            /**
     * Создание согласия через OpenBanking API (приватный метод для внутреннего использования)
     * POST /account-consents/request
     */
    private String createConsent(UUID userId, BankCredentials credentials, String clientId) {
        BankProperties.BankConfig bankConfig = getBankConfig(credentials.bankId());                                                                             
        String accessToken = tokenService.getAccessToken(userId, credentials);
        
        ConsentRequestDto requestDto = ConsentRequestDto.createDefault(clientId, credentials.username());
        
        String url = bankConfig.getBaseUrl() + "/account-consents/request";
        
        log.info("creating consent for client_id={}, bank={}, url={}", clientId, credentials.bankId(), url);
        
        try {
            ConsentResponseDto response = bankRestClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("X-Requesting-Bank", credentials.username())
                    .header("X-Request-ID", UUID.randomUUID().toString())
                    .body(requestDto)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            (request, responseEntity) -> {
                                throw new BankApiException(
                                        "Failed to create consent: " + responseEntity.getStatusCode(),
                                        responseEntity.getStatusCode().value(),
                                        credentials.bankId()
                                );
                            })
                    .body(ConsentResponseDto.class);
            
            if (response == null || response.consentId() == null) {
                throw new BankApiException(
                        "Empty consent response from bank",
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        credentials.bankId()
                );
            }
            
            log.info("consent created: consent_id={}, status={}, auto_approved={}", 
                    response.consentId(), response.status(), response.autoApproved());
            
            return response.consentId();
            
        } catch (RestClientException e) {
            log.error("failed to create consent for bank={}: {}", credentials.bankId(), e.getMessage());
            throw new BankApiException(
                    "Failed to create consent: " + e.getMessage(),
                    e,
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    credentials.bankId()
            );
        }
    }

    private BankProperties.BankConfig getBankConfig(String bankId) {
        BankProperties.BankConfig config = bankProperties.getConfigs().get(bankId);
        if (config == null) {
            throw new IllegalArgumentException("Unknown bank: " + bankId);
        }
        return config;
    }

    private String generateCacheKey(UUID userId, String bankId, String clientId) {
        return userId.toString() + "_" + bankId + "_" + clientId;
    }

    /**
     * Внутренний класс для хранения кешированного consent с временем истечения
     */
    private record CachedConsent(String consentId, Instant expiryTime) {
        boolean isExpired() {
            return Instant.now().isAfter(expiryTime);
        }
    }
}
