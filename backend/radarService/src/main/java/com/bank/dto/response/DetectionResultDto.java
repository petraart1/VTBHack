package com.bank.dto.response;

import java.util.List;

public record DetectionResultDto(
        Integer newSubscriptionsDetected,
        Integer existingSubscriptionsUpdated,
        Integer priceChangesDetected,
        Long processingTimeMs,
        List<SubscriptionDto> newSubscriptions
) {
}

