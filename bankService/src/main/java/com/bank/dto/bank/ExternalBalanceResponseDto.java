package com.bank.dto.bank;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO для ответа внешнего банка на запрос балансов
 * Open Banking Russia v2.1 compatible
 */
public record ExternalBalanceResponseDto(
        @JsonProperty("balances")
        List<ExternalBalanceDto> balances
) {
    
    public record ExternalBalanceDto(
            @JsonProperty("balance_type")
            String balanceType,
            
            @JsonProperty("amount")
            BigDecimal amount,
            
            @JsonProperty("currency")
            String currency,
            
            @JsonProperty("credit_line")
            BigDecimal creditLine,
            
            @JsonProperty("as_of_datetime")
            LocalDateTime asOfDateTime
    ) {
    }
}

