package com.bank.repository;

import com.bank.model.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, UUID> {

    List<PriceHistory> findBySubscriptionIdOrderByDetectedAtDesc(UUID subscriptionId);

    List<PriceHistory> findTop10BySubscriptionIdOrderByDetectedAtDesc(UUID subscriptionId);
    
    boolean existsBySubscriptionIdAndNewPrice(UUID subscriptionId, BigDecimal newPrice);
    
    List<PriceHistory> findBySubscriptionIdInOrderByDetectedAtDesc(List<UUID> subscriptionIds);
}

