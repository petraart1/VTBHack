package com.bank.service;

import com.bank.dto.response.BalanceDto;
import com.bank.dto.mapper.BalanceMapper;
import com.bank.exception.AccountNotFoundException;
import com.bank.model.AccountBalance;
import com.bank.repository.AccountRepository;
import com.bank.repository.BalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Сервис для работы с балансами банковских счетов
 * Соответствует требованиям:
 * - Все методы с @Transactional
 * - Read-only для queries (оптимизация)
 * - Проверка прав доступа (пользователь видит только свои данные)
 * - Lowercase логирование
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BalanceService {

    private final BalanceRepository balanceRepository;
    private final AccountRepository accountRepository;
    private final BalanceMapper balanceMapper;

    /**
     * Получение последних балансов счета
     * Используется индекс: idx_balances_account_time
     */
    @Transactional(readOnly = true)
    public List<BalanceDto> getLatestBalances(UUID userId, UUID accountId) {
        log.info("fetching latest balances: user={}, account={}", userId, accountId);

        // проверка прав доступа - пользователь может видеть только свои счета
        accountRepository.findByUserAndId(userId, accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        String.format("account %s not found for user %s", accountId, userId)
                ));

        List<AccountBalance> balances = balanceRepository.findByAccountIdOrderByAsOfDesc(accountId);

        log.debug("found {} balance records for account {}", balances.size(), accountId);

        return balances.stream()
                .map(balanceMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Получение истории балансов за период
     * Используется partial индекс: idx_balances_recent (оптимизирован для 90 дней)
     */
    @Transactional(readOnly = true)
    public List<BalanceDto> getBalanceHistory(UUID userId, UUID accountId, LocalDateTime from, LocalDateTime to) {
        log.info("fetching balance history: user={}, account={}, from={}, to={}",
                userId, accountId, from, to);

        // проверка прав доступа
        accountRepository.findByUserAndId(userId, accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        String.format("account %s not found for user %s", accountId, userId)
                ));

        List<AccountBalance> balances = balanceRepository.findByAccountAndDateRange(accountId, from, to);

        log.debug("found {} balance records for period", balances.size());

        return balances.stream()
                .map(balanceMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Сохранение баланса (используется при синхронизации с Open Banking API)
     * Constraint: uc_balance_snapshot предотвращает дубликаты
     */
    @Transactional
    public void saveBalance(AccountBalance balance) {
        log.debug("saving balance: accountId={}, type={}, amount={}",
                balance.getAccountId(), balance.getBalanceType(), balance.getAmount());

        balanceRepository.save(balance);
    }
}

