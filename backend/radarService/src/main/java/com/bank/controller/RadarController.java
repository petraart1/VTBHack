package com.bank.controller;

import com.bank.dto.request.CancellationRequestDto;
import com.bank.dto.response.*;
import com.bank.service.*;
import com.bank.util.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/radar")
@RequiredArgsConstructor
@Tag(name = "Radar", description = "API для управления подписками и мониторинга")
@SecurityRequirement(name = "Bearer Authentication")
public class RadarController {

    private final SubscriptionDetectionService detectionService;
    private final PriceMonitoringService priceMonitoringService;
    private final AlertService alertService;
    private final AlternativeService alternativeService;
    private final SubscriptionManagementService subscriptionManagementService;

    /**
     * Запускает обнаружение подписок для текущего пользователя
     */
    @PostMapping("/detect")
    @Operation(summary = "Обнаружить подписки", 
            description = "Запускает процесс обнаружения подписок на основе транзакций пользователя")
    public ResponseEntity<DetectionResultDto> detectSubscriptions(Authentication authentication) {
        UUID userId = AuthUtil.extractUserIdFromAuthentication(authentication);
        log.info("User {} requested subscription detection", userId);
        
        DetectionResultDto result = detectionService.detectSubscriptions(userId);
        return ResponseEntity.ok(result);
    }

    /**
     * Получает все подписки пользователя
     */
    @GetMapping("/subscriptions")
    @Operation(summary = "Получить все подписки", 
            description = "Возвращает список всех подписок пользователя")
    public ResponseEntity<List<SubscriptionDto>> getSubscriptions(Authentication authentication) {
        UUID userId = AuthUtil.extractUserIdFromAuthentication(authentication);
        log.info("User {} requested all subscriptions", userId);
        
        List<SubscriptionDto> subscriptions = detectionService.getUserSubscriptions(userId);
        return ResponseEntity.ok(subscriptions);
    }

    /**
     * Получает активные подписки пользователя
     */
    @GetMapping("/subscriptions/active")
    @Operation(summary = "Получить активные подписки", 
            description = "Возвращает список активных подписок пользователя")
    public ResponseEntity<List<SubscriptionDto>> getActiveSubscriptions(Authentication authentication) {
        UUID userId = AuthUtil.extractUserIdFromAuthentication(authentication);
        log.info("User {} requested active subscriptions", userId);
        
        List<SubscriptionDto> subscriptions = detectionService.getActiveSubscriptions(userId);
        return ResponseEntity.ok(subscriptions);
    }

    /**
     * Получает детали конкретной подписки
     */
    @GetMapping("/subscriptions/{subscriptionId}")
    @Operation(summary = "Получить детали подписки", 
            description = "Возвращает детальную информацию о подписке")
    public ResponseEntity<SubscriptionDto> getSubscriptionDetails(
            Authentication authentication,
            @PathVariable UUID subscriptionId) {
        UUID userId = AuthUtil.extractUserIdFromAuthentication(authentication);
        log.info("User {} requested subscription details: {}", userId, subscriptionId);
        
        SubscriptionDto subscription = subscriptionManagementService.getSubscriptionDetails(userId, subscriptionId);
        return ResponseEntity.ok(subscription);
    }

    /**
     * Подтверждает обнаруженную подписку
     */
    @PostMapping("/subscriptions/{subscriptionId}/confirm")
    @Operation(summary = "Подтвердить подписку", 
            description = "Подтверждает обнаруженную подписку")
    public ResponseEntity<SubscriptionDto> confirmSubscription(
            Authentication authentication,
            @PathVariable UUID subscriptionId) {
        UUID userId = AuthUtil.extractUserIdFromAuthentication(authentication);
        log.info("User {} confirming subscription: {}", userId, subscriptionId);
        
        SubscriptionDto subscription = subscriptionManagementService.confirmSubscription(userId, subscriptionId);
        return ResponseEntity.ok(subscription);
    }

    /**
     * Отменяет подписку
     */
    @PostMapping("/subscriptions/{subscriptionId}/cancel")
    @Operation(summary = "Отменить подписку", 
            description = "Помечает подписку как отмененную")
    public ResponseEntity<Void> cancelSubscription(
            Authentication authentication,
            @PathVariable UUID subscriptionId,
            @RequestBody(required = false) CancellationRequestDto request) {
        UUID userId = AuthUtil.extractUserIdFromAuthentication(authentication);
        log.info("User {} cancelling subscription: {}", userId, subscriptionId);
        
        String reason = request != null ? request.reason() : null;
        subscriptionManagementService.cancelSubscription(userId, subscriptionId, reason);
        
        return ResponseEntity.ok().build();
    }

    /**
     * Получает статистику по подпискам
     */
    @GetMapping("/subscriptions/stats")
    @Operation(summary = "Получить статистику подписок", 
            description = "Возвращает статистику по всем подпискам пользователя")
    public ResponseEntity<SubscriptionStatsDto> getSubscriptionStats(Authentication authentication) {
        UUID userId = AuthUtil.extractUserIdFromAuthentication(authentication);
        log.info("User {} requested subscription stats", userId);
        
        SubscriptionStatsDto stats = subscriptionManagementService.getSubscriptionStats(userId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Получает историю изменения цен для подписки
     */
    @GetMapping("/subscriptions/{subscriptionId}/price-history")
    @Operation(summary = "Получить историю цен", 
            description = "Возвращает историю изменения цен для конкретной подписки")
    public ResponseEntity<List<PriceHistoryDto>> getPriceHistory(
            Authentication authentication,
            @PathVariable UUID subscriptionId) {
        UUID userId = AuthUtil.extractUserIdFromAuthentication(authentication);
        log.info("User {} requested price history for subscription: {}", userId, subscriptionId);
        
        List<PriceHistoryDto> history = priceMonitoringService.getPriceHistory(subscriptionId);
        return ResponseEntity.ok(history);
    }

    /**
     * Получает все изменения цен пользователя
     */
    @GetMapping("/price-history")
    @Operation(summary = "Получить всю историю цен", 
            description = "Возвращает историю изменения цен по всем подпискам пользователя")
    public ResponseEntity<List<PriceHistoryDto>> getUserPriceHistory(Authentication authentication) {
        UUID userId = AuthUtil.extractUserIdFromAuthentication(authentication);
        log.info("User {} requested all price history", userId);
        
        List<PriceHistoryDto> history = priceMonitoringService.getUserPriceHistory(userId);
        return ResponseEntity.ok(history);
    }

    /**
     * Получает альтернативы для подписки
     */
    @GetMapping("/subscriptions/{subscriptionId}/alternatives")
    @Operation(summary = "Получить альтернативы", 
            description = "Возвращает список альтернативных сервисов для подписки")
    public ResponseEntity<List<AlternativeDto>> getAlternatives(
            Authentication authentication,
            @PathVariable UUID subscriptionId) {
        UUID userId = AuthUtil.extractUserIdFromAuthentication(authentication);
        log.info("User {} requested alternatives for subscription: {}", userId, subscriptionId);
        
        List<AlternativeDto> alternatives = alternativeService.findAlternatives(subscriptionId);
        return ResponseEntity.ok(alternatives);
    }

    /**
     * Получает все уведомления пользователя
     */
    @GetMapping("/alerts")
    @Operation(summary = "Получить все уведомления", 
            description = "Возвращает список всех уведомлений пользователя")
    public ResponseEntity<List<AlertDto>> getAllAlerts(Authentication authentication) {
        UUID userId = AuthUtil.extractUserIdFromAuthentication(authentication);
        log.info("User {} requested all alerts", userId);
        
        List<AlertDto> alerts = alertService.getAllAlerts(userId);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Получает непрочитанные уведомления
     */
    @GetMapping("/alerts/unread")
    @Operation(summary = "Получить непрочитанные уведомления", 
            description = "Возвращает список непрочитанных уведомлений пользователя")
    public ResponseEntity<List<AlertDto>> getUnreadAlerts(Authentication authentication) {
        UUID userId = AuthUtil.extractUserIdFromAuthentication(authentication);
        log.info("User {} requested unread alerts", userId);
        
        List<AlertDto> alerts = alertService.getUnreadAlerts(userId);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Помечает уведомление как прочитанное
     */
    @PutMapping("/alerts/{alertId}/read")
    @Operation(summary = "Пометить уведомление как прочитанное", 
            description = "Помечает конкретное уведомление как прочитанное")
    public ResponseEntity<Void> markAlertAsRead(
            Authentication authentication,
            @PathVariable UUID alertId) {
        UUID userId = AuthUtil.extractUserIdFromAuthentication(authentication);
        log.info("User {} marking alert {} as read", userId, alertId);
        
        alertService.markAsRead(userId, alertId);
        return ResponseEntity.ok().build();
    }

    /**
     * Помечает все уведомления как прочитанные
     */
    @PutMapping("/alerts/read-all")
    @Operation(summary = "Пометить все уведомления как прочитанные", 
            description = "Помечает все уведомления пользователя как прочитанные")
    public ResponseEntity<Void> markAllAlertsAsRead(Authentication authentication) {
        UUID userId = AuthUtil.extractUserIdFromAuthentication(authentication);
        log.info("User {} marking all alerts as read", userId);
        
        alertService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Удаляет уведомление
     */
    @DeleteMapping("/alerts/{alertId}")
    @Operation(summary = "Удалить уведомление", 
            description = "Удаляет конкретное уведомление")
    public ResponseEntity<Void> deleteAlert(
            Authentication authentication,
            @PathVariable UUID alertId) {
        UUID userId = AuthUtil.extractUserIdFromAuthentication(authentication);
        log.info("User {} deleting alert {}", userId, alertId);
        
        alertService.deleteAlert(userId, alertId);
        return ResponseEntity.ok().build();
    }
}

