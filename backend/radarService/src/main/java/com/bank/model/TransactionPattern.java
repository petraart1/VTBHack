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
 * Паттерны транзакций для ML алгоритма обнаружения подписок
 */
@Entity
@Table(
        name = "transaction_patterns",
        indexes = {
                @Index(name = "idx_patterns_user", columnList = "user_id, last_analyzed"),
                @Index(name = "idx_patterns_merchant", columnList = "merchant_name")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uc_user_merchant", columnNames = {"user_id", "merchant_name"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionPattern {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "merchant_name", nullable = false, length = 256)
    private String merchantName;

    @Column(name = "merchant_category", length = 64)
    private String merchantCategory;

    // Статистика
    @Column(name = "occurrence_count", nullable = false)
    @Builder.Default
    private Integer occurrenceCount = 0;

    @Column(name = "average_amount", precision = 18, scale = 2)
    private BigDecimal averageAmount;

    @Column(name = "std_deviation", precision = 18, scale = 2)
    private BigDecimal stdDeviation;

    @Column(name = "average_days_between")
    private Integer averageDaysBetween;

    @Column(name = "std_dev_days")
    private Integer stdDevDays;

    // Последние даты платежей (храним как TEXT в формате JSON для простоты)
    @Column(name = "recent_payment_dates", columnDefinition = "TEXT")
    private String recentPaymentDates;  // JSON array: ["2025-01-01T10:00:00", ...]

    @CreationTimestamp
    @Column(name = "first_seen", nullable = false)
    private LocalDateTime firstSeen;

    @Column(name = "last_seen", nullable = false)
    private LocalDateTime lastSeen;

    @UpdateTimestamp
    @Column(name = "last_analyzed", nullable = false)
    private LocalDateTime lastAnalyzed;

    @PrePersist
    protected void onCreate() {
        if (firstSeen == null) {
            firstSeen = LocalDateTime.now();
        }
        if (lastSeen == null) {
            lastSeen = LocalDateTime.now();
        }
        if (lastAnalyzed == null) {
            lastAnalyzed = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastAnalyzed = LocalDateTime.now();
    }
}

