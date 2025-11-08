package com.bank.repository;

import com.bank.model.SubscriptionAlternative;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubscriptionAlternativeRepository extends JpaRepository<SubscriptionAlternative, UUID> {

    List<SubscriptionAlternative> findBySubscriptionIdOrderByPriceAsc(UUID subscriptionId);
    
    List<SubscriptionAlternative> findBySubscriptionIdOrderBySavingsDesc(UUID subscriptionId);
    
    java.util.Optional<SubscriptionAlternative> findTopBySubscriptionIdOrderBySavingsDesc(UUID subscriptionId);

    void deleteBySubscriptionId(UUID subscriptionId);
}

