package com.bank.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransactionDto(
        UUID id,

        @JsonProperty("account_id")
        UUID accountId,

        @JsonProperty("external_transaction_id")
        String externalTransactionId,

        @JsonProperty("booking_datetime")
        LocalDateTime bookingDateTime,

        @JsonProperty("value_datetime")
        LocalDateTime valueDateTime,

        BigDecimal amount,

        String currency,

        @JsonProperty("debit_credit_indicator")
        String debitCreditIndicator,

        String status,

        String description,

        @JsonProperty("merchant_name")
        String merchantName,

        @JsonProperty("merchant_category_code")
        String merchantCategoryCode,

        @JsonProperty("running_balance")
        BigDecimal runningBalance,

        @JsonProperty("created_at")
        LocalDateTime createdAt
) {

}
