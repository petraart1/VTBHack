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

@Entity
@Table(
        name = "bank_accounts",
        indexes = {
                @Index(name = "idx_accounts_user_id", columnList = "user_id"),
                @Index(name = "idx_accounts_user_bank", columnList = "user_id, bank_id"),
                @Index(name = "idx_accounts_external_id", columnList = "external_account_id"),
                @Index(name = "idx_accounts_last_sync", columnList = "last_sync_at")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "bank_id", nullable = false, length = 64)
    private String bankId;

    @Column(name = "external_account_id", nullable = false, length = 128)
    private String externalAccountId;

    @Column(name = "account_number_masked", length = 64)
    private String accountNumberMasked;

    @Column(name = "iban", length = 34)
    private String iban;

    @Column(name = "account_type", length = 64)
    private String accountType;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "nickname", length = 128)
    private String nickname;

    @Column(name = "available_balance", precision = 18, scale = 2)
    private BigDecimal availableBalance;

    @Column(name = "booked_balance", precision = 18, scale = 2)
    private BigDecimal bookedBalance;

    @Column(name = "credit_limit", precision = 18, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "status", length = 32)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

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
}
