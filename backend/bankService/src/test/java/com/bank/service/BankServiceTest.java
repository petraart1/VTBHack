package com.bank.service;

import com.bank.dto.mapper.AccountMapper;
import com.bank.dto.response.AccountDto;
import com.bank.exception.AccountNotFoundException;
import com.bank.model.BankAccount;
import com.bank.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private BankService bankService;

    private UUID userId;
    private UUID accountId;
    private BankAccount account1;
    private BankAccount account2;
    private AccountDto accountDto1;
    private AccountDto accountDto2;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();

        LocalDateTime now = LocalDateTime.now();

        account1 = BankAccount.builder()
                .id(accountId)
                .userId(userId)
                .bankId("vbank")
                .externalAccountId("acc-12345")
                .accountNumberMasked("****1234")
                .iban("RU12345678901234567890")
                .accountType("Personal")
                .currency("RUB")
                .nickname("Основной счет")
                .availableBalance(BigDecimal.valueOf(10000.00))
                .bookedBalance(BigDecimal.valueOf(9500.00))
                .creditLimit(BigDecimal.ZERO)
                .status("ACTIVE")
                .lastSyncAt(now.minusHours(1))
                .createdAt(now.minusDays(30))
                .build();

        account2 = BankAccount.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .bankId("vbank")
                .externalAccountId("acc-67890")
                .accountNumberMasked("****5678")
                .iban("RU09876543210987654321")
                .accountType("Savings")
                .currency("RUB")
                .nickname("Накопительный")
                .availableBalance(BigDecimal.valueOf(50000.00))
                .bookedBalance(BigDecimal.valueOf(50000.00))
                .creditLimit(BigDecimal.ZERO)
                .status("ACTIVE")
                .lastSyncAt(now.minusHours(2))
                .createdAt(now.minusDays(15))
                .build();

        accountDto1 = new AccountDto(
                account1.getId(), account1.getBankId(), account1.getExternalAccountId(),
                account1.getAccountNumberMasked(), account1.getIban(), account1.getAccountType(),
                account1.getCurrency(), account1.getNickname(), account1.getAvailableBalance(),
                account1.getBookedBalance(), account1.getCreditLimit(), account1.getStatus(),
                account1.getLastSyncAt(), account1.getCreatedAt()
        );

        accountDto2 = new AccountDto(
                account2.getId(), account2.getBankId(), account2.getExternalAccountId(),
                account2.getAccountNumberMasked(), account2.getIban(), account2.getAccountType(),
                account2.getCurrency(), account2.getNickname(), account2.getAvailableBalance(),
                account2.getBookedBalance(), account2.getCreditLimit(), account2.getStatus(),
                account2.getLastSyncAt(), account2.getCreatedAt()
        );
    }

    @Test
    void getAccounts_ShouldReturnMappedAccounts() {
        // Given
        when(accountRepository.findActiveAccountsByUserId(userId))
                .thenReturn(List.of(account1, account2));
        when(accountMapper.toDto(account1)).thenReturn(accountDto1);
        when(accountMapper.toDto(account2)).thenReturn(accountDto2);

        // When
        List<AccountDto> result = bankService.getAccounts(userId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(accountDto1);
        assertThat(result.get(1)).isEqualTo(accountDto2);

        verify(accountRepository).findActiveAccountsByUserId(userId);
        verify(accountMapper, times(2)).toDto(any(BankAccount.class));
    }

    @Test
    void getAccounts_ShouldReturnEmptyList_WhenNoAccountsFound() {
        // Given
        when(accountRepository.findActiveAccountsByUserId(userId))
                .thenReturn(List.of());

        // When
        List<AccountDto> result = bankService.getAccounts(userId);

        // Then
        assertThat(result).isEmpty();

        verify(accountRepository).findActiveAccountsByUserId(userId);
        verify(accountMapper, never()).toDto(any());
    }

    @Test
    void getAccountsByBank_ShouldReturnFilteredAccounts() {
        // Given
        String bankId = "vbank";
        when(accountRepository.findActiveAccountsByUserAndBank(userId, bankId))
                .thenReturn(List.of(account1, account2));
        when(accountMapper.toDto(account1)).thenReturn(accountDto1);
        when(accountMapper.toDto(account2)).thenReturn(accountDto2);

        // When
        List<AccountDto> result = bankService.getAccountsByBank(userId, bankId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(accountDto1);
        assertThat(result.get(1)).isEqualTo(accountDto2);

        verify(accountRepository).findActiveAccountsByUserAndBank(userId, bankId);
        verify(accountMapper, times(2)).toDto(any(BankAccount.class));
    }

    @Test
    void getAccountsByBank_ShouldReturnEmptyList_WhenNoAccountsForBank() {
        // Given
        String bankId = "unknown-bank";
        when(accountRepository.findActiveAccountsByUserAndBank(userId, bankId))
                .thenReturn(List.of());

        // When
        List<AccountDto> result = bankService.getAccountsByBank(userId, bankId);

        // Then
        assertThat(result).isEmpty();

        verify(accountRepository).findActiveAccountsByUserAndBank(userId, bankId);
        verify(accountMapper, never()).toDto(any());
    }

    @Test
    void getAccountDetails_ShouldReturnMappedAccount_WhenAccountExists() {
        // Given
        when(accountRepository.findByUserAndId(userId, accountId))
                .thenReturn(Optional.of(account1));
        when(accountMapper.toDto(account1)).thenReturn(accountDto1);

        // When
        AccountDto result = bankService.getAccountDetails(userId, accountId);

        // Then
        assertThat(result).isEqualTo(accountDto1);

        verify(accountRepository).findByUserAndId(userId, accountId);
        verify(accountMapper).toDto(account1);
    }

    @Test
    void getAccountDetails_ShouldThrowAccountNotFoundException_WhenAccountDoesNotExist() {
        // Given
        when(accountRepository.findByUserAndId(userId, accountId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bankService.getAccountDetails(userId, accountId))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("Account " + accountId + " not found for user " + userId);

        verify(accountRepository).findByUserAndId(userId, accountId);
        verify(accountMapper, never()).toDto(any());
    }

    @Test
    void saveAccount_ShouldSaveNewAccount_WhenAccountDoesNotExist() {
        // Given
        BankAccount newAccount = BankAccount.builder()
                .userId(userId)
                .externalAccountId("new-acc-123")
                .bankId("vbank")
                .accountNumberMasked("****1234")
                .currency("RUB")
                .status("ACTIVE")
                .build();

        when(accountRepository.findByUserAndExternalAccountId(userId, "new-acc-123"))
                .thenReturn(Optional.empty());
        when(accountRepository.save(newAccount)).thenReturn(newAccount);

        // When
        bankService.saveAccount(newAccount);

        // Then
        verify(accountRepository).findByUserAndExternalAccountId(userId, "new-acc-123");
        verify(accountRepository).save(newAccount);
    }

    @Test
    void saveAccount_ShouldSkipSaving_WhenAccountAlreadyExists() {
        // Given
        BankAccount existingAccount = BankAccount.builder()
                .userId(userId)
                .externalAccountId("existing-acc-456")
                .bankId("vbank")
                .accountNumberMasked("****4567")
                .currency("RUB")
                .status("ACTIVE")
                .build();

        when(accountRepository.findByUserAndExternalAccountId(userId, "existing-acc-456"))
                .thenReturn(Optional.of(account1));

        // When
        bankService.saveAccount(existingAccount);

        // Then
        verify(accountRepository).findByUserAndExternalAccountId(userId, "existing-acc-456");
        verify(accountRepository, never()).save(any());
    }
}
