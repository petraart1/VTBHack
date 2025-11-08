package com.bank.repository;

import com.bank.model.SubscriptionAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubscriptionAlertRepository extends JpaRepository<SubscriptionAlert, UUID> {

    List<SubscriptionAlert> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<SubscriptionAlert> findByUserIdAndIsReadOrderByCreatedAtDesc(UUID userId, Boolean isRead);

    List<SubscriptionAlert> findTop10ByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID userId);

    long countByUserIdAndIsReadFalse(UUID userId);
}

