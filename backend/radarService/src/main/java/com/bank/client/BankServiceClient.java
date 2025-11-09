package com.bank.client;

import com.bank.dto.response.AccountDto;
import com.bank.dto.response.TransactionDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Клиент для интеграции с BankService
 * Использует WebClient для асинхронных вызовов
 */
@Component
@Slf4j
public class BankServiceClient {

    private final WebClient webClient;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public BankServiceClient(
            @Value("${bank-service.url}") String bankServiceUrl,
            WebClient.Builder webClientBuilder
    ) {
        this.webClient = webClientBuilder
                .baseUrl(bankServiceUrl)
                .build();
    }

    /**
     * Получить все счета пользователя
     */
    @CircuitBreaker(name = "bankService", fallbackMethod = "getAccountsFallback")
    @Retry(name = "bankService")
    public List<AccountDto> getAccounts(String jwtToken) {
        log.debug("Fetching accounts from BankService");

        return webClient.get()
                .uri("/api/v1/bank/accounts")
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<AccountDto>>() {})
                .block();
    }

    /**
     * Получить транзакции по счету
     */
    @CircuitBreaker(name = "bankService", fallbackMethod = "getTransactionsFallback")
    @Retry(name = "bankService")
    public List<TransactionDto> getTransactions(
            UUID accountId,
            LocalDateTime from,
            LocalDateTime to,
            String jwtToken
    ) {
        log.debug("Fetching transactions for account: {}, from: {}, to: {}", accountId, from, to);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/bank/accounts/{accountId}/transactions")
                        .queryParam("fromBookingDateTime", from.format(ISO_FORMATTER))
                        .queryParam("toBookingDateTime", to.format(ISO_FORMATTER))
                        .queryParam("size", 1000)  // максимальный размер страницы
                        .build(accountId))
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<TransactionDto>>() {})
                .block();
    }

    /**
     * Получить все транзакции пользователя за период
     */
    
    public List<TransactionDto> getAllUserTransactions(
            LocalDateTime from,
            LocalDateTime to,
            String jwtToken
    ) {
        log.info("Fetching all transactions from {} to {}", from, to);

        // 1. Получаем все счета пользователя
        List<AccountDto> accounts = getAccounts(jwtToken);

        if (accounts == null || accounts.isEmpty()) {
            log.warn("No accounts found for user");
            return Collections.emptyList();
        }

        log.debug("Found {} accounts, fetching transactions", accounts.size());

        // 2. Для каждого счета получаем транзакции
        return accounts.stream()
                .flatMap(account -> {
                    try {
                        List<TransactionDto> transactions = getTransactions(
                                account.id(),
                                from,
                                to,
                                jwtToken
                        );
                        return transactions != null ? transactions.stream() : java.util.stream.Stream.empty();
                    } catch (Exception e) {
                        log.error("Failed to fetch transactions for account {}: {}",
                                account.id(), e.getMessage());
                        return java.util.stream.Stream.empty();
                    }
                })
                .toList();
    }

    // Fallback methods 
    private List<AccountDto> getAccountsFallback(String jwtToken, Throwable t) {
        log.error("Failed to fetch accounts from BankService: {}", t.getMessage());
        return Collections.emptyList();
    }

    private List<TransactionDto> getTransactionsFallback(
            UUID accountId,
            LocalDateTime from,
            LocalDateTime to,
            String jwtToken,
            Throwable t
    ) {
        log.error("Failed to fetch transactions for account {}: {}", accountId, t.getMessage());
        return Collections.emptyList();
    }
}

