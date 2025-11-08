package com.bank.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO для счета из BankService
 */
public record AccountDto(
        UUID id,
        UUID userId,
        String bankId,
        @JsonProperty("externalAccountId")
        String externalAccountId,
        @JsonProperty("accountNumberMasked")
        String accountNumberMasked,
        String iban,
        @JsonProperty("accountType")
        String accountType,
        String currency,
        String nickname,
        @JsonProperty("availableBalance")
        BigDecimal availableBalance,
        @JsonProperty("bookedBalance")
        BigDecimal bookedBalance,
        String status
) {
}

