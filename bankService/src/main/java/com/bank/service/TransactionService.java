package com.bank.service;

import com.bank.dto.PageDto;
import com.bank.dto.TransactionDto;
import com.bank.dto.TransactionFilterRequest;
import com.bank.dto.TransactionMapper;
import com.bank.exception.AccountNotFoundException;
import com.bank.exception.TransactionNotFoundException;
import com.bank.model.AccountTransaction;
import com.bank.repository.AccountRepository;
import com.bank.repository.AccountTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {

    private final AccountTransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionMapper transactionMapper;
    private static final int MAX_PAGE_SIZE = 500;

    @Transactional(readOnly = true)
    public PageDto<TransactionDto> listTransactions(UUID userId, UUID accountId, TransactionFilterRequest filter) {
        log.info("fetching transactions: user={}, account={}", userId, accountId);

        // проверяем, что счет принадлежит пользователю
        accountRepository.findByUserAndId(userId, accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        String.format("account %s not found for user %s", accountId, userId)
                ));

        int pageSize = Math.min(filter.size(), MAX_PAGE_SIZE);
        int pageNumber = Math.max(filter.page(), 0);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        LocalDateTime fromDate = filter.fromBookingDateTime() != null
                ? filter.fromBookingDateTime()
                : LocalDateTime.now().minusMonths(6);
        LocalDateTime toDate = filter.toBookingDateTime() != null
                ? filter.toBookingDateTime()
                : LocalDateTime.now();

        Page<AccountTransaction> result;

        if (filter.merchantName() != null && !filter.merchantName().isEmpty()) {
            result = transactionRepository.findByAccountAndDateRangeAndMerchant(
                    accountId, fromDate, toDate, filter.merchantName(), pageable
            );
        } else if (filter.debitCreditIndicator() != null && !filter.debitCreditIndicator().isEmpty()) {
            result = transactionRepository.findByAccountAndDateRangeAndDebitCredit(
                    accountId, fromDate, toDate, filter.debitCreditIndicator(), pageable
            );
        } else if (filter.merchantCategoryCode() != null && !filter.merchantCategoryCode().isEmpty()) {
            result = transactionRepository.findByAccountAndCategoryAndDateRange(
                    accountId, filter.merchantCategoryCode(), fromDate, toDate, pageable
            );
        } else {
            result = transactionRepository.findByAccountAndDateRange(
                    accountId, fromDate, toDate, pageable
            );
        }

        return new PageDto<>(
                result.getContent().stream()
                        .map(transactionMapper::toDto)
                        .collect(Collectors.toList()),
                result.getTotalElements(),
                result.getNumber(),
                result.getSize(),
                result.getTotalPages(),
                result.hasNext(),
                result.hasPrevious()
        );
    }

    @Transactional(readOnly = true)
    public TransactionDto getTransactionDetails(UUID userId, UUID accountId, UUID transactionId) {
        log.info("fetching transaction details: user={}, account={}, transaction={}",
                userId, accountId, transactionId);

        accountRepository.findByUserAndId(userId, accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        String.format("account %s not found for user %s", accountId, userId)
                ));

        AccountTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(
                        String.format("Transaction %s not found", transactionId)
                ));

        if (!transaction.getAccountId().equals(accountId)) {
            throw new IllegalArgumentException(
                    String.format("Transaction %s does not belong to account %s", transactionId, accountId)
            );
        }

        return transactionMapper.toDto(transaction);
    }

    @Transactional
    public void saveTransaction(AccountTransaction transaction) {
        log.debug("saving transaction: externalId={}", transaction.getExternalTransactionId());

        UUID accountId = transaction.getAccountId();
        String externalId = transaction.getExternalTransactionId();

        transactionRepository.findByAccountAndExternalId(accountId, externalId)
                .ifPresentOrElse(
                        existing -> log.debug("transaction already exists, skipping: {}", externalId),
                        () -> transactionRepository.save(transaction)
                );
    }
}

