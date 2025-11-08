package com.bank.dto.response;

import com.bank.model.SubscriptionAlternative;

import java.math.BigDecimal;
import java.util.UUID;

public record AlternativeDto(
        UUID id,
        UUID subscriptionId,
        String alternativeName,
        String description,
        BigDecimal price,
        String currency,
        String frequency,
        BigDecimal savings,
        String referralLink,
        Integer rating
) {
    public static AlternativeDto from(SubscriptionAlternative alternative) {
        return new AlternativeDto(
                alternative.getId(),
                alternative.getSubscriptionId(),
                alternative.getAlternativeName(),
                alternative.getDescription(),
                alternative.getPrice(),
                alternative.getCurrency(),
                alternative.getFrequency(),
                alternative.getSavings(),
                alternative.getReferralLink(),
                alternative.getRating()
        );
    }
}

