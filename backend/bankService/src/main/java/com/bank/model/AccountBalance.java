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

@Entity
@Table(
        name = "account_balances",
        indexes = {
                @Index(name = "idx_balances_account_time", columnList = "account_id, as_of_datetime DESC"),
                @Index(name = "idx_balances_recent", columnList = "account_id, balance_type, as_of_datetime DESC")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "balance_type", nullable = false, length = 64)
    private String balanceType;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, columnDefinition = "varchar(3)")
    private String currency;

    @Column(name = "credit_line", precision = 18, scale = 2)
    private BigDecimal creditLine;

    @Column(name = "as_of_datetime", nullable = false)
    private LocalDateTime asOfDateTime;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

