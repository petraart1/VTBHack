package com.bank.service;

import com.bank.dto.AccountDto;
import com.bank.dto.AccountMapper;
import com.bank.exception.AccountNotFoundException;
import com.bank.model.BankAccount;
import com.bank.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Сервис для работы с банковскими счетами
 * Соответствует требованиям SOLID и документации проекта
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BankService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 500;

    /**
     * Получение всех активных счетов пользователя
     * Кеширование: TTL 5 минут
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "accounts", key = "#userId")
    public List<AccountDto> getAccounts(UUID userId) {
        log.info("Fetching accounts for user: {}", userId);

        List<BankAccount> accounts = accountRepository.findActiveAccountsByUserId(userId);

        log.debug("Found {} accounts for user {}", accounts.size(), userId);

        return accounts.stream()
                .map(accountMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Получение счетов пользователя по конкретному банку
     * Кеширование: TTL 5 минут
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "accounts", key = "#userId + '_' + #bankId")
    public List<AccountDto> getAccountsByBank(UUID userId, String bankId) {
        log.info("Fetching accounts for user: {}, bank: {}", userId, bankId);

        List<BankAccount> accounts = accountRepository.findActiveAccountsByUserAndBank(userId, bankId);

        return accounts.stream()
                .map(accountMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Получение деталей конкретного счета
     * Проверка прав доступа: пользователь может видеть только свои счета
     */
    @Transactional(readOnly = true)
    public AccountDto getAccountDetails(UUID userId, UUID accountId) {
        log.info("Fetching account details: user={}, account={}", userId, accountId);

        BankAccount account = accountRepository.findByUserAndId(userId, accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        String.format("Account %s not found for user %s", accountId, userId)
                ));

        return accountMapper.toDto(account);
    }

    /**
     * Сохранение счета (используется при синхронизации с Open Banking API)
     * Предотвращение дубликатов через unique constraint
     */
    @Transactional
    public void saveAccount(BankAccount account) {
        log.debug("Saving account: externalId={}", account.getExternalAccountId());

        accountRepository.findByUserAndExternalAccountId(account.getUserId(), account.getExternalAccountId())
                .ifPresentOrElse(
                        existing -> log.debug("Account already exists, skipping: {}", account.getExternalAccountId()),
                        () -> accountRepository.save(account)
                );
    }
}
