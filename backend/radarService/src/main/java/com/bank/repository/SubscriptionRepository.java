package com.bank.repository;

import com.bank.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    List<Subscription> findByUserId(UUID userId);

    List<Subscription> findByUserIdAndStatus(UUID userId, Subscription.SubscriptionStatus status);
    
    List<Subscription> findByUserIdAndStatusIn(UUID userId, List<Subscription.SubscriptionStatus> statuses);

    List<Subscription> findByUserIdAndCategory(UUID userId, String category);

    Optional<Subscription> findByUserIdAndMerchantName(UUID userId, String merchantName);
    
    Optional<Subscription> findByUserIdAndMerchantNameIgnoreCase(UUID userId, String merchantName);

    @Query("SELECT DISTINCT s.userId FROM Subscription s WHERE s.status IN ('DETECTED', 'CONFIRMED')")
    List<UUID> findAllActiveUsers();

    @Query("SELECT s FROM Subscription s WHERE s.nextExpectedDate BETWEEN :from AND :to")
    List<Subscription> findUpcomingPayments(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("SELECT s FROM Subscription s WHERE s.status = 'CONFIRMED' " +
           "AND s.lastPaymentDate < :threshold")
    List<Subscription> findUnusedSubscriptions(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT s FROM Subscription s WHERE s.userId = :userId " +
           "AND s.priceIncreased = true " +
           "ORDER BY s.updatedAt DESC")
    List<Subscription> findRecentPriceIncreases(@Param("userId") UUID userId);
}

