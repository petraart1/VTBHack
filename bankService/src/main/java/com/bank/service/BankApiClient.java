package com.bank.service;

import com.bank.config.BankCredentials;
import com.bank.config.BankProperties;
import com.bank.dto.bank.ExternalAccountResponseDto;
import com.bank.dto.bank.ExternalBalanceResponseDto;
import com.bank.dto.bank.ExternalTransactionResponseDto;
import com.bank.exception.BankApiException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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
@Slf4j
@RequiredArgsConstructor
public class BankApiClient {

    private final BankProperties bankProperties;
    private final TokenService tokenService;
    private final RestClient bankRestClient;
    private final ConsentService consentService;
    
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    /**
     * Получение списка счетов пользователя из внешнего банка
     * 
     * @param userId ID пользователя
     * @param credentials Учетные данные банка
     * @return Список счетов
     */
    @Retry(name = "bankApi", fallbackMethod = "getAccountsFallback")
    @CircuitBreaker(name = "bankApi", fallbackMethod = "getAccountsFallback")
        public ExternalAccountResponseDto getAccounts(UUID userId, BankCredentials credentials) {
        BankProperties.BankConfig bankConfig = getBankConfig(credentials.bankId());
        String accessToken = tokenService.getAccessToken(userId, credentials);
        
        // Используем clientId из credentials или дефолтное значение team210-1
        String clientId = credentials.clientId();
        if (clientId == null || clientId.isBlank()) {
            clientId = credentials.username() + "-1";  // Дефолтный client_id
            log.warn("clientId not provided in credentials, using default: {}", clientId);
        }
        
        // Получаем consent_id для межбанкового запроса
        String consentId = consentService.getConsentId(userId, credentials, clientId);
        
        // URL с client_id query параметром для межбанкового запроса
        String url = bankConfig.getBaseUrl() + bankConfig.getAccountsEndpoint() + "?client_id=" + clientId;
        
        log.info("fetching accounts from bank={} for user={}, client_id={}, consent_id={}",
                credentials.bankId(), userId, clientId, consentId);
        
        try {
            ExternalAccountResponseDto response = bankRestClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("X-Request-ID", UUID.randomUUID().toString())
                    .header("X-Requesting-Bank", credentials.username())
                    .header("X-Consent-Id", consentId)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            (request, responseEntity) -> {
                                handleBankApiError(responseEntity.getStatusCode().value(), 
                                        credentials.bankId(), "getAccounts");
                            })
                    .body(ExternalAccountResponseDto.class);
            
            if (response == null) {
                throw new BankApiException(
                        "Empty response from bank",
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        credentials.bankId()
                );
            }
            
            // Проверяем, что accounts не null
            if (response.accounts() == null) {
                log.warn("accounts list is null in response from bank={}, returning empty list", credentials.bankId());
                // Возвращаем DTO с пустым списком вместо null
                return new ExternalAccountResponseDto(List.of());
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
        
        BankProperties.BankConfig bankConfig = getBankConfig(credentials.bankId());                                                                             
        String accessToken = tokenService.getAccessToken(userId, credentials);
        
        // Используем clientId из credentials или дефолтное значение
        String clientId = credentials.clientId();
        if (clientId == null || clientId.isBlank()) {
            clientId = credentials.username() + "-1";
            log.warn("clientId not provided in credentials, using default: {}", clientId);
        }
        
        // Получаем consent_id для межбанкового запроса
        String consentId = consentService.getConsentId(userId, credentials, clientId);
        
        String endpoint = bankConfig.getTransactionsEndpoint().replace("{accountId}", accountId);                                                               
        String url = bankConfig.getBaseUrl() + endpoint +
                "?from=" + from.format(ISO_FORMATTER) +
                "&to=" + to.format(ISO_FORMATTER) +
                "&client_id=" + clientId;
        
        log.info("fetching transactions from bank={} for account={}, client_id={}, consent_id={}", 
                credentials.bankId(), accountId, clientId, consentId);
        
        try {
            ExternalTransactionResponseDto response = bankRestClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("X-Request-ID", UUID.randomUUID().toString())
                    .header("X-Requesting-Bank", credentials.username())
                    .header("X-Consent-Id", consentId)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            (request, responseEntity) -> {
                                handleBankApiError(responseEntity.getStatusCode().value(),
                                        credentials.bankId(), "getTransactions");
                            })
                    .body(ExternalTransactionResponseDto.class);
            
            if (response == null) {
                throw new BankApiException(
                        "Empty response from bank",
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        credentials.bankId()
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
        
        // Используем clientId из credentials или дефолтное значение
        String clientId = credentials.clientId();
        if (clientId == null || clientId.isBlank()) {
            clientId = credentials.username() + "-1";
            log.warn("clientId not provided in credentials, using default: {}", clientId);
        }
        
        // Получаем consent_id для межбанкового запроса
        String consentId = consentService.getConsentId(userId, credentials, clientId);
        
        String endpoint = bankConfig.getBalancesEndpoint().replace("{accountId}", accountId);                                                                   
        String url = bankConfig.getBaseUrl() + endpoint + "?client_id=" + clientId;
        
        log.info("fetching balances from bank={} for account={}, client_id={}, consent_id={}", 
                credentials.bankId(), accountId, clientId, consentId);
        
        try {
            ExternalBalanceResponseDto response = bankRestClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("X-Request-ID", UUID.randomUUID().toString())
                    .header("X-Requesting-Bank", credentials.username())
                    .header("X-Consent-Id", consentId)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            (request, responseEntity) -> {
                                handleBankApiError(responseEntity.getStatusCode().value(),
                                        credentials.bankId(), "getBalances");
                            })
                    .body(ExternalBalanceResponseDto.class);
            
            if (response == null) {
                throw new BankApiException(
                        "Empty response from bank",
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        credentials.bankId()
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
            throw new IllegalArgumentException("Unknown bank: " + bankId);
        }
        return config;
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
     * Fallback для getTransactions
     */
    private ExternalTransactionResponseDto getTransactionsFallback(
            UUID userId, BankCredentials credentials, String accountId,
            LocalDateTime from, LocalDateTime to, Exception e) {
        
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

