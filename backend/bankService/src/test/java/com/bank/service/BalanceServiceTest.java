package com.bank.service;

import com.bank.dto.mapper.BalanceMapper;
import com.bank.dto.response.BalanceDto;
import com.bank.exception.AccountNotFoundException;
import com.bank.model.AccountBalance;
import com.bank.repository.AccountRepository;
import com.bank.repository.BalanceRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

    @Mock
    private BalanceRepository balanceRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private BalanceMapper balanceMapper;

    @InjectMocks
    private BalanceService balanceService;

    private UUID userId;
    private UUID accountId;
    private AccountBalance balance1;
    private AccountBalance balance2;
    private BalanceDto balanceDto1;
    private BalanceDto balanceDto2;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();

        balance1 = AccountBalance.builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .balanceType("AVAILABLE")
                .amount(BigDecimal.valueOf(1000.00))
                .currency("RUB")
                .creditLine(BigDecimal.ZERO)
                .asOfDateTime(LocalDateTime.now().minusDays(1))
                .build();

        balance2 = AccountBalance.builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .balanceType("INTERIM_BOOKED")
                .amount(BigDecimal.valueOf(950.00))
                .currency("RUB")
                .creditLine(BigDecimal.ZERO)
                .asOfDateTime(LocalDateTime.now().minusDays(2))
                .build();

        balanceDto1 = new BalanceDto("AVAILABLE", BigDecimal.valueOf(1000.00), "RUB",
                BigDecimal.ZERO, balance1.getAsOfDateTime());
        balanceDto2 = new BalanceDto("INTERIM_BOOKED", BigDecimal.valueOf(950.00), "RUB",
                BigDecimal.ZERO, balance2.getAsOfDateTime());
    }

    @Test
    void getLatestBalances_ShouldReturnMappedBalances_WhenAccountExists() {
        // Given
        when(accountRepository.findByUserAndId(userId, accountId))
                .thenReturn(Optional.of(mock(com.bank.model.BankAccount.class)));
        when(balanceRepository.findByAccountIdOrderByAsOfDesc(accountId))
                .thenReturn(List.of(balance1, balance2));
        when(balanceMapper.toDto(balance1)).thenReturn(balanceDto1);
        when(balanceMapper.toDto(balance2)).thenReturn(balanceDto2);

        // When
        List<BalanceDto> result = balanceService.getLatestBalances(userId, accountId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(balanceDto1);
        assertThat(result.get(1)).isEqualTo(balanceDto2);

        verify(accountRepository).findByUserAndId(userId, accountId);
        verify(balanceRepository).findByAccountIdOrderByAsOfDesc(accountId);
        verify(balanceMapper, times(2)).toDto(any(AccountBalance.class));
    }

    @Test
    void getLatestBalances_ShouldThrowAccountNotFoundException_WhenAccountDoesNotExist() {
        // Given
        when(accountRepository.findByUserAndId(userId, accountId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> balanceService.getLatestBalances(userId, accountId))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("account " + accountId + " not found for user " + userId);

        verify(accountRepository).findByUserAndId(userId, accountId);
        verify(balanceRepository, never()).findByAccountIdOrderByAsOfDesc(any());
        verify(balanceMapper, never()).toDto(any());
    }

    @Test
    void getBalanceHistory_ShouldReturnMappedBalances_WhenAccountExistsAndDateRangeProvided() {
        // Given
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();

        when(accountRepository.findByUserAndId(userId, accountId))
                .thenReturn(Optional.of(mock(com.bank.model.BankAccount.class)));
        when(balanceRepository.findByAccountAndDateRange(accountId, from, to))
                .thenReturn(List.of(balance1, balance2));
        when(balanceMapper.toDto(balance1)).thenReturn(balanceDto1);
        when(balanceMapper.toDto(balance2)).thenReturn(balanceDto2);

        // When
        List<BalanceDto> result = balanceService.getBalanceHistory(userId, accountId, from, to);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(balanceDto1);
        assertThat(result.get(1)).isEqualTo(balanceDto2);

        verify(accountRepository).findByUserAndId(userId, accountId);
        verify(balanceRepository).findByAccountAndDateRange(accountId, from, to);
        verify(balanceMapper, times(2)).toDto(any(AccountBalance.class));
    }

    @Test
    void getBalanceHistory_ShouldThrowAccountNotFoundException_WhenAccountDoesNotExist() {
        // Given
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();

        when(accountRepository.findByUserAndId(userId, accountId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> balanceService.getBalanceHistory(userId, accountId, from, to))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("account " + accountId + " not found for user " + userId);

        verify(accountRepository).findByUserAndId(userId, accountId);
        verify(balanceRepository, never()).findByAccountAndDateRange(any(), any(), any());
        verify(balanceMapper, never()).toDto(any());
    }

    @Test
    void saveBalance_ShouldCallRepositorySave() {
        // Given
        AccountBalance balanceToSave = AccountBalance.builder()
                .accountId(accountId)
                .balanceType("AVAILABLE")
                .amount(BigDecimal.valueOf(500.00))
                .currency("RUB")
                .build();

        // When
        balanceService.saveBalance(balanceToSave);

        // Then
        verify(balanceRepository).save(balanceToSave);
    }

    @Test
    void getLatestBalances_ShouldReturnEmptyList_WhenNoBalancesFound() {
        // Given
        when(accountRepository.findByUserAndId(userId, accountId))
                .thenReturn(Optional.of(mock(com.bank.model.BankAccount.class)));
        when(balanceRepository.findByAccountIdOrderByAsOfDesc(accountId))
                .thenReturn(List.of());

        // When
        List<BalanceDto> result = balanceService.getLatestBalances(userId, accountId);

        // Then
        assertThat(result).isEmpty();

        verify(accountRepository).findByUserAndId(userId, accountId);
        verify(balanceRepository).findByAccountIdOrderByAsOfDesc(accountId);
        verify(balanceMapper, never()).toDto(any());
    }

    @Test
    void getBalanceHistory_ShouldReturnEmptyList_WhenNoBalancesInDateRange() {
        // Given
        LocalDateTime from = LocalDateTime.now().minusDays(30);
        LocalDateTime to = LocalDateTime.now().minusDays(15);

        when(accountRepository.findByUserAndId(userId, accountId))
                .thenReturn(Optional.of(mock(com.bank.model.BankAccount.class)));
        when(balanceRepository.findByAccountAndDateRange(accountId, from, to))
                .thenReturn(List.of());

        // When
        List<BalanceDto> result = balanceService.getBalanceHistory(userId, accountId, from, to);

        // Then
        assertThat(result).isEmpty();

        verify(accountRepository).findByUserAndId(userId, accountId);
        verify(balanceRepository).findByAccountAndDateRange(accountId, from, to);
        verify(balanceMapper, never()).toDto(any());
    }
}
