package com.bank.dto.response;

import com.bank.model.SubscriptionAlert;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.UUID;

public record AlertDto(
        UUID id,
        UUID userId,
        UUID subscriptionId,
        String alertType,
        String title,
        String message,
        String actionUrl,
        Boolean isRead,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {
    public static AlertDto from(SubscriptionAlert alert) {
        return new AlertDto(
                alert.getId(),
                alert.getUserId(),
                alert.getSubscriptionId(),
                alert.getAlertType().name(),
                alert.getTitle(),
                alert.getMessage(),
                alert.getActionUrl(),
                alert.isRead(),
                alert.getCreatedAt()
        );
    }
}

