package com.bank.scheduler;

import com.bank.repository.SubscriptionRepository;
import com.bank.service.PriceMonitoringService;
import com.bank.service.SubscriptionDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {

    private final SubscriptionDetectionService detectionService;
    private final PriceMonitoringService priceMonitoringService;
    private final SubscriptionRepository subscriptionRepository;

    /**
     * Автоматическое обнаружение подписок для всех пользователей
     * Запускается каждую ночь в 02:00
     */
    @Scheduled(cron = "${radar.scheduler.detection-cron:0 0 2 * * *}")
    public void scheduledSubscriptionDetection() {
        log.info("Starting scheduled subscription detection for all users");
        
        try {
            List<UUID> activeUsers = subscriptionRepository.findAllActiveUsers();
            log.info("Found {} active users for subscription detection", activeUsers.size());
            
            int successCount = 0;
            int errorCount = 0;
            
            for (UUID userId : activeUsers) {
                try {
                    detectionService.detectSubscriptions(userId);
                    successCount++;
                    
                    // Небольшая задержка между пользователями чтобы не перегружать bankService
                    TimeUnit.MILLISECONDS.sleep(500);
                    
                } catch (Exception e) {
                    log.error("Failed to detect subscriptions for user {}: {}", userId, e.getMessage(), e);
                    errorCount++;
                }
            }
            
            log.info("Scheduled subscription detection completed: success={}, errors={}", successCount, errorCount);
            
        } catch (Exception e) {
            log.error("Scheduled subscription detection failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Мониторинг изменения цен для всех пользователей
     * Запускается каждые 6 часов
     */
    @Scheduled(fixedDelayString = "${radar.scheduler.price-monitoring-interval:21600000}", 
               initialDelayString = "${radar.scheduler.price-monitoring-initial-delay:60000}")
    public void scheduledPriceMonitoring() {
        log.info("Starting scheduled price monitoring for all users");
        
        try {
            List<UUID> activeUsers = subscriptionRepository.findAllActiveUsers();
            log.info("Found {} active users for price monitoring", activeUsers.size());
            
            int successCount = 0;
            int errorCount = 0;
            
            for (UUID userId : activeUsers) {
                try {
                    priceMonitoringService.checkPriceChanges(userId);
                    successCount++;
                    
                } catch (Exception e) {
                    log.error("Failed to check price changes for user {}: {}", userId, e.getMessage(), e);
                    errorCount++;
                }
            }
            
            log.info("Scheduled price monitoring completed: success={}, errors={}", successCount, errorCount);
            
        } catch (Exception e) {
            log.error("Scheduled price monitoring failed: {}", e.getMessage(), e);
        }
    }
}

