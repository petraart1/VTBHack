package com.bank.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Альтернативные предложения для подписки
 */
@Entity
@Table(
        name = "subscription_alternatives",
        indexes = {
                @Index(name = "idx_alternatives_subscription", columnList = "subscription_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionAlternative {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(name = "alternative_name", nullable = false, length = 256)
    private String alternativeName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "frequency", nullable = false, length = 32)
    private String frequency;

    @Column(name = "savings", precision = 18, scale = 2)
    private BigDecimal savings;  // сколько сэкономит пользователь

    @Column(name = "referral_link", length = 512)
    private String referralLink;

    @Column(name = "rating")
    private Integer rating;  // 1-5 звезд

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

