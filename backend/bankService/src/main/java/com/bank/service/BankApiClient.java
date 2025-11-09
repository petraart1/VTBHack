package com.bank.service;

import com.bank.config.BankingConstants;
import com.bank.dto.common.BankCredentials;
import com.bank.config.BankProperties;
import com.bank.exception.BankNotFoundException;
import com.bank.dto.response.ExternalAccountResponseDto;
import com.bank.dto.response.ExternalBalanceResponseDto;
import com.bank.dto.response.ExternalTransactionResponseDto;
import com.bank.exception.BankApiException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Клиент для взаимодействия с внешними Bank API
 * Реализует все операции согласно Open Banking Russia v2.1
 * 
 * Особенности:
 * - Автоматическое управление токенами через TokenService
 * - Retry policy: 3 попытки с экспоненциальной задержкой (Resilience4j)
 * - Circuit Breaker: предотвращение каскадных сбоев (Resilience4j)
 * - Логирование всех запросов
 */
@Service
@RequiredArgsConstructor
public class BankApiClient {

    private static final Logger log = LoggerFactory.getLogger(BankApiClient.class);

    private final BankProperties bankProperties;
    private final TokenService tokenService;
    private final RestClient bankRestClient;
    private final WebClient bankWebClient;
    private final ConsentService consentService;

    /**
     * Получение списка счетов пользователя из внешнего банка
     * 
     * @param userId ID пользователя
     * @param credentials Учетные данные банка
     * @return Список счетов
     */

    /**
     * Реактивная версия получения списка счетов
     */
    @CircuitBreaker(name = "bankApi", fallbackMethod = "getAccountsReactiveFallback")
    public reactor.core.publisher.Mono<ExternalAccountResponseDto> getAccountsReactive(UUID userId, BankCredentials credentials) {
        BankProperties.BankConfig bankConfig = getBankConfig(credentials.bankId());
        String accessToken = tokenService.getAccessToken(userId, credentials);
        
        // Используем clientId из credentials (обязательно в формате teamXXX-Y)
        String clientId = credentials.clientId();
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId is required and must be in format teamXXX-Y");
        }
        
        // Получаем consent_id для межбанкового запроса
        String consentId = consentService.getConsentId(userId, credentials, clientId);
        
        // URL с client_id query параметром для межбанкового запроса
        String url = bankConfig.getBaseUrl() + bankConfig.getAccountsEndpoint() + "?client_id=" + clientId;
        
        // Извлекаем teamId для логирования и заголовков
        String teamId = extractTeamIdFromClientId(clientId);
        log.info("fetching accounts reactively from bank={} for user={}, client_id={}, team_id={}, consent_id={}",
                credentials.bankId(), userId, "***", "***", "***");

        return bankWebClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Request-ID", UUID.randomUUID().toString())
                .header("X-Requesting-Bank", teamId)  // Используем teamXXX, а не username
                .header("X-Consent-Id", consentId)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> {
                            log.error("External API returned error status: {} for URL: {}", clientResponse.statusCode(), url);
                            return clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("Response body: {}", errorBody);
                                        handleBankApiError(clientResponse.statusCode().value(),
                                                credentials.bankId(), "getAccountsReactive");
                                        return reactor.core.publisher.Mono.error(
                                                new BankApiException(
                                                        "Failed to fetch accounts: " + clientResponse.statusCode(),
                                                        clientResponse.statusCode().value(),
                                                        credentials.bankId()
                                                )
                                        );
                                    });
                        })
                .bodyToMono(ExternalAccountResponseDto.class)
                .doOnNext(response -> {
                    log.info("External API response received: accounts count={}, has data={}",
                            response.accounts() != null ? response.accounts().size() : "null",
                            response != null ? "yes" : "no");

                    // Проверяем, что accounts не null
                    if (response.accounts() == null || response.accounts().isEmpty()) {
                        log.warn("accounts list is null or empty in response from bank={}, response present={}",
                                credentials.bankId(), response != null ? "yes" : "no");
                        throw new BankApiException(
                                "Empty accounts response from bank",
                                502,
                                credentials.bankId()
                        );
                    }

                    log.info("successfully fetched {} accounts from bank={}",
                            response.accounts().size(), credentials.bankId());
                })
                .onErrorResume(throwable -> {
                    log.error("failed to fetch accounts reactively from bank={}: {}", credentials.bankId(), throwable.getMessage());
                    return reactor.core.publisher.Mono.error(throwable);
                });
    }

    /**
     * Fallback для реактивного getAccounts
     */
    public reactor.core.publisher.Mono<ExternalAccountResponseDto> getAccountsReactiveFallback(
            UUID userId, BankCredentials credentials, Throwable throwable) {

        log.error("CIRCUIT BREAKER: getAccountsReactive failed for user={}, bank={}, error={}. Circuit breaker activated!",
                userId, credentials.bankId(), throwable.getMessage());

        // Возвращаем пустой ответ - Circuit Breaker активен
        return reactor.core.publisher.Mono.just(
                new ExternalAccountResponseDto(
                        new ExternalAccountResponseDto.Data(List.of()),
                        null,
                        null
                )
        );
    }

    /**
     * Fallback для реактивного getTransactions
     */
    public reactor.core.publisher.Mono<ExternalTransactionResponseDto> getTransactionsReactiveFallback(
            UUID userId, BankCredentials credentials, UUID accountId, Throwable throwable) {

        log.error("CIRCUIT BREAKER: getTransactionsReactive failed for user={}, bank={}, account={}, error={}. Circuit breaker activated!",
                userId, credentials.bankId(), accountId, throwable.getMessage());

        // Возвращаем пустой ответ
        return reactor.core.publisher.Mono.just(
                new ExternalTransactionResponseDto(
                        new ExternalTransactionResponseDto.Data(List.of()),
                        null, null
                )
        );
    }

    /**
     * Fallback для реактивного getBalances
     */
    public reactor.core.publisher.Mono<ExternalBalanceResponseDto> getBalancesReactiveFallback(
            UUID userId, BankCredentials credentials, UUID accountId, Throwable throwable) {

        log.error("CIRCUIT BREAKER: getBalancesReactive failed for user={}, bank={}, account={}, error={}. Circuit breaker activated!",
                userId, credentials.bankId(), accountId, throwable.getMessage());

        // Возвращаем пустой ответ
        return reactor.core.publisher.Mono.just(
                new ExternalBalanceResponseDto(
                        new ExternalBalanceResponseDto.Data(List.of()),
                        null, null
                )
        );
    }

    /**
     * Реактивная версия получения транзакций
     */
    @CircuitBreaker(name = "bankApi", fallbackMethod = "getTransactionsReactiveFallback")
    public reactor.core.publisher.Mono<ExternalTransactionResponseDto> getTransactionsReactive(
            UUID userId, BankCredentials credentials, UUID accountId) {

        BankProperties.BankConfig bankConfig = getBankConfig(credentials.bankId());
        String accessToken = tokenService.getAccessToken(userId, credentials);

        String clientId = credentials.clientId();
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId is required and must be in format teamXXX-Y");
        }

        String consentId = consentService.getConsentId(userId, credentials, clientId);

        String url = bankConfig.getBaseUrl() + bankConfig.getTransactionsEndpoint()
                .replace("{accountId}", accountId.toString()) + "?client_id=" + clientId;
        // Примечание: для реактивной версии можно добавить параметры from_booking_date_time и to_booking_date_time

        String teamId = extractTeamIdFromClientId(clientId);
        log.info("fetching transactions reactively from bank={} for user={}, account={}, client_id={}, team_id={}, consent_id={}",
                credentials.bankId(), userId, accountId, "***", "***", "***");

        return bankWebClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Request-ID", UUID.randomUUID().toString())
                .header("X-Requesting-Bank", teamId)
                .header("X-Consent-Id", consentId)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> {
                            log.error("External API returned error status: {} for URL: {}", clientResponse.statusCode(), url);
                            return clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("Response body: {}", errorBody);
                                        handleBankApiError(clientResponse.statusCode().value(),
                                                credentials.bankId(), "getTransactionsReactive");
                                        return reactor.core.publisher.Mono.error(
                                                new BankApiException(
                                                        "Failed to fetch transactions: " + clientResponse.statusCode(),
                                                        clientResponse.statusCode().value(),
                                                        credentials.bankId()
                                                )
                                        );
                                    });
                        })
                .bodyToMono(ExternalTransactionResponseDto.class)
                .doOnNext(response -> {
                    log.info("External API response received: transactions count={}, has data={}",
                            response.transactions() != null ? response.transactions().size() : "null",
                            response != null ? "yes" : "no");

                    if (response.transactions() == null || response.transactions().isEmpty()) {
                        log.warn("transactions list is null or empty in response from bank={}, response present={}",
                                credentials.bankId(), response != null ? "yes" : "no");
                        throw new BankApiException(
                                "Empty transactions response from bank",
                                502,
                                credentials.bankId()
                        );
                    }

                    log.info("successfully fetched {} transactions from bank={}",
                            response.transactions().size(), credentials.bankId());
                })
                .onErrorResume(throwable -> {
                    log.error("failed to fetch transactions reactively from bank={}: {}", credentials.bankId(), throwable.getMessage());
                    return reactor.core.publisher.Mono.error(throwable);
                });
    }

    /**
     * Реактивная версия получения балансов
     */
    @CircuitBreaker(name = "bankApi", fallbackMethod = "getBalancesReactiveFallback")
    public reactor.core.publisher.Mono<ExternalBalanceResponseDto> getBalancesReactive(
            UUID userId, BankCredentials credentials, UUID accountId) {

        BankProperties.BankConfig bankConfig = getBankConfig(credentials.bankId());
        String accessToken = tokenService.getAccessToken(userId, credentials);

        String clientId = credentials.clientId();
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId is required and must be in format teamXXX-Y");
        }

        String consentId = consentService.getConsentId(userId, credentials, clientId);

        String url = bankConfig.getBaseUrl() + bankConfig.getBalancesEndpoint()
                .replace("{accountId}", accountId.toString()) + "?client_id=" + clientId;

        String teamId = extractTeamIdFromClientId(clientId);
        log.info("fetching balances reactively from bank={} for user={}, account={}, client_id={}, team_id={}, consent_id={}",
                credentials.bankId(), userId, accountId, "***", "***", "***");

        return bankWebClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + accessToken)
                .header("X-Request-ID", UUID.randomUUID().toString())
                .header("X-Requesting-Bank", teamId)
                .header("X-Consent-Id", consentId)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> {
                            log.error("External API returned error status: {} for URL: {}", clientResponse.statusCode(), url);
                            return clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.error("Response body: {}", errorBody);
                                        handleBankApiError(clientResponse.statusCode().value(),
                                                credentials.bankId(), "getBalancesReactive");
                                        return reactor.core.publisher.Mono.error(
                                                new BankApiException(
                                                        "Failed to fetch balances: " + clientResponse.statusCode(),
                                                        clientResponse.statusCode().value(),
                                                        credentials.bankId()
                                                )
                                        );
                                    });
                        })
                .bodyToMono(ExternalBalanceResponseDto.class)
                .doOnNext(response -> {
                    log.info("External API response received: balances count={}, has data={}",
                            response.balances() != null ? response.balances().size() : "null",
                            response != null ? "yes" : "no");

                    if (response.balances() == null || response.balances().isEmpty()) {
                        log.warn("balances list is null or empty in response from bank={}, response present={}",
                                credentials.bankId(), response != null ? "yes" : "no");
                        throw new BankApiException(
                                "Empty balances response from bank",
                                502,
                                credentials.bankId()
                        );
                    }

                    log.info("successfully fetched {} balances from bank={}",
                            response.balances().size(), credentials.bankId());
                })
                .onErrorResume(throwable -> {
                    log.error("failed to fetch balances reactively from bank={}: {}", credentials.bankId(), throwable.getMessage());
                    return reactor.core.publisher.Mono.error(throwable);
                });
    }

    public ExternalAccountResponseDto getAccounts(UUID userId, BankCredentials credentials) {
        return getAccounts(userId, credentials, null);
    }

    public ExternalAccountResponseDto getAccounts(UUID userId, BankCredentials credentials, String forceConsentId) {
        BankProperties.BankConfig bankConfig = getBankConfig(credentials.bankId());
        String accessToken = tokenService.getAccessToken(userId, credentials);

        // Используем clientId из credentials (обязательно в формате teamXXX-Y)
        String clientId = credentials.clientId();
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId is required and must be in format teamXXX-Y");
        }

        // Используем переданный consentId или получаем новый
        String consentId;
        if (forceConsentId != null && !forceConsentId.isBlank()) {
            consentId = forceConsentId;
            log.info("Using forced consent_id={} for user={}, bank={}", consentId, userId, credentials.bankId());
        } else {
            consentId = consentService.getConsentId(userId, credentials, clientId);
        }
        
        // URL с client_id query параметром для межбанкового запроса
        String url = bankConfig.getBaseUrl() + bankConfig.getAccountsEndpoint() + "?client_id=" + clientId;

        // Извлекаем teamId для логирования и заголовков
        String teamId = extractTeamIdFromClientId(clientId);
        log.info("fetching accounts from bank={} for user={}, client_id={}, team_id={}, consent_id={}",
                credentials.bankId(), userId, "***", "***", "***");
        log.info("External API URL: {}", url);

        try {
            // Добавляем логирование полного ответа
            ExternalAccountResponseDto response = bankRestClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("X-Request-ID", UUID.randomUUID().toString())
                    .header("X-Requesting-Bank", teamId)  // Используем teamXXX, а не username
                    .header("X-Consent-Id", consentId)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            (request, responseEntity) -> {
                                log.error("External API returned error status: {} for URL: {}", responseEntity.getStatusCode(), url);
                                log.error("Response body: {}", responseEntity.getBody());
                                handleBankApiError(responseEntity.getStatusCode().value(), 
                                        credentials.bankId(), "getAccounts");
                            })
                    .onStatus(status -> status.is2xxSuccessful(),
                            (request, responseEntity) -> {
                                log.info("External API returned success status: {} for URL: {}", responseEntity.getStatusCode(), url);
                                log.info("RAW API RESPONSE BODY: {}", responseEntity.getBody());
                            })
                    .body(ExternalAccountResponseDto.class);
            
            if (response == null) {
                log.error("Response is null from external API for URL: {}", url);
                throw new BankApiException(
                        "Empty response from bank",
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        credentials.bankId()
                );
            }
            
            // Логируем ответ без чувствительных данных
            log.info("External API response received: accounts count={}, has data={}",
                    response.accounts() != null ? response.accounts().size() : "null",
                    response != null ? "yes" : "no");

            // Проверяем, что accounts не null
            if (response.accounts() == null || response.accounts().isEmpty()) {
                log.warn("accounts list is null or empty in response from bank={}, response present={}",
                        credentials.bankId(), response != null ? "yes" : "no");
                // Возвращаем DTO с пустым списком
                return new ExternalAccountResponseDto(
                        new ExternalAccountResponseDto.Data(List.of()),
                        null,
                        null
                );
            }
            
            log.info("successfully fetched {} accounts from bank={}", 
                    response.accounts().size(), credentials.bankId());
            
            return response;
            
        } catch (RestClientException e) {
            log.error("failed to fetch accounts from bank={}: {}", credentials.bankId(), e.getMessage());
            throw new BankApiException(
                    "Failed to fetch accounts: " + e.getMessage(),
                    e,
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    credentials.bankId()
            );
        }
    }

    /**
     * Получение транзакций по счету
     * 
     * @param userId ID пользователя
     * @param credentials Учетные данные банка
     * @param accountId ID счета во внешнем банке
     * @param from Начало периода
     * @param to Конец периода
     * @return Список транзакций
     */
        @Retry(name = "bankApi", fallbackMethod = "getTransactionsFallback")
    @CircuitBreaker(name = "bankApi", fallbackMethod = "getTransactionsFallback")
    public ExternalTransactionResponseDto getTransactions(
            UUID userId,
            BankCredentials credentials,
            String accountId,
            LocalDateTime from,
            LocalDateTime to) {
        return getTransactions(userId, credentials, accountId, from, to, 1, 50);
    }

    /**
     * Получение транзакций по счету с пагинацией
     * 
     * @param userId ID пользователя
     * @param credentials Учетные данные банка
     * @param accountId ID счета во внешнем банке
     * @param from Начало периода
     * @param to Конец периода
     * @param page Номер страницы (default: 1)
     * @param limit Количество транзакций на странице (default: 50, max: 500)
     * @return Список транзакций
     */
    @Retry(name = "bankApi", fallbackMethod = "getTransactionsFallback")
    @CircuitBreaker(name = "bankApi", fallbackMethod = "getTransactionsFallback")
    public ExternalTransactionResponseDto getTransactions(
            UUID userId,
            BankCredentials credentials,
            String accountId,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int limit) {
        
        BankProperties.BankConfig bankConfig = getBankConfig(credentials.bankId());                                                                             
        String accessToken = tokenService.getAccessToken(userId, credentials);
        
        // Используем clientId из credentials (обязательно в формате teamXXX-Y)
        String clientId = credentials.clientId();
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId is required and must be in format teamXXX-Y");
        }
        
        // Получаем consent_id для межбанкового запроса
        String consentId = consentService.getConsentId(userId, credentials, clientId);
        
        // Валидация параметров пагинации согласно документации
        int validPage = Math.max(1, page);
        int validLimit = Math.min(Math.max(1, limit), 500);  // min: 1, max: 500
        
        String endpoint = bankConfig.getTransactionsEndpoint().replace("{accountId}", accountId);                                                               
        String url = bankConfig.getBaseUrl() + endpoint +
                "?from_booking_date_time=" + from.format(BankingConstants.ISO_FORMATTER) +
                "&to_booking_date_time=" + to.format(BankingConstants.ISO_FORMATTER) +
                "&page=" + validPage +
                "&limit=" + validLimit +
                "&client_id=" + clientId;
        
        // Извлекаем teamId для заголовков
        String teamId = extractTeamIdFromClientId(clientId);
        log.info("fetching transactions from bank={} for account={}, client_id={}, team_id={}, consent_id={}",
                credentials.bankId(), accountId, "***", "***", "***");
        
        try {
            ExternalTransactionResponseDto response = bankRestClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("X-Request-ID", UUID.randomUUID().toString())
                    .header("X-Requesting-Bank", teamId)  // Используем teamXXX, а не username
                    .header("X-Consent-Id", consentId)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            (request, responseEntity) -> {
                                log.error("External API returned error status: {} for URL: {}", responseEntity.getStatusCode(), url);
                                log.error("Response body: {}", responseEntity.getBody());
                                handleBankApiError(responseEntity.getStatusCode().value(),
                                        credentials.bankId(), "getTransactions");
                            })
                    .onStatus(status -> status.is2xxSuccessful(),
                            (request, responseEntity) -> {
                                log.info("External API returned success status: {} for URL: {}", responseEntity.getStatusCode(), url);
                                log.info("RAW API RESPONSE BODY: {}", responseEntity.getBody());
                            })
                    .body(ExternalTransactionResponseDto.class);
            
            if (response == null) {
                throw new BankApiException(
                        "Empty response from bank",
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        credentials.bankId()
                );
            }
            
            // Логируем ответ без чувствительных данных
            log.info("External API response received: transactions count={}, has data={}",
                    response.transactions() != null ? response.transactions().size() : "null",
                    response != null ? "yes" : "no");

            // Проверяем, что transactions не null
            if (response.transactions() == null || response.transactions().isEmpty()) {
                log.warn("transactions list is null or empty in response from bank={}, response present={}",
                        credentials.bankId(), response != null ? "yes" : "no");
                // Возвращаем DTO с пустым списком
                return new ExternalTransactionResponseDto(
                        new ExternalTransactionResponseDto.Data(List.of()),
                        null,
                        null
                );
            }
            
            log.info("successfully fetched {} transactions from bank={}",
                    response.transactions().size(), credentials.bankId());
            
            return response;
            
        } catch (RestClientException e) {
            log.error("failed to fetch transactions from bank={}: {}", credentials.bankId(), e.getMessage());
            throw new BankApiException(
                    "Failed to fetch transactions: " + e.getMessage(),
                    e,
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    credentials.bankId()
            );
        }
    }

    /**
     * Получение балансов по счету
     * 
     * @param userId ID пользователя
     * @param credentials Учетные данные банка
     * @param accountId ID счета во внешнем банке
     * @return Балансы счета
     */
        @Retry(name = "bankApi", fallbackMethod = "getBalancesFallback")
    @CircuitBreaker(name = "bankApi", fallbackMethod = "getBalancesFallback")
    public ExternalBalanceResponseDto getBalances(
            UUID userId,
            BankCredentials credentials,
            String accountId) {
        
        BankProperties.BankConfig bankConfig = getBankConfig(credentials.bankId());                                                                             
        String accessToken = tokenService.getAccessToken(userId, credentials);
        
        // Используем clientId из credentials (обязательно в формате teamXXX-Y)
        String clientId = credentials.clientId();
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId is required and must be in format teamXXX-Y");
        }
        
        // Получаем consent_id для межбанкового запроса
        String consentId = consentService.getConsentId(userId, credentials, clientId);
        
        String endpoint = bankConfig.getBalancesEndpoint().replace("{accountId}", accountId);                                                                   
        String url = bankConfig.getBaseUrl() + endpoint + "?client_id=" + clientId;
        
        // Извлекаем teamId для заголовков
        String teamId = extractTeamIdFromClientId(clientId);
        log.info("fetching balances from bank={} for account={}, client_id={}, team_id={}, consent_id={}",
                credentials.bankId(), accountId, "***", "***", "***");
        
        try {
            ExternalBalanceResponseDto response = bankRestClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("X-Request-ID", UUID.randomUUID().toString())
                    .header("X-Requesting-Bank", teamId)  // Используем teamXXX, а не username
                    .header("X-Consent-Id", consentId)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            (request, responseEntity) -> {
                                log.error("External API returned error status: {} for URL: {}", responseEntity.getStatusCode(), url);
                                log.error("Response body: {}", responseEntity.getBody());
                                handleBankApiError(responseEntity.getStatusCode().value(),
                                        credentials.bankId(), "getBalances");
                            })
                    .onStatus(status -> status.is2xxSuccessful(),
                            (request, responseEntity) -> {
                                log.info("External API returned success status: {} for URL: {}", responseEntity.getStatusCode(), url);
                                log.info("RAW API RESPONSE BODY: {}", responseEntity.getBody());
                            })
                    .body(ExternalBalanceResponseDto.class);
            
            if (response == null) {
                throw new BankApiException(
                        "Empty response from bank",
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        credentials.bankId()
                );
            }
            
            // Логируем ответ без чувствительных данных
            log.info("External API response received: balances count={}, has data={}",
                    response.balances() != null ? response.balances().size() : "null",
                    response != null ? "yes" : "no");

            // Проверяем, что balances не null
            if (response.balances() == null || response.balances().isEmpty()) {
                log.warn("balances list is null or empty in response from bank={}, response present={}",
                        credentials.bankId(), response != null ? "yes" : "no");
                // Возвращаем DTO с пустым списком
                return new ExternalBalanceResponseDto(
                        new ExternalBalanceResponseDto.Data(List.of()),
                        null,
                        null
                );
            }
            
            log.info("successfully fetched {} balances from bank={}",
                    response.balances().size(), credentials.bankId());
            
            return response;
            
        } catch (RestClientException e) {
            log.error("failed to fetch balances from bank={}: {}", credentials.bankId(), e.getMessage());
            throw new BankApiException(
                    "Failed to fetch balances: " + e.getMessage(),
                    e,
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    credentials.bankId()
            );
        }
    }

    /**
     * Получение конфигурации банка
     */
    private BankProperties.BankConfig getBankConfig(String bankId) {
        BankProperties.BankConfig config = bankProperties.getConfigs().get(bankId);
        if (config == null) {
            throw new BankNotFoundException(bankId);
        }
        return config;
    }

    /**
     * Извлекает teamXXX из clientId формата teamXXX-Y
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

    /**
     * Обработка ошибок Bank API
     */
    private void handleBankApiError(int statusCode, String bankId, String operation) {
        String message = switch (statusCode) {
            case 401 -> "Unauthorized - invalid or expired token";
            case 403 -> "Forbidden - insufficient permissions";
            case 404 -> "Not found - account or resource does not exist";
            case 429 -> "Too many requests - rate limit exceeded";
            case 500, 502, 503 -> "Bank API unavailable";
            default -> "Bank API error: " + statusCode;
        };
        
        log.error("bank api error: bank={}, operation={}, status={}, message={}",
                bankId, operation, statusCode, message);
        
        throw new BankApiException(message, statusCode, bankId);
    }

    // ==================== FALLBACK METHODS ====================

    /**
     * Fallback для getAccounts
     * Вызывается после исчерпания retry attempts или при открытом Circuit Breaker
     */
    private ExternalAccountResponseDto getAccountsFallback(UUID userId, BankCredentials credentials, Exception e) {
        log.error("fallback: getAccounts failed for user={}, bank={}, error={}",
                userId, credentials.bankId(), e.getMessage());
        
        throw new BankApiException(
                "Bank API unavailable after retries: " + e.getMessage(),
                e,
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                credentials.bankId()
        );
    }

    /**
     * Fallback для getTransactions (перегрузка без пагинации)
     */
    private ExternalTransactionResponseDto getTransactionsFallback(
            UUID userId, BankCredentials credentials, String accountId,
            LocalDateTime from, LocalDateTime to, Exception e) {
        return getTransactionsFallback(userId, credentials, accountId, from, to, 1, 50, e);
    }

    /**
     * Fallback для getTransactions (с пагинацией)
     */
    private ExternalTransactionResponseDto getTransactionsFallback(
            UUID userId, BankCredentials credentials, String accountId,
            LocalDateTime from, LocalDateTime to, int page, int limit, Exception e) {
        
        log.error("fallback: getTransactions failed for user={}, bank={}, account={}, error={}",
                userId, credentials.bankId(), accountId, e.getMessage());
        
        throw new BankApiException(
                "Bank API unavailable after retries: " + e.getMessage(),
                e,
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                credentials.bankId()
        );
    }

    /**
     * Fallback для getBalances
     */
    private ExternalBalanceResponseDto getBalancesFallback(
            UUID userId, BankCredentials credentials, String accountId, Exception e) {
        
        log.error("fallback: getBalances failed for user={}, bank={}, account={}, error={}",
                userId, credentials.bankId(), accountId, e.getMessage());
        
        throw new BankApiException(
                "Bank API unavailable after retries: " + e.getMessage(),
                e,
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                credentials.bankId()
        );
    }
}

