package com.bank.service;

import com.bank.config.BankingConstants;
import com.bank.dto.common.BankCredentials;
import com.bank.dto.response.ExternalAccountResponseDto;
import com.bank.dto.response.ExternalBalanceResponseDto;
import com.bank.dto.response.ExternalTransactionResponseDto;
import com.bank.exception.SyncFailedException;
import com.bank.model.AccountBalance;
import com.bank.model.AccountTransaction;
import com.bank.model.BankAccount;
import com.bank.model.SyncLog;
import com.bank.repository.AccountRepository;
import com.bank.repository.SyncLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Сервис для синхронизации данных из внешних банков
 * Выполняет:
 * - Синхронизацию счетов
 * - Синхронизацию транзакций (за последние 6 месяцев)
 * - Синхронизацию балансов
 * - Логирование всех операций в sync_logs
 */
@Service
@RequiredArgsConstructor
public class BankSyncService {

    private static final Logger log = LoggerFactory.getLogger(BankSyncService.class);

    private final BankApiClient bankApiClient;
    private final BankService bankService;
    private final TransactionService transactionService;
    private final BalanceService balanceService;
    private final AccountRepository accountRepository;
    private final SyncLogRepository syncLogRepository;

    /**
     * Реактивная версия полной синхронизации: счета + транзакции + балансы
     * Все операции выполняются параллельно для максимальной производительности
     */
    @Transactional
    @CacheEvict(value = "accounts", key = "'user:' + #userId")
    public reactor.core.publisher.Mono<SyncResult> syncAllReactive(UUID userId, BankCredentials credentials) {
        log.info("starting reactive full sync for user={}, bank={}", userId, credentials.bankId());

        long startTime = System.currentTimeMillis();

        // Получаем счета реактивно
        return bankApiClient.getAccountsReactive(userId, credentials)
                .flatMap(accountsResponse -> {
                    // После получения счетов, синхронизируем их и получаем список accountIds
                    int accountsSynced = syncAccountsFromResponse(userId, credentials.bankId(), accountsResponse);

                    if (accountsSynced == 0) {
                        log.warn("No accounts found for user={}, bank={}, skipping transactions and balances",
                                userId, credentials.bankId());
                        return reactor.core.publisher.Mono.just(
                                new SyncResult(true, accountsSynced, 0, 0,
                                        System.currentTimeMillis() - startTime, null)
                        );
                    }

                    // Получаем список счетов для параллельной обработки транзакций и балансов
                    List<UUID> accountIds = accountRepository.findActiveAccountsByUserAndBank(userId, credentials.bankId())
                            .stream()
                            .map(BankAccount::getId)
                            .toList();

                    if (accountIds.isEmpty()) {
                        return reactor.core.publisher.Mono.just(
                                new SyncResult(true, accountsSynced, 0, 0,
                                        System.currentTimeMillis() - startTime, null)
                        );
                    }

                    // Создаем Flux из accountIds и обрабатываем параллельно
                    reactor.core.publisher.Flux<SyncData> syncFlux = reactor.core.publisher.Flux.fromIterable(accountIds)
                            .flatMap(accountId -> {
                                reactor.core.publisher.Mono<ExternalTransactionResponseDto> transactionsMono =
                                        bankApiClient.getTransactionsReactive(userId, credentials, accountId)
                                                .onErrorResume(throwable -> {
                                                    log.warn("Failed to get transactions for account {}: {}", accountId, throwable.getMessage());
                                                    return reactor.core.publisher.Mono.just(
                                                            new ExternalTransactionResponseDto(
                                                                    new ExternalTransactionResponseDto.Data(List.of()),
                                                                    null, null
                                                            )
                                                    );
                                                });

                                reactor.core.publisher.Mono<ExternalBalanceResponseDto> balancesMono =
                                        bankApiClient.getBalancesReactive(userId, credentials, accountId)
                                                .onErrorResume(throwable -> {
                                                    log.warn("Failed to get balances for account {}: {}", accountId, throwable.getMessage());
                                                    return reactor.core.publisher.Mono.just(
                                                            new ExternalBalanceResponseDto(
                                                                    new ExternalBalanceResponseDto.Data(List.of()),
                                                                    null, null
                                                            )
                                                    );
                                                });

                                return reactor.core.publisher.Mono.zip(transactionsMono, balancesMono)
                                        .map(tuple -> new SyncData(accountId, tuple.getT1(), tuple.getT2()));
                            });

                    // Собираем все результаты и сохраняем
                    return syncFlux.collectList()
                            .map(syncDataList -> {
                                int totalTransactions = 0;
                                int totalBalances = 0;

                                for (SyncData data : syncDataList) {
                                    totalTransactions += syncTransactionsFromResponse(data.accountId(), data.transactions());
                                    totalBalances += syncBalancesFromResponse(data.accountId(), data.balances());
                                }

                                long duration = System.currentTimeMillis() - startTime;
                                SyncResult result = new SyncResult(true, accountsSynced, totalTransactions, totalBalances, duration, null);

                                logSyncOperation(userId, null, "FULL_SYNC_REACTIVE", "SUCCESS",
                                        String.format("Synced: %d accounts, %d transactions, %d balances",
                                                accountsSynced, totalTransactions, totalBalances),
                                        accountsSynced + totalTransactions + totalBalances, duration);

                                log.info("reactive full sync completed successfully for user={}, bank={}, duration={}ms",
                                        userId, credentials.bankId(), duration);

                                return result;
                            });
                })
                .onErrorResume(throwable -> {
                    long duration = System.currentTimeMillis() - startTime;
                    SyncResult result = new SyncResult(false, 0, 0, 0, duration, throwable.getMessage());

                    logSyncOperation(userId, null, "FULL_SYNC_REACTIVE", "FAILED",
                            "Error: " + throwable.getMessage(), 0, duration);

                    log.error("reactive full sync failed for user={}, bank={}: {}",
                            userId, credentials.bankId(), throwable.getMessage(), throwable);

                    return reactor.core.publisher.Mono.just(result);
                });
    }

    /**
     * Класс для хранения данных синхронизации одного счета
     */
    private static class SyncData {
        private final UUID accountId;
        private final ExternalTransactionResponseDto transactions;
        private final ExternalBalanceResponseDto balances;

        public SyncData(UUID accountId, ExternalTransactionResponseDto transactions, ExternalBalanceResponseDto balances) {
            this.accountId = accountId;
            this.transactions = transactions;
            this.balances = balances;
        }

        public UUID accountId() { return accountId; }
        public ExternalTransactionResponseDto transactions() { return transactions; }
        public ExternalBalanceResponseDto balances() { return balances; }
    }

    /**
     * Синхронизация счетов из реактивного ответа
     */
    private int syncAccountsFromResponse(UUID userId, String bankId, ExternalAccountResponseDto response) {
        if (response.accounts() == null || response.accounts().isEmpty()) {
            return 0;
        }

        int savedCount = 0;
        for (ExternalAccountResponseDto.ExternalAccountDto externalAccount : response.accounts()) {
            try {
                BankAccount account = mapToEntity(userId, bankId, externalAccount);
                bankService.saveAccount(account);
                savedCount++;
            } catch (Exception e) {
                log.error("Failed to save account {}: {}", externalAccount.accountId(), e.getMessage(), e);
            }
        }
        return savedCount;
    }

    /**
     * Синхронизация транзакций из реактивного ответа
     */
    private int syncTransactionsFromResponse(UUID accountId, ExternalTransactionResponseDto response) {
        if (response.transactions() == null || response.transactions().isEmpty()) {
            return 0;
        }

        int savedCount = 0;
        for (ExternalTransactionResponseDto.ExternalTransactionDto external : response.transactions()) {
            try {
                AccountTransaction transaction = mapToEntity(accountId, external);
                transactionService.saveTransaction(transaction);
                savedCount++;
            } catch (Exception e) {
                log.error("Failed to save transaction: {}", e.getMessage(), e);
            }
        }
        return savedCount;
    }

    /**
     * Синхронизация балансов из реактивного ответа
     */
    private int syncBalancesFromResponse(UUID accountId, ExternalBalanceResponseDto response) {
        if (response.balances() == null || response.balances().isEmpty()) {
            return 0;
        }

        int savedCount = 0;
        for (ExternalBalanceResponseDto.ExternalBalanceDto external : response.balances()) {
            try {
                AccountBalance balance = mapToEntity(accountId, external);
                balanceService.saveBalance(balance);
                savedCount++;
            } catch (Exception e) {
                log.error("Failed to save balance: {}", e.getMessage(), e);
            }
        }
        return savedCount;
    }

    /**
     * Полная синхронизация: счета + транзакции + балансы
     * Выполняется при добавлении нового банка или по требованию пользователя
     * Инвалидирует кеш счетов пользователя после синхронизации
     */
    @Transactional
    @CacheEvict(value = "accounts", key = "'user:' + #userId") // Очищаем кеш счетов пользователя
    public SyncResult syncAll(UUID userId, BankCredentials credentials, String forceConsentId) {
        log.info("starting full sync for user={}, bank={}", userId, credentials.bankId());
        
        long startTime = System.currentTimeMillis();
        SyncResult result = new SyncResult();
        
        try {
            // 1. Синхронизация счетов
            result.accountsSynced = syncAccounts(userId, credentials, forceConsentId);
            
            // 2. Синхронизация транзакций (для всех счетов)
            result.transactionsSynced = syncTransactions(userId, credentials);
            
            // 3. Синхронизация балансов
            result.balancesSynced = syncBalances(userId, credentials);
            
            long duration = System.currentTimeMillis() - startTime;
            result.success = true;
            result.durationMs = duration;
            
            // Логирование успеха
            logSyncOperation(userId, null, "FULL_SYNC", "SUCCESS",
                    String.format("Synced: %d accounts, %d transactions, %d balances",
                            result.accountsSynced, result.transactionsSynced, result.balancesSynced),
                    result.accountsSynced + result.transactionsSynced + result.balancesSynced,
                    duration);
            
            log.info("full sync completed successfully for user={}, bank={}, duration={}ms",
                    userId, credentials.bankId(), duration);
            
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            result.success = false;
            result.errorMessage = e.getMessage();
            result.durationMs = duration;
            
            // Логирование ошибки
            logSyncOperation(userId, null, "FULL_SYNC", "FAILED",
                    "Error: " + e.getMessage(), 0, duration);
            
            log.error("full sync failed for user={}, bank={}: {}", userId, credentials.bankId(), e.getMessage(), e);
            
            throw new SyncFailedException(e.getMessage(), e);
        }
    }

    /**
     * Синхронизация только счетов
     */
    @Transactional
    public int syncAccounts(UUID userId, BankCredentials credentials, String forceConsentId) {
        log.info("syncing accounts for user={}, bank={}, clientId={}, forceConsentId={}",
                userId, credentials.bankId(), credentials.clientId(), forceConsentId);

        long startTime = System.currentTimeMillis();

        try {
            ExternalAccountResponseDto response;
            if (forceConsentId != null && !forceConsentId.isBlank()) {
                response = bankApiClient.getAccounts(userId, credentials, forceConsentId);
            } else {
                response = bankApiClient.getAccounts(userId, credentials);
            }
            log.info("Received {} accounts from external API for user={}, bank={}",
                    response.accounts().size(), userId, credentials.bankId());

            int savedCount = 0;
            for (ExternalAccountResponseDto.ExternalAccountDto externalAccount : response.accounts()) {
                try {
                    log.debug("Processing account: externalId={}, currency={}",
                            externalAccount.accountId(), externalAccount.currency());

                    BankAccount account = mapToEntity(userId, credentials.bankId(), externalAccount);
                    bankService.saveAccount(account);
                    savedCount++;

                    log.debug("Saved account {}/{}: id={}, externalId={}",
                            savedCount, response.accounts().size(), account.getId(), account.getExternalAccountId());
                } catch (Exception e) {
                    log.error("Failed to save account {}: {}", externalAccount.accountId(), e.getMessage(), e);
                    // Продолжаем с другими счетами
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            
            logSyncOperation(userId, null, "ACCOUNTS", "SUCCESS",
                    "Synced " + savedCount + " accounts", savedCount, duration);
            
            log.info("synced {} accounts for user={}, bank={}", savedCount, userId, credentials.bankId());
            
            return savedCount;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logSyncOperation(userId, null, "ACCOUNTS", "FAILED",
                    "Error: " + e.getMessage(), 0, duration);
            throw e;
        }
    }

    /**
     * Синхронизация транзакций (за последние 6 месяцев)
     */
    @Transactional
    public int syncTransactions(UUID userId, BankCredentials credentials) {
        log.info("syncing transactions for user={}, bank={}", userId, credentials.bankId());
        
        // Получаем все счета пользователя для данного банка
        List<BankAccount> accounts = bankService.getAccountsByBank(userId, credentials.bankId())
                .stream()
                .map(dto -> {
                    // Создаем минимальный объект BankAccount для получения external_account_id
                    BankAccount acc = new BankAccount();
                    acc.setId(dto.id());
                    acc.setExternalAccountId(dto.externalAccountId());
                    return acc;
                })
                .toList();
        
        if (accounts.isEmpty()) {
            log.warn("no accounts found for user={}, bank={}", userId, credentials.bankId());
            return 0;
        }
        
        LocalDateTime from = LocalDateTime.now().minusMonths(6);
        LocalDateTime to = LocalDateTime.now();
        
        int totalTransactions = 0;
        long startTime = System.currentTimeMillis();
        
        try {
            for (BankAccount account : accounts) {
                ExternalTransactionResponseDto response = bankApiClient.getTransactions(
                        userId, credentials, account.getExternalAccountId(), from, to
                );
                
                for (ExternalTransactionResponseDto.ExternalTransactionDto externalTx : response.transactions()) {
                    AccountTransaction transaction = mapToEntity(account.getId(), externalTx);
                    transactionService.saveTransaction(transaction);
                    totalTransactions++;
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            
            logSyncOperation(userId, null, "TRANSACTIONS", "SUCCESS",
                    "Synced " + totalTransactions + " transactions", totalTransactions, duration);
            
            log.info("synced {} transactions for user={}, bank={}", totalTransactions, userId, credentials.bankId());
            
            return totalTransactions;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logSyncOperation(userId, null, "TRANSACTIONS", "FAILED",
                    "Error: " + e.getMessage(), totalTransactions, duration);
            throw e;
        }
    }

    /**
     * Синхронизация балансов
     */
    @Transactional
    public int syncBalances(UUID userId, BankCredentials credentials) {
        log.info("syncing balances for user={}, bank={}", userId, credentials.bankId());
        
        // Получаем все счета пользователя для данного банка
        List<BankAccount> accounts = bankService.getAccountsByBank(userId, credentials.bankId())
                .stream()
                .map(dto -> {
                    BankAccount acc = new BankAccount();
                    acc.setId(dto.id());
                    acc.setExternalAccountId(dto.externalAccountId());
                    return acc;
                })
                .toList();
        
        if (accounts.isEmpty()) {
            log.warn("no accounts found for user={}, bank={}", userId, credentials.bankId());
            return 0;
        }
        
        int totalBalances = 0;
        long startTime = System.currentTimeMillis();
        
        try {
            for (BankAccount account : accounts) {
                ExternalBalanceResponseDto response = bankApiClient.getBalances(
                        userId, credentials, account.getExternalAccountId()
                );
                
                for (ExternalBalanceResponseDto.ExternalBalanceDto externalBalance : response.balances()) {
                    AccountBalance balance = mapToEntity(account.getId(), externalBalance);
                    balanceService.saveBalance(balance);
                    totalBalances++;
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            
            logSyncOperation(userId, null, "BALANCES", "SUCCESS",
                    "Synced " + totalBalances + " balances", totalBalances, duration);
            
            log.info("synced {} balances for user={}, bank={}", totalBalances, userId, credentials.bankId());
            
            return totalBalances;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logSyncOperation(userId, null, "BALANCES", "FAILED",
                    "Error: " + e.getMessage(), totalBalances, duration);
            throw e;
        }
    }

    /**
     * Маппинг внешнего счета в сущность BankAccount
     */
    private BankAccount mapToEntity(UUID userId, String bankId, 
                                    ExternalAccountResponseDto.ExternalAccountDto external) {
        BankAccount account = new BankAccount();
        account.setUserId(userId);
        account.setBankId(bankId);
        account.setExternalAccountId(external.accountId());
        account.setAccountType(external.accountType());
        account.setCurrency(external.currency());
        account.setAccountNumberMasked(maskAccountNumber(external.accountNumber()));
        account.setIban(external.iban());
        account.setStatus("ACTIVE");
        
        // Балансы будут получены отдельно через balances endpoint
        // В accounts endpoint балансы могут отсутствовать в новом формате API
        
        account.setLastSyncAt(LocalDateTime.now());
        
        return account;
    }

    /**
     * Маппинг внешней транзакции в сущность AccountTransaction
     */
    private AccountTransaction mapToEntity(UUID accountId,
                                          ExternalTransactionResponseDto.ExternalTransactionDto external) {
        AccountTransaction transaction = new AccountTransaction();
        transaction.setAccountId(accountId);
        transaction.setExternalTransactionId(external.transactionId());
        transaction.setBookingDateTime(parseDateTime(external.bookingDateTime()));
        transaction.setValueDateTime(parseDateTime(external.valueDateTime()));
        transaction.setAmount(external.amountValue());
        transaction.setCurrency(external.amountCurrency() != null ? external.amountCurrency() : "RUB"); // Default to RUB if null
        transaction.setDebitCreditIndicator(external.debitCreditIndicator() != null ? external.debitCreditIndicator() :
                (external.amountValue() != null && external.amountValue().compareTo(BigDecimal.ZERO) < 0 ? "DEBIT" : "CREDIT"));
        transaction.setStatus(external.status());
        transaction.setDescription(external.description());
        transaction.setMerchantName(external.merchantName());
        transaction.setMerchantCategoryCode(external.merchantCategoryCode());
        transaction.setRunningBalance(external.runningBalance());
        
        return transaction;
    }

    /**
     * Маппинг внешнего баланса в сущность AccountBalance
     */
    private AccountBalance mapToEntity(UUID accountId,
                                      ExternalBalanceResponseDto.ExternalBalanceDto external) {
        AccountBalance balance = new AccountBalance();
        balance.setAccountId(accountId);
        balance.setBalanceType(external.balanceType() != null ? external.balanceType() : "AVAILABLE"); // Default to AVAILABLE
        balance.setAmount(external.amountValue());
        balance.setCurrency(external.amountCurrency() != null ? external.amountCurrency() : "RUB");
        balance.setCreditLine(external.creditLine());
        balance.setAsOfDateTime(parseDateTime(external.asOfDateTime()) != null ?
                parseDateTime(external.asOfDateTime()) : LocalDateTime.now());
        
        return balance;
    }

    /**
     * Маскирование номера счета (показываем только последние 4 цифры)
     */
    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "****";
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }

    /**
     * Логирование операции синхронизации
     */
    private void logSyncOperation(UUID userId, UUID accountId, String syncType,
                                 String status, String message, int recordsFetched, long durationMs) {
        SyncLog syncLog = new SyncLog();
        syncLog.setUserId(userId);
        syncLog.setAccountId(accountId);
        syncLog.setSyncType(syncType);
        syncLog.setStatus(status);
        syncLog.setMessage(message);
        syncLog.setRecordsFetched(recordsFetched);
        syncLog.setStartedAt(LocalDateTime.now().minusNanos(durationMs * 1_000_000));
        syncLog.setFinishedAt(LocalDateTime.now());
        syncLog.setDurationMs(durationMs);
        
        syncLogRepository.save(syncLog);
    }

    /**
     * Парсит строку даты-времени в LocalDateTime
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr, BankingConstants.ISO_FORMATTER);
        } catch (Exception e) {
            log.warn("Failed to parse date time: {}", dateTimeStr, e);
            return null;
        }
    }

    /**
     * Результат синхронизации
     */
    public static class SyncResult {
        public boolean success;
        public int accountsSynced;
        public int transactionsSynced;
        public int balancesSynced;
        public long durationMs;
        public String errorMessage;

        public SyncResult() {}

        public SyncResult(boolean success, int accountsSynced, int transactionsSynced,
                         int balancesSynced, long durationMs, String errorMessage) {
            this.success = success;
            this.accountsSynced = accountsSynced;
            this.transactionsSynced = transactionsSynced;
            this.balancesSynced = balancesSynced;
            this.durationMs = durationMs;
            this.errorMessage = errorMessage;
        }
    }
}
