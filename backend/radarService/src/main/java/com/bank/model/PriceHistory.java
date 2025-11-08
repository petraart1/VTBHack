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
 * История изменения цен подписки
 */
@Entity
@Table(
        name = "price_history",
        indexes = {
                @Index(name = "idx_price_history_subscription", columnList = "subscription_id, detected_at DESC")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(name = "old_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal oldPrice;

    @Column(name = "new_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal newPrice;

    @Column(name = "price_change", nullable = false, precision = 18, scale = 2)
    private BigDecimal priceChange;

    @Column(name = "percentage_change")
    private Double percentageChange;

    @Column(name = "transaction_id")
    private UUID transactionId;  // ссылка на транзакцию в BankService (optional)

    @CreationTimestamp
    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    @PrePersist
    protected void onCreate() {
        if (detectedAt == null) {
            detectedAt = LocalDateTime.now();
        }
    }
}

