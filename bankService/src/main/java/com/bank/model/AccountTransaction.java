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
        name = "account_transactions",
        indexes = {
                @Index(name = "idx_tx_account_booking", columnList = "account_id, booking_datetime DESC"),
                @Index(name = "idx_tx_merchant", columnList = "merchant_name, account_id, booking_datetime DESC"),
                @Index(name = "idx_tx_external_id", columnList = "external_transaction_id"),
                @Index(name = "idx_tx_debits", columnList = "account_id, booking_datetime DESC")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "external_transaction_id", nullable = false, length = 128)
    private String externalTransactionId;

    @Column(name = "booking_datetime")
    private LocalDateTime bookingDateTime;

    @Column(name = "value_datetime")
    private LocalDateTime valueDateTime;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "debit_credit_indicator", length = 16)
    private String debitCreditIndicator;

    @Column(name = "status", length = 32)
    @Builder.Default
    private String status = "BOOKED";

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "merchant_name", length = 256)
    private String merchantName;

    @Column(name = "merchant_category_code", length = 64)
    private String merchantCategoryCode;

    @Column(name = "bank_transaction_code", length = 64)
    private String bankTransactionCode;

    @Column(name = "proprietary_code", length = 64)
    private String proprietaryCode;

    @Column(name = "running_balance", precision = 18, scale = 2)
    private BigDecimal runningBalance;

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

