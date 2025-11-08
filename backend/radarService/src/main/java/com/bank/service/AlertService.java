package com.bank.service;

import com.bank.dto.response.AlertDto;
import com.bank.model.*;
import com.bank.repository.SubscriptionAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final SubscriptionAlertRepository alertRepository;

    @Transactional
    @CacheEvict(value = "userAlerts", key = "#userId")
    public void createNewSubscriptionAlert(UUID userId, Subscription subscription) {
        log.info("Creating new subscription alert for user {} - subscription: {}", 
                userId, subscription.getMerchantName());

        SubscriptionAlert alert = new SubscriptionAlert();
        alert.setUserId(userId);
        alert.setSubscriptionId(subscription.getId());
        alert.setAlertType(SubscriptionAlert.AlertType.NEW_SUBSCRIPTION_DETECTED);
        alert.setTitle("Обнаружена новая подписка");
        alert.setMessage(String.format(
                "Найдена подписка на %s с ежемесячным платежом %.2f %s",
                subscription.getMerchantName(),
                subscription.getAmount(),
                subscription.getCurrency()
        ));
        alert.setActionUrl("/radar/subscriptions/" + subscription.getId());
        alert.setRead(false);
        alert.setCreatedAt(LocalDateTime.now());

        alertRepository.save(alert);
    }

    @Transactional
    @CacheEvict(value = "userAlerts", key = "#userId")
    public void createPriceIncreaseAlert(UUID userId, Subscription subscription, PriceHistory priceHistory) {
        log.info("Creating price increase alert for user {} - subscription: {}", 
                userId, subscription.getMerchantName());

        SubscriptionAlert alert = new SubscriptionAlert();
        alert.setUserId(userId);
        alert.setSubscriptionId(subscription.getId());
        alert.setAlertType(SubscriptionAlert.AlertType.PRICE_INCREASE);
        alert.setTitle("Повышение цены подписки");
        alert.setMessage(String.format(
                "Цена подписки на %s увеличилась с %.2f %s до %.2f %s (+%.1f%%)",
                subscription.getMerchantName(),
                priceHistory.getOldPrice(),
                subscription.getCurrency(),
                priceHistory.getNewPrice(),
                subscription.getCurrency(),
                Math.abs(priceHistory.getPercentageChange())
        ));
        alert.setActionUrl("/radar/subscriptions/" + subscription.getId());
        alert.setRead(false);
        alert.setCreatedAt(LocalDateTime.now());

        alertRepository.save(alert);
    }

    @Transactional
    @CacheEvict(value = "userAlerts", key = "#userId")
    public void createUnusedSubscriptionAlert(UUID userId, Subscription subscription) {
        log.info("Creating unused subscription alert for user {} - subscription: {}", 
                userId, subscription.getMerchantName());

        SubscriptionAlert alert = new SubscriptionAlert();
        alert.setUserId(userId);
        alert.setSubscriptionId(subscription.getId());
        alert.setAlertType(SubscriptionAlert.AlertType.UNUSED_SUBSCRIPTION);
        alert.setTitle("Неиспользуемая подписка");
        alert.setMessage(String.format(
                "Подписка на %s не используется. Вы тратите %.2f %s в месяц",
                subscription.getMerchantName(),
                subscription.getAmount(),
                subscription.getCurrency()
        ));
        alert.setActionUrl("/radar/subscriptions/" + subscription.getId() + "/cancel");
        alert.setRead(false);
        alert.setCreatedAt(LocalDateTime.now());

        alertRepository.save(alert);
    }

    @Transactional
    @CacheEvict(value = "userAlerts", key = "#userId")
    public void createBetterAlternativeAlert(UUID userId, Subscription subscription, SubscriptionAlternative alternative) {
        log.info("Creating better alternative alert for user {} - subscription: {}", 
                userId, subscription.getMerchantName());

        SubscriptionAlert alert = new SubscriptionAlert();
        alert.setUserId(userId);
        alert.setSubscriptionId(subscription.getId());
        alert.setAlertType(SubscriptionAlert.AlertType.BETTER_ALTERNATIVE);
        alert.setTitle("Найдена более выгодная альтернатива");
        alert.setMessage(String.format(
                "Вместо %s (%.2f %s) можно использовать %s (%.2f %s). Экономия: %.2f %s в месяц",
                subscription.getMerchantName(),
                subscription.getAmount(),
                subscription.getCurrency(),
                alternative.getAlternativeName(),
                alternative.getPrice(),
                alternative.getCurrency(),
                alternative.getSavings(),
                subscription.getCurrency()
        ));
        alert.setActionUrl("/radar/alternatives/" + alternative.getId());
        alert.setRead(false);
        alert.setCreatedAt(LocalDateTime.now());

        alertRepository.save(alert);
    }

    @Cacheable(value = "userAlerts", key = "#userId")
    public List<AlertDto> getUnreadAlerts(UUID userId) {
        return alertRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<AlertDto> getAllAlerts(UUID userId) {
        return alertRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "userAlerts", key = "#userId")
    public void markAsRead(UUID userId, UUID alertId) {
        SubscriptionAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found"));

        if (!alert.getUserId().equals(userId)) {
            throw new RuntimeException("Alert does not belong to user");
        }

        alert.setRead(true);
        alertRepository.save(alert);
        
        log.info("Marked alert {} as read for user {}", alertId, userId);
    }

    @Transactional
    @CacheEvict(value = "userAlerts", key = "#userId")
    public void markAllAsRead(UUID userId) {
        List<SubscriptionAlert> unreadAlerts = alertRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false);
        
        unreadAlerts.forEach(alert -> alert.setRead(true));
        alertRepository.saveAll(unreadAlerts);
        
        log.info("Marked {} alerts as read for user {}", unreadAlerts.size(), userId);
    }

    @Transactional
    @CacheEvict(value = "userAlerts", key = "#userId")
    public void deleteAlert(UUID userId, UUID alertId) {
        SubscriptionAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found"));

        if (!alert.getUserId().equals(userId)) {
            throw new RuntimeException("Alert does not belong to user");
        }

        alertRepository.delete(alert);
        
        log.info("Deleted alert {} for user {}", alertId, userId);
    }

    private AlertDto toDto(SubscriptionAlert alert) {
        return AlertDto.from(alert);
    }
}
