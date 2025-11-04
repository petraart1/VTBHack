package com.bank.service;

import com.bank.config.BankCredentials;
import com.bank.dto.bank.ExternalAccountResponseDto;
import com.bank.dto.bank.ExternalBalanceResponseDto;
import com.bank.dto.bank.ExternalTransactionResponseDto;
import com.bank.model.AccountBalance;
import com.bank.model.AccountTransaction;
import com.bank.model.BankAccount;
import com.bank.model.SyncLog;
import com.bank.repository.SyncLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@Slf4j
@RequiredArgsConstructor
public class BankSyncService {

    private final BankApiClient bankApiClient;
    private final BankService bankService;
    private final TransactionService transactionService;
    private final BalanceService balanceService;
    private final SyncLogRepository syncLogRepository;

    /**
     * Полная синхронизация: счета + транзакции + балансы
     * Выполняется при добавлении нового банка или по требованию пользователя
     */
    @Transactional
    public SyncResult syncAll(UUID userId, BankCredentials credentials) {
        log.info("starting full sync for user={}, bank={}", userId, credentials.bankId());
        
        long startTime = System.currentTimeMillis();
        SyncResult result = new SyncResult();
        
        try {
            // 1. Синхронизация счетов
            result.accountsSynced = syncAccounts(userId, credentials);
            
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
            
            throw new RuntimeException("Sync failed: " + e.getMessage(), e);
        }
    }

    /**
     * Синхронизация только счетов
     */
    @Transactional
    public int syncAccounts(UUID userId, BankCredentials credentials) {
        log.info("syncing accounts for user={}, bank={}", userId, credentials.bankId());
        
        long startTime = System.currentTimeMillis();
        
        try {
            ExternalAccountResponseDto response = bankApiClient.getAccounts(userId, credentials);
            
            int savedCount = 0;
            for (ExternalAccountResponseDto.ExternalAccountDto externalAccount : response.accounts()) {
                BankAccount account = mapToEntity(userId, credentials.bankId(), externalAccount);
                bankService.saveAccount(account);
                savedCount++;
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
        
        // Извлекаем балансы из ответа
        if (external.balances() != null && !external.balances().isEmpty()) {
            for (ExternalAccountResponseDto.BalanceDto balanceDto : external.balances()) {
                if ("AVAILABLE".equals(balanceDto.balanceType())) {
                    account.setAvailableBalance(balanceDto.amount());
                } else if ("INTERIM_BOOKED".equals(balanceDto.balanceType())) {
                    account.setBookedBalance(balanceDto.amount());
                }
                if (balanceDto.creditLine() != null) {
                    account.setCreditLimit(balanceDto.creditLine());
                }
            }
        }
        
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
        transaction.setBookingDateTime(external.bookingDateTime());
        transaction.setValueDateTime(external.valueDateTime());
        transaction.setAmount(external.amount());
        transaction.setCurrency(external.currency());
        transaction.setDebitCreditIndicator(external.debitCreditIndicator());
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
        balance.setBalanceType(external.balanceType());
        balance.setAmount(external.amount());
        balance.setCurrency(external.currency());
        balance.setCreditLine(external.creditLine());
        balance.setAsOfDateTime(external.asOfDateTime() != null ? external.asOfDateTime() : LocalDateTime.now());
        
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
     * Результат синхронизации
     */
    public static class SyncResult {
        public boolean success;
        public int accountsSynced;
        public int transactionsSynced;
        public int balancesSynced;
        public long durationMs;
        public String errorMessage;
    }
}
