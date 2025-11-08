package com.bank.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Алерты для пользователя о подписках
 */
@Entity
@Table(
        name = "subscription_alerts",
        indexes = {
                @Index(name = "idx_alerts_user_unread", columnList = "user_id, is_read, created_at DESC")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "subscription_id")
    private UUID subscriptionId;  // может быть null для общих алертов

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 64)
    private AlertType alertType;

    @Column(name = "title", nullable = false, length = 256)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "action_url", length = 512)
    private String actionUrl;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean isRead = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public enum AlertType {
        PRICE_INCREASE,              // Цена подписки выросла
        NEW_SUBSCRIPTION_DETECTED,   // Обнаружена новая подписка
        UNUSED_SUBSCRIPTION,         // Подписка не используется
        BETTER_ALTERNATIVE,          // Найдена более выгодная альтернатива
        UPCOMING_PAYMENT,            // Скоро списание
        CANCELLATION_REMINDER        // Напоминание отменить подписку
    }
}

