package com.bank.service;

import com.bank.dto.response.PriceHistoryDto;
import com.bank.model.PriceHistory;
import com.bank.model.Subscription;
import com.bank.repository.PriceHistoryRepository;
import com.bank.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceMonitoringService {

    private final SubscriptionRepository subscriptionRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final AlertService alertService;

    private static final double MIN_PRICE_CHANGE_PERCENT = 5.0;

    @Transactional
    public void checkPriceChanges(UUID userId) {
        log.info("Checking price changes for user: {}", userId);
        
        List<Subscription> activeSubscriptions = subscriptionRepository.findByUserIdAndStatusIn(
                userId,
                List.of(Subscription.SubscriptionStatus.DETECTED, Subscription.SubscriptionStatus.CONFIRMED)
        );

        int priceChangesDetected = 0;

        for (Subscription subscription : activeSubscriptions) {
            if (subscription.isPriceIncreased() && subscription.getPreviousAmount() != null) {
                boolean historyExists = priceHistoryRepository.existsBySubscriptionIdAndNewPrice(
                        subscription.getId(),
                        subscription.getAmount()
                );

                if (!historyExists) {
                    PriceHistory priceHistory = createPriceHistory(subscription);
                    priceHistoryRepository.save(priceHistory);

                    if (Math.abs(priceHistory.getPercentageChange()) >= MIN_PRICE_CHANGE_PERCENT) {
                        alertService.createPriceIncreaseAlert(userId, subscription, priceHistory);
                        priceChangesDetected++;
                        
                        log.info("Price increase detected for subscription {}: {} -> {} ({}%)",
                                subscription.getMerchantName(),
                                subscription.getPreviousAmount(),
                                subscription.getAmount(),
                                priceHistory.getPercentageChange());
                    }
                }
            }
        }

        if (priceChangesDetected > 0) {
            log.info("Detected {} significant price changes for user {}", priceChangesDetected, userId);
        }
    }

    private PriceHistory createPriceHistory(Subscription subscription) {
        PriceHistory history = new PriceHistory();
        history.setSubscriptionId(subscription.getId());
        history.setOldPrice(subscription.getPreviousAmount());
        history.setNewPrice(subscription.getAmount());
        
        BigDecimal priceChange = subscription.getAmount().subtract(subscription.getPreviousAmount());
        history.setPriceChange(priceChange);
        
        BigDecimal percentageChange = priceChange
                .divide(subscription.getPreviousAmount(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        history.setPercentageChange(percentageChange.doubleValue());
        
        history.setDetectedAt(LocalDateTime.now());
        
        return history;
    }

    public List<PriceHistoryDto> getPriceHistory(UUID subscriptionId) {
        return priceHistoryRepository.findBySubscriptionIdOrderByDetectedAtDesc(subscriptionId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<PriceHistoryDto> getUserPriceHistory(UUID userId) {
        List<UUID> subscriptionIds = subscriptionRepository.findByUserId(userId).stream()
                .map(Subscription::getId)
                .collect(Collectors.toList());

        return priceHistoryRepository.findBySubscriptionIdInOrderByDetectedAtDesc(subscriptionIds).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private PriceHistoryDto toDto(PriceHistory history) {
        return new PriceHistoryDto(
                history.getId(),
                history.getSubscriptionId(),
                history.getOldPrice(),
                history.getNewPrice(),
                history.getPriceChange(),
                history.getPercentageChange(),
                history.getDetectedAt()
        );
    }
}
