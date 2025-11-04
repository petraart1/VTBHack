package com.bank.dto.bank;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO для ответа внешнего банка на запрос транзакций
 * Open Banking Russia v2.1 compatible
 */
public record ExternalTransactionResponseDto(
        @JsonProperty("transactions")
        List<ExternalTransactionDto> transactions
) {
    
    public record ExternalTransactionDto(
            @JsonProperty("transaction_id")
            String transactionId,
            
            @JsonProperty("booking_datetime")
            LocalDateTime bookingDateTime,
            
            @JsonProperty("value_datetime")
            LocalDateTime valueDateTime,
            
            @JsonProperty("amount")
            BigDecimal amount,
            
            @JsonProperty("currency")
            String currency,
            
            @JsonProperty("debit_credit_indicator")
            String debitCreditIndicator,
            
            @JsonProperty("status")
            String status,
            
            @JsonProperty("description")
            String description,
            
            @JsonProperty("merchant_name")
            String merchantName,
            
            @JsonProperty("merchant_category_code")
            String merchantCategoryCode,
            
            @JsonProperty("running_balance")
            BigDecimal runningBalance
    ) {
    }
}

