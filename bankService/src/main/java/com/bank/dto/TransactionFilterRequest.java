package com.bank.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record TransactionFilterRequest(
        @JsonProperty("from_booking_datetime")
        LocalDateTime fromBookingDateTime,

        @JsonProperty("to_booking_datetime")
        LocalDateTime toBookingDateTime,

        @JsonProperty("merchant_name")
        String merchantName,

        @JsonProperty("merchant_category_code")
        String merchantCategoryCode,

        @JsonProperty("debit_credit_indicator")
        String debitCreditIndicator,

        int page,

        int size
) {
    // Конструктор по умолчанию с дефолтными значениями
    public TransactionFilterRequest {
        page = page > 0 ? page : 0;
        size = size > 0 ? size : 50;
    }

    // Дополнительный конструктор для удобства
    public TransactionFilterRequest() {
        this(null, null, null, null, null, 0, 50);
    }
}
