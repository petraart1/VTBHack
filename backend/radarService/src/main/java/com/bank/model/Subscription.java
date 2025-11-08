package com.bank.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Подписка пользователя (обнаруженная или подтвержденная)
 * Соответствует стандартам ЦБ РФ для финансовых сервисов
 */
@Entity
@Table(
        name = "subscriptions",
        indexes = {
                @Index(name = "idx_subscriptions_user_id", columnList = "user_id"),
                @Index(name = "idx_subscriptions_status", columnList = "status"),
                @Index(name = "idx_subscriptions_next_payment", columnList = "user_id, next_expected_date"),
                @Index(name = "idx_subscriptions_category", columnList = "user_id, category"),
                @Index(name = "idx_subscriptions_merchant", columnList = "merchant_name")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "account_id")
    private UUID accountId;  // может быть null если подписка из нескольких счетов

    // Характеристики подписки
    @Column(name = "merchant_name", nullable = false, length = 256)
    private String merchantName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 64)
    private SubscriptionCategory category;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 32)
    private SubscriptionFrequency frequency;

    // Статус
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.DETECTED;

    // Даты
    @Column(name = "first_payment_date", nullable = false)
    private LocalDateTime firstPaymentDate;

    @Column(name = "last_payment_date", nullable = false)
    private LocalDateTime lastPaymentDate;

    @Column(name = "next_expected_date")
    private LocalDateTime nextExpectedDate;

    // Аналитика
    @Column(name = "total_payments", nullable = false)
    @Builder.Default
    private Integer totalPayments = 0;

    @Column(name = "total_spent", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalSpent = BigDecimal.ZERO;

    @Column(name = "price_increased")
    @Builder.Default
    private boolean priceIncreased = false;

    @Column(name = "previous_amount", precision = 18, scale = 2)
    private BigDecimal previousAmount;

    // Confidence score (насколько уверены что это подписка)
    @Column(name = "confidence_score")
    private Double confidenceScore;  // 0.0 - 1.0

    // Дополнительная информация
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "cancellation_url", length = 512)
    private String cancellationUrl;  // URL для отмены подписки

    @Column(name = "cancellation_instructions", columnDefinition = "TEXT")
    private String cancellationInstructions;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum SubscriptionStatus {
        DETECTED,    // Автоматически обнаружена
        CONFIRMED,   // Подтверждена пользователем
        CANCELLED,   // Отменена
        INACTIVE     // Неактивна (давно не было платежей)
    }
}

