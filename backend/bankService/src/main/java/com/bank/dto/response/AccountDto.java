package com.bank.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AccountDto(
        UUID id,

        @JsonProperty("bank_id")
        String bankId,

        @JsonProperty("external_account_id")
        String externalAccountId,

        @JsonProperty("account_number_masked")
        String accountNumberMasked,

        String iban,

        @JsonProperty("account_type")
        String accountType,

        String currency,

        String nickname,

        @JsonProperty("available_balance")
        BigDecimal availableBalance,

        @JsonProperty("booked_balance")
        BigDecimal bookedBalance,

        @JsonProperty("credit_limit")
        BigDecimal creditLimit,

        String status,

        @JsonProperty("last_sync_at")
        LocalDateTime lastSyncAt,

        @JsonProperty("created_at")
        LocalDateTime createdAt
) {}
