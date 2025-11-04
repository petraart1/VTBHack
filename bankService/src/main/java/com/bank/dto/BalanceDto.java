package com.bank.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BalanceDto(
        @JsonProperty("balance_type")
        String balanceType,

        BigDecimal amount,

        String currency,

        @JsonProperty("credit_line")
        BigDecimal creditLine,

        @JsonProperty("as_of_datetime")
        LocalDateTime asOfDateTime
) {

}


