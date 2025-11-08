package com.bank.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO для транзакции из BankService
 * Копирует структуру из bankService/dto/response/TransactionDto.java
 */
public record TransactionDto(
        UUID id,
        UUID accountId,
        @JsonProperty("externalTransactionId")
        String externalTransactionId,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @JsonProperty("bookingDateTime")
        LocalDateTime bookingDateTime,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @JsonProperty("valueDateTime")
        LocalDateTime valueDateTime,
        BigDecimal amount,
        String currency,
        @JsonProperty("debitCreditIndicator")
        String debitCreditIndicator,
        String status,
        String description,
        @JsonProperty("merchantName")
        String merchantName,
        @JsonProperty("merchantCategoryCode")
        String merchantCategoryCode,
        @JsonProperty("bankTransactionCode")
        String bankTransactionCode,
        @JsonProperty("runningBalance")
        BigDecimal runningBalance,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @JsonProperty("createdAt")
        LocalDateTime createdAt
) {
}

