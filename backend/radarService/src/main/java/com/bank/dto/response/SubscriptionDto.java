package com.bank.dto.response;

import com.bank.model.Subscription;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SubscriptionDto(
        UUID id,
        UUID userId,
        UUID accountId,
        String merchantName,
        String category,
        BigDecimal amount,
        String currency,
        String frequency,
        String status,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime firstPaymentDate,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime lastPaymentDate,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime nextExpectedDate,
        Integer totalPayments,
        BigDecimal totalSpent,
        Boolean priceIncreased,
        BigDecimal previousAmount,
        Double confidenceScore,
        String description,
        String cancellationUrl,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime updatedAt
) {
    public static SubscriptionDto from(Subscription subscription) {
        return new SubscriptionDto(
                subscription.getId(),
                subscription.getUserId(),
                subscription.getAccountId(),
                subscription.getMerchantName(),
                subscription.getCategory() != null ? subscription.getCategory().name() : null,
                subscription.getAmount(),
                subscription.getCurrency(),
                subscription.getFrequency() != null ? subscription.getFrequency().name() : null,
                subscription.getStatus().name(),
                subscription.getFirstPaymentDate(),
                subscription.getLastPaymentDate(),
                subscription.getNextExpectedDate(),
                subscription.getTotalPayments(),
                subscription.getTotalSpent(),
                subscription.isPriceIncreased(),
                subscription.getPreviousAmount(),
                subscription.getConfidenceScore(),
                subscription.getDescription(),
                subscription.getCancellationUrl(),
                subscription.getCreatedAt(),
                subscription.getUpdatedAt()
        );
    }
}

