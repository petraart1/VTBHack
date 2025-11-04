package com.bank.dto;

/**
 * DTO для результата синхронизации
 */
public record SyncResultDto(
        boolean success,
        int accountsSynced,
        int transactionsSynced,
        int balancesSynced,
        long durationMs,
        String errorMessage
) {
}

