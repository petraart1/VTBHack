package com.bank.service;

import com.bank.dto.response.SubscriptionDto;
import com.bank.dto.response.SubscriptionStatsDto;
import com.bank.model.Subscription;
import com.bank.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionManagementService {

    private final SubscriptionRepository subscriptionRepository;

    @Cacheable(value = "subscriptionDetails", key = "#subscriptionId")
    public SubscriptionDto getSubscriptionDetails(UUID userId, UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        if (!subscription.getUserId().equals(userId)) {
            throw new RuntimeException("Subscription does not belong to user");
        }

        return toDto(subscription);
    }

    @Transactional
    @CacheEvict(value = {"userSubscriptions", "subscriptionDetails"}, key = "#subscriptionId")
    public SubscriptionDto confirmSubscription(UUID userId, UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        if (!subscription.getUserId().equals(userId)) {
            throw new RuntimeException("Subscription does not belong to user");
        }

        if (subscription.getStatus() != Subscription.SubscriptionStatus.DETECTED) {
            throw new RuntimeException("Only DETECTED subscriptions can be confirmed");
        }

        subscription.setStatus(Subscription.SubscriptionStatus.CONFIRMED);
        subscriptionRepository.save(subscription);

        log.info("User {} confirmed subscription: {}", userId, subscriptionId);

        return toDto(subscription);
    }

    @Transactional
    @CacheEvict(value = {"userSubscriptions", "subscriptionDetails"}, key = "#subscriptionId")
    public void cancelSubscription(UUID userId, UUID subscriptionId, String reason) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        if (!subscription.getUserId().equals(userId)) {
            throw new RuntimeException("Subscription does not belong to user");
        }

        subscription.setStatus(Subscription.SubscriptionStatus.CANCELLED);
        subscriptionRepository.save(subscription);

        log.info("User {} cancelled subscription: {} (reason: {})", userId, subscriptionId, reason);
    }

    @Cacheable(value = "subscriptionStats", key = "#userId")
    public SubscriptionStatsDto getSubscriptionStats(UUID userId) {
        List<Subscription> allSubscriptions = subscriptionRepository.findByUserId(userId);

        int activeCount = (int) allSubscriptions.stream()
                .filter(s -> s.getStatus() == Subscription.SubscriptionStatus.CONFIRMED || 
                             s.getStatus() == Subscription.SubscriptionStatus.DETECTED)
                .count();

        int detectedCount = (int) allSubscriptions.stream()
                .filter(s -> s.getStatus() == Subscription.SubscriptionStatus.DETECTED)
                .count();

        int cancelledCount = (int) allSubscriptions.stream()
                .filter(s -> s.getStatus() == Subscription.SubscriptionStatus.CANCELLED)
                .count();

        BigDecimal totalMonthlySpend = allSubscriptions.stream()
                .filter(s -> s.getStatus() == Subscription.SubscriptionStatus.CONFIRMED || 
                             s.getStatus() == Subscription.SubscriptionStatus.DETECTED)
                .map(Subscription::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalYearlySpend = totalMonthlySpend.multiply(BigDecimal.valueOf(12));

        BigDecimal potentialSavings = allSubscriptions.stream()
                .filter(s -> s.getStatus() == Subscription.SubscriptionStatus.CANCELLED)
                .map(Subscription::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<SubscriptionDto> topSubscriptions = allSubscriptions.stream()
                .filter(s -> s.getStatus() == Subscription.SubscriptionStatus.CONFIRMED || 
                             s.getStatus() == Subscription.SubscriptionStatus.DETECTED)
                .sorted((s1, s2) -> s2.getAmount().compareTo(s1.getAmount()))
                .limit(5)
                .map(this::toDto)
                .collect(Collectors.toList());

        List<SubscriptionDto> recentSubscriptions = allSubscriptions.stream()
                .filter(s -> s.getStatus() == Subscription.SubscriptionStatus.DETECTED)
                .sorted((s1, s2) -> s2.getCreatedAt().compareTo(s1.getCreatedAt()))
                .limit(5)
                .map(this::toDto)
                .collect(Collectors.toList());

        int priceIncreasedCount = (int) allSubscriptions.stream()
                .filter(Subscription::isPriceIncreased)
                .count();

        return new SubscriptionStatsDto(
                activeCount,
                detectedCount,
                cancelledCount,
                totalMonthlySpend,
                totalYearlySpend,
                potentialSavings,
                topSubscriptions,
                recentSubscriptions,
                priceIncreasedCount
        );
    }

    private SubscriptionDto toDto(Subscription subscription) {
        return SubscriptionDto.from(subscription);
    }
}
