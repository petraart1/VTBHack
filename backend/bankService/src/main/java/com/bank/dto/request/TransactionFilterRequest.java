package com.bank.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
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
        @Pattern(regexp = "^$|(DEBIT|CREDIT)", message = "debit_credit_indicator must be empty, DEBIT or CREDIT")
        String debitCreditIndicator,

        @Min(value = 0, message = "page must be >= 0")
        int page,

        @Min(value = 1, message = "size must be >= 1")
        @Max(value = 500, message = "size must be <= 500")
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
