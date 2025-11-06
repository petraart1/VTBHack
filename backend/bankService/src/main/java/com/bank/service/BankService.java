package com.bank.service;

import com.bank.config.BankingConstants;
import com.bank.dto.response.AccountDto;
import com.bank.dto.mapper.AccountMapper;
import com.bank.exception.AccountNotFoundException;
import com.bank.model.BankAccount;
import com.bank.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequiredArgsConstructor
public class BankService {

    private static final Logger log = LoggerFactory.getLogger(BankService.class);

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    /**
     * Получение всех активных счетов пользователя
     * Кеширование: TTL 5 минут, ключ: accounts:user:{userId}
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "accounts", key = "'user:' + #userId")
    public List<AccountDto> getAccounts(UUID userId) {
        log.info("Fetching accounts for user: {}", userId);

        List<BankAccount> accounts = accountRepository.findActiveAccountsByUserId(userId);

        log.info("Found {} accounts for user {}", accounts.size(), userId);

        List<AccountDto> result = accounts.stream()
                .map(accountMapper::toDto)
                .collect(Collectors.toList());

        log.info("Returning {} account DTOs for user {}", result.size(), userId);
        return result;
    }

    /**
     * Получение счетов пользователя по конкретному банку
     * Кеширование: TTL 5 минут, ключ: accounts:user_bank:{userId}:{bankId}
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "accounts", key = "'user_bank:' + #userId + ':' + #bankId")
    public List<AccountDto> getAccountsByBank(UUID userId, String bankId) {
        log.info("Fetching accounts for user: {}, bank: {}", userId, bankId);

        List<BankAccount> accounts = accountRepository.findActiveAccountsByUserAndBank(userId, bankId);
        log.info("Found {} accounts in database for user={}, bank={}", accounts.size(), userId, bankId);

        List<AccountDto> result = accounts.stream()
                .map(accountMapper::toDto)
                .collect(Collectors.toList());

        log.info("Returning {} account DTOs for user={}, bank={}", result.size(), userId, bankId);
        return result;
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
        log.info("Saving account: userId={}, externalId={}, bankId={}",
                account.getUserId(), account.getExternalAccountId(), account.getBankId());

        accountRepository.findByUserAndExternalAccountId(account.getUserId(), account.getExternalAccountId())
                .ifPresentOrElse(
                        existing -> log.info("Account already exists, skipping: {}", account.getExternalAccountId()),
                        () -> {
                            BankAccount saved = accountRepository.save(account);
                            log.info("Account saved successfully: id={}, externalId={}", saved.getId(), saved.getExternalAccountId());
                        }
                );
    }
}
