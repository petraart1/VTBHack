package com.bank.dto.response;

import com.bank.model.PriceHistory;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PriceHistoryDto(
        UUID id,
        UUID subscriptionId,
        BigDecimal oldPrice,
        BigDecimal newPrice,
        BigDecimal priceChange,
        Double percentageChange,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime detectedAt
) {
    public static PriceHistoryDto from(PriceHistory priceHistory) {
        return new PriceHistoryDto(
                priceHistory.getId(),
                priceHistory.getSubscriptionId(),
                priceHistory.getOldPrice(),
                priceHistory.getNewPrice(),
                priceHistory.getPriceChange(),
                priceHistory.getPercentageChange(),
                priceHistory.getDetectedAt()
        );
    }
}

