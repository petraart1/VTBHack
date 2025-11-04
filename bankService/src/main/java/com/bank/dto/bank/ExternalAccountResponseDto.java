package com.bank.dto.bank;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO для ответа внешнего банка на запрос списка счетов
 * Open Banking Russia v2.1 compatible
 */
public record ExternalAccountResponseDto(
        @JsonProperty("accounts")
        List<ExternalAccountDto> accounts
) {
    
    public record ExternalAccountDto(
            @JsonProperty("account_id")
            String accountId,
            
            @JsonProperty("account_type")
            String accountType,
            
            @JsonProperty("currency")
            String currency,
            
            @JsonProperty("account_number")
            String accountNumber,
            
            @JsonProperty("iban")
            String iban,
            
            @JsonProperty("status")
            String status,
            
            @JsonProperty("balances")
            List<BalanceDto> balances
    ) {
    }
    
    public record BalanceDto(
            @JsonProperty("balance_type")
            String balanceType,
            
            @JsonProperty("amount")
            BigDecimal amount,
            
            @JsonProperty("currency")
            String currency,
            
            @JsonProperty("credit_line")
            BigDecimal creditLine
    ) {
    }
}

