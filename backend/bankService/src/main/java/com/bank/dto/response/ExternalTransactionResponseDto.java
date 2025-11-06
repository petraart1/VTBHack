package com.bank.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO для ответа внешнего банка на запрос транзакций
 * Open Banking Russia v2.1 compatible
 */
public record ExternalTransactionResponseDto(
        @JsonProperty("data")
        Data data,

        @JsonProperty("links")
        Links links,

        @JsonProperty("meta")
        Meta meta
) {

    public List<ExternalTransactionDto> transactions() {
        return data != null && data.transaction() != null ? data.transaction() : List.of();
    }

    public record Data(
            @JsonProperty("transaction")
            List<ExternalTransactionDto> transaction
    ) {
    }

    public record Links(
            @JsonProperty("self")
            String self,

            @JsonProperty("next")
            String next
    ) {
    }

    public record Meta(
            @JsonProperty("totalPages")
            Integer totalPages
    ) {
    }
    
    public record ExternalTransactionDto(
            @JsonProperty("transactionId")
            String transactionId,

            @JsonProperty("bookingDateTime")
            String bookingDateTime,

            @JsonProperty("valueDateTime")
            String valueDateTime,

            @JsonProperty("amount")
            Amount amount,

            @JsonProperty("currency")
            String currency,

            @JsonProperty("debitCreditIndicator")
            String debitCreditIndicator,

            @JsonProperty("status")
            String status,

            @JsonProperty("description")
            String description,

            @JsonProperty("merchantName")
            String merchantName,

            @JsonProperty("merchantCategoryCode")
            String merchantCategoryCode,

            @JsonProperty("runningBalance")
            BigDecimal runningBalance
    ) {

        public BigDecimal amountValue() {
            return amount != null ? amount.amount() : null;
        }

        public String amountCurrency() {
            return amount != null ? amount.currency() : null;
        }
    }

    public record Amount(
            @JsonProperty("amount")
            BigDecimal amount,

            @JsonProperty("currency")
            String currency
    ) {
    }
}
