package com.bank.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO для ответа внешнего банка на запрос балансов
 * Open Banking Russia v2.1 compatible
 */
public record ExternalBalanceResponseDto(
        @JsonProperty("data")
        Data data,

        @JsonProperty("links")
        Links links,

        @JsonProperty("meta")
        Meta meta
) {

    public List<ExternalBalanceDto> balances() {
        return data != null && data.balance() != null ? data.balance() : List.of();
    }

    public record Data(
            @JsonProperty("balance")
            List<ExternalBalanceDto> balance
    ) {
    }

    public record Links(
            @JsonProperty("self")
            String self
    ) {
    }

    public record Meta(
            @JsonProperty("totalPages")
            Integer totalPages
    ) {
    }
    
    public record ExternalBalanceDto(
            @JsonProperty("balanceType")
            String balanceType,

            @JsonProperty("amount")
            Amount amount,

            @JsonProperty("currency")
            String currency,

            @JsonProperty("creditLine")
            BigDecimal creditLine,

            @JsonProperty("asOfDateTime")
            String asOfDateTime
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
