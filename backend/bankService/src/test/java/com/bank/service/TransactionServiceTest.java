package com.bank.service;

import com.bank.config.BankingConstants;
import com.bank.dto.common.PageDto;
import com.bank.dto.mapper.TransactionMapper;
import com.bank.dto.request.TransactionFilterRequest;
import com.bank.dto.response.TransactionDto;
import com.bank.exception.AccountNotFoundException;
import com.bank.exception.TransactionNotFoundException;
import com.bank.model.AccountTransaction;
import com.bank.repository.AccountRepository;
import com.bank.repository.AccountTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountTransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionService transactionService;

    private UUID userId;
    private UUID accountId;
    private UUID transactionId;
    private AccountTransaction transaction1;
    private AccountTransaction transaction2;
    private TransactionDto transactionDto1;
    private TransactionDto transactionDto2;
    private TransactionFilterRequest filter;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        transactionId = UUID.randomUUID();

        LocalDateTime now = LocalDateTime.now();

        transaction1 = AccountTransaction.builder()
                .id(transactionId)
                .accountId(accountId)
                .externalTransactionId("ext-tx-123")
                .bookingDateTime(now.minusDays(1))
                .valueDateTime(now.minusDays(1))
                .amount(BigDecimal.valueOf(-1000.00))
                .currency("RUB")
                .debitCreditIndicator("DEBIT")
                .status("BOOKED")
                .description("Оплата в магазине")
                .merchantName("Магнит")
                .merchantCategoryCode("5411")
                .runningBalance(BigDecimal.valueOf(9000.00))
                .createdAt(now.minusDays(1))
                .build();

        transaction2 = AccountTransaction.builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .externalTransactionId("ext-tx-456")
                .bookingDateTime(now.minusDays(2))
                .valueDateTime(now.minusDays(2))
                .amount(BigDecimal.valueOf(5000.00))
                .currency("RUB")
                .debitCreditIndicator("CREDIT")
                .status("BOOKED")
                .description("Зарплата")
                .merchantName("Работодатель")
                .merchantCategoryCode("0000")
                .runningBalance(BigDecimal.valueOf(10000.00))
                .createdAt(now.minusDays(2))
                .build();

        transactionDto1 = new TransactionDto(
                transaction1.getId(), transaction1.getAccountId(), transaction1.getExternalTransactionId(),
                transaction1.getBookingDateTime(), transaction1.getValueDateTime(), transaction1.getAmount(),
                transaction1.getCurrency(), transaction1.getDebitCreditIndicator(), transaction1.getStatus(),
                transaction1.getDescription(), transaction1.getMerchantName(), transaction1.getMerchantCategoryCode(),
                transaction1.getRunningBalance(), transaction1.getCreatedAt()
        );

        transactionDto2 = new TransactionDto(
                transaction2.getId(), transaction2.getAccountId(), transaction2.getExternalTransactionId(),
                transaction2.getBookingDateTime(), transaction2.getValueDateTime(), transaction2.getAmount(),
                transaction2.getCurrency(), transaction2.getDebitCreditIndicator(), transaction2.getStatus(),
                transaction2.getDescription(), transaction2.getMerchantName(), transaction2.getMerchantCategoryCode(),
                transaction2.getRunningBalance(), transaction2.getCreatedAt()
        );

        filter = new TransactionFilterRequest(
                now.minusMonths(1), now, null, null, null, 0, 50
        );
    }

    @Test
    void listTransactions_ShouldReturnPagedTransactions_WhenNoFiltersApplied() {
        // Given
        Pageable pageable = PageRequest.of(0, 50);
        Page<AccountTransaction> page = new PageImpl<>(List.of(transaction1, transaction2), pageable, 2);

        when(accountRepository.findByUserAndId(userId, accountId))
                .thenReturn(Optional.of(mock(com.bank.model.BankAccount.class)));
        when(transactionRepository.findByAccountAndDateRange(eq(accountId), any(), any(), eq(pageable)))
                .thenReturn(page);
        when(transactionMapper.toDto(transaction1)).thenReturn(transactionDto1);
        when(transactionMapper.toDto(transaction2)).thenReturn(transactionDto2);

        // When
        PageDto<TransactionDto> result = transactionService.listTransactions(userId, accountId, filter);

        // Then
        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0)).isEqualTo(transactionDto1);
        assertThat(result.content().get(1)).isEqualTo(transactionDto2);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.pageNumber()).isEqualTo(0);
        assertThat(result.pageSize()).isEqualTo(50);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();

        verify(accountRepository).findByUserAndId(userId, accountId);
        verify(transactionRepository).findByAccountAndDateRange(eq(accountId), any(), any(), eq(pageable));
    }

    @Test
    void listTransactions_ShouldApplyMerchantNameFilter() {
        // Given
        TransactionFilterRequest merchantFilter = new TransactionFilterRequest(
                null, null, "Магнит", null, null, 0, 20
        );
        Pageable pageable = PageRequest.of(0, 20);
        Page<AccountTransaction> page = new PageImpl<>(List.of(transaction1), pageable, 1);

        when(accountRepository.findByUserAndId(userId, accountId))
                .thenReturn(Optional.of(mock(com.bank.model.BankAccount.class)));
        when(transactionRepository.findByAccountAndDateRangeAndMerchant(
                eq(accountId), any(), any(), eq("Магнит"), eq(pageable)))
                .thenReturn(page);
        when(transactionMapper.toDto(transaction1)).thenReturn(transactionDto1);

        // When
        PageDto<TransactionDto> result = transactionService.listTransactions(userId, accountId, merchantFilter);

        // Then
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0)).isEqualTo(transactionDto1);

        verify(transactionRepository).findByAccountAndDateRangeAndMerchant(
                eq(accountId), any(), any(), eq("Магнит"), eq(pageable));
    }

    @Test
    void listTransactions_ShouldApplyDebitCreditFilter() {
        // Given
        TransactionFilterRequest debitFilter = new TransactionFilterRequest(
                null, null, null, null, "DEBIT", 0, 30
        );
        Pageable pageable = PageRequest.of(0, 30);
        Page<AccountTransaction> page = new PageImpl<>(List.of(transaction1), pageable, 1);

        when(accountRepository.findByUserAndId(userId, accountId))
                .thenReturn(Optional.of(mock(com.bank.model.BankAccount.class)));
        when(transactionRepository.findByAccountAndDateRangeAndDebitCredit(
                eq(accountId), any(), any(), eq("DEBIT"), eq(pageable)))
                .thenReturn(page);
        when(transactionMapper.toDto(transaction1)).thenReturn(transactionDto1);

        // When
        PageDto<TransactionDto> result = transactionService.listTransactions(userId, accountId, debitFilter);

        // Then
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0)).isEqualTo(transactionDto1);

        verify(transactionRepository).findByAccountAndDateRangeAndDebitCredit(
                eq(accountId), any(), any(), eq("DEBIT"), eq(pageable));
    }

    @Test
    void listTransactions_ShouldApplyCategoryFilter() {
        // Given
        TransactionFilterRequest categoryFilter = new TransactionFilterRequest(
                null, null, null, "5411", null, 1, 25
        );
        Pageable pageable = PageRequest.of(1, 25);
        Page<AccountTransaction> page = new PageImpl<>(List.of(transaction1), pageable, 26);

        when(accountRepository.findByUserAndId(userId, accountId))
                .thenReturn(Optional.of(mock(com.bank.model.BankAccount.class)));
        when(transactionRepository.findByAccountAndCategoryAndDateRange(
                eq(accountId), eq("5411"), any(), any(), eq(pageable)))
                .thenReturn(page);
        when(transactionMapper.toDto(transaction1)).thenReturn(transactionDto1);

        // When
        PageDto<TransactionDto> result = transactionService.listTransactions(userId, accountId, categoryFilter);

        // Then
        assertThat(result.content()).hasSize(1);
        assertThat(result.pageNumber()).isEqualTo(1);
        assertThat(result.pageSize()).isEqualTo(25);
        assertThat(result.totalElements()).isEqualTo(26);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isTrue();
    }

    @Test
    void listTransactions_ShouldThrowAccountNotFoundException_WhenAccountDoesNotExist() {
        // Given
        when(accountRepository.findByUserAndId(userId, accountId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> transactionService.listTransactions(userId, accountId, filter))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("account " + accountId + " not found for user " + userId);

        verify(accountRepository).findByUserAndId(userId, accountId);
        verify(transactionRepository, never()).findByAccountAndDateRange(any(), any(), any(), any());
    }

    @Test
    void listTransactions_ShouldRespectMaxPageSize() {
        // Given
        TransactionFilterRequest largeSizeFilter = new TransactionFilterRequest(
                null, null, null, null, null, 0, 1000
        );
        Pageable pageable = PageRequest.of(0, BankingConstants.MAX_PAGE_SIZE);
        Page<AccountTransaction> page = new PageImpl<>(List.of(), pageable, 0);

        when(accountRepository.findByUserAndId(userId, accountId))
                .thenReturn(Optional.of(mock(com.bank.model.BankAccount.class)));
        when(transactionRepository.findByAccountAndDateRange(eq(accountId), any(), any(), eq(pageable)))
                .thenReturn(page);

        // When
        transactionService.listTransactions(userId, accountId, largeSizeFilter);

        // Then
        verify(transactionRepository).findByAccountAndDateRange(
                eq(accountId), any(), any(), eq(pageable));
        assertThat(pageable.getPageSize()).isEqualTo(BankingConstants.MAX_PAGE_SIZE);
    }

    @Test
    void getTransactionDetails_ShouldReturnTransaction_WhenExistsAndBelongsToAccount() {
        // Given
        when(accountRepository.findByUserAndId(userId, accountId))
                .thenReturn(Optional.of(mock(com.bank.model.BankAccount.class)));
        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.of(transaction1));
        when(transactionMapper.toDto(transaction1)).thenReturn(transactionDto1);

        // When
        TransactionDto result = transactionService.getTransactionDetails(userId, accountId, transactionId);

        // Then
        assertThat(result).isEqualTo(transactionDto1);

        verify(accountRepository).findByUserAndId(userId, accountId);
        verify(transactionRepository).findById(transactionId);
        verify(transactionMapper).toDto(transaction1);
    }

    @Test
    void getTransactionDetails_ShouldThrowAccountNotFoundException_WhenAccountDoesNotExist() {
        // Given
        when(accountRepository.findByUserAndId(userId, accountId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> transactionService.getTransactionDetails(userId, accountId, transactionId))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("account " + accountId + " not found for user " + userId);

        verify(accountRepository).findByUserAndId(userId, accountId);
        verify(transactionRepository, never()).findById(any());
    }

    @Test
    void getTransactionDetails_ShouldThrowTransactionNotFoundException_WhenTransactionDoesNotExist() {
        // Given
        when(accountRepository.findByUserAndId(userId, accountId))
                .thenReturn(Optional.of(mock(com.bank.model.BankAccount.class)));
        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> transactionService.getTransactionDetails(userId, accountId, transactionId))
                .isInstanceOf(TransactionNotFoundException.class)
                .hasMessageContaining("Transaction " + transactionId + " not found");

        verify(accountRepository).findByUserAndId(userId, accountId);
        verify(transactionRepository).findById(transactionId);
    }

    @Test
    void getTransactionDetails_ShouldThrowIllegalArgumentException_WhenTransactionDoesNotBelongToAccount() {
        // Given
        UUID wrongAccountId = UUID.randomUUID();
        AccountTransaction transactionFromWrongAccount = AccountTransaction.builder()
                .id(transactionId)
                .accountId(wrongAccountId) // Different account
                .build();

        when(accountRepository.findByUserAndId(userId, accountId))
                .thenReturn(Optional.of(mock(com.bank.model.BankAccount.class)));
        when(transactionRepository.findById(transactionId))
                .thenReturn(Optional.of(transactionFromWrongAccount));

        // When & Then
        assertThatThrownBy(() -> transactionService.getTransactionDetails(userId, accountId, transactionId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Transaction " + transactionId + " does not belong to account " + accountId);

        verify(accountRepository).findByUserAndId(userId, accountId);
        verify(transactionRepository).findById(transactionId);
        verify(transactionMapper, never()).toDto(any());
    }

    @Test
    void saveTransaction_ShouldSaveNewTransaction_WhenDoesNotExist() {
        // Given
        AccountTransaction newTransaction = AccountTransaction.builder()
                .accountId(accountId)
                .externalTransactionId("new-ext-tx-789")
                .build();

        when(transactionRepository.findByAccountAndExternalId(accountId, "new-ext-tx-789"))
                .thenReturn(Optional.empty());

        // When
        transactionService.saveTransaction(newTransaction);

        // Then
        verify(transactionRepository).findByAccountAndExternalId(accountId, "new-ext-tx-789");
        verify(transactionRepository).save(newTransaction);
    }

    @Test
    void saveTransaction_ShouldSkipSaving_WhenTransactionAlreadyExists() {
        // Given
        AccountTransaction existingTransaction = AccountTransaction.builder()
                .accountId(accountId)
                .externalTransactionId("existing-ext-tx-999")
                .build();

        when(transactionRepository.findByAccountAndExternalId(accountId, "existing-ext-tx-999"))
                .thenReturn(Optional.of(transaction1));

        // When
        transactionService.saveTransaction(existingTransaction);

        // Then
        verify(transactionRepository).findByAccountAndExternalId(accountId, "existing-ext-tx-999");
        verify(transactionRepository, never()).save(any());
    }
}
