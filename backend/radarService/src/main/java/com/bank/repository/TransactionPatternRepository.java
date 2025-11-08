package com.bank.repository;

import com.bank.model.TransactionPattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionPatternRepository extends JpaRepository<TransactionPattern, UUID> {

    Optional<TransactionPattern> findByUserIdAndMerchantName(UUID userId, String merchantName);

    List<TransactionPattern> findByUserId(UUID userId);

    @Query("SELECT tp FROM TransactionPattern tp WHERE tp.userId = :userId " +
           "AND tp.occurrenceCount >= :minOccurrences " +
           "AND tp.stdDevDays <= :maxStdDevDays " +
           "ORDER BY tp.lastSeen DESC")
    List<TransactionPattern> findRecurringPatterns(
            @Param("userId") UUID userId,
            @Param("minOccurrences") Integer minOccurrences,
            @Param("maxStdDevDays") Integer maxStdDevDays
    );

    @Query("SELECT tp FROM TransactionPattern tp WHERE tp.lastAnalyzed < :threshold")
    List<TransactionPattern> findStalePatterns(@Param("threshold") LocalDateTime threshold);
}

