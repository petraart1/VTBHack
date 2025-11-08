package com.bank.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record SubscriptionStatsDto(
        Integer totalSubscriptions,
        Integer activeSubscriptions,
        Integer cancelledSubscriptions,
        BigDecimal monthlyTotal,
        BigDecimal yearlyTotal,
        BigDecimal potentialSavings,
        List<SubscriptionDto> topExpensive,
        List<SubscriptionDto> unusedSubscriptions,
        Integer priceIncreasesCount
) {
}

