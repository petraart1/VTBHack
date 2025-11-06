package com.bank.repository;

import com.bank.model.BankAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<BankAccount, UUID> {

    @Query("select ba from BankAccount ba where ba.userId = :userId and ba.status = 'ACTIVE'")
    List<BankAccount> findActiveAccountsByUserId(@Param("userId") UUID userId);

    @Query("select ba from BankAccount ba where ba.userId = :userId and ba.bankId = :bankId and ba.status = 'ACTIVE'")
    List<BankAccount> findActiveAccountsByUserAndBank(
            @Param("userId") UUID userId,
            @Param("bankId") String bankId
    );

    @Query("select ba from BankAccount ba where ba.userId = :userId and ba.id = :accountId")
    Optional<BankAccount> findByUserAndId(
            @Param("userId") UUID userId,
            @Param("accountId") UUID accountId
    );

    @Query("select ba from BankAccount ba where ba.userId = :userId and ba.externalAccountId = :externalAccountId")
    Optional<BankAccount> findByUserAndExternalAccountId(
            @Param("userId") UUID userId,
            @Param("externalAccountId") String externalAccountId
    );

    @Query("select ba from BankAccount ba where ba.userId = :userId")
    Page<BankAccount> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("select ba from BankAccount ba where ba.userId = :userId")
    List<BankAccount> findByUserId(@Param("userId") UUID userId);
}

