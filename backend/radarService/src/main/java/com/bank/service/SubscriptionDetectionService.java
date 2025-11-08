package com.bank.service;

import com.bank.client.BankServiceClient;
import com.bank.dto.response.DetectionResultDto;
import com.bank.dto.response.SubscriptionDto;
import com.bank.dto.response.TransactionDto;
import com.bank.model.*;
import com.bank.repository.SubscriptionRepository;
import com.bank.repository.TransactionPatternRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionDetectionService {

    private final BankServiceClient bankServiceClient;
    private final SubscriptionRepository subscriptionRepository;
    private final TransactionPatternRepository patternRepository;
    private final AlertService alertService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Минимальное количество повторяющихся платежей для определения подписки
    private static final int MIN_RECURRING_PAYMENTS = 3;
    
    // Максимальное отклонение суммы (5%)
    private static final double MAX_AMOUNT_DEVIATION = 0.05;

    /**
     * Запускает полное обнаружение подписок для пользователя
     */
    @Transactional
    public DetectionResultDto detectSubscriptions(UUID userId) {
        log.info("Starting subscription detection for user: {}", userId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Получить JWT токен из контекста
            String jwtToken = extractJwtToken();
            
            // Получить транзакции за последние 12 месяцев
            LocalDateTime from = LocalDateTime.now().minusMonths(12);
            LocalDateTime to = LocalDateTime.now();
            List<TransactionDto> transactions = bankServiceClient.getTransactions(userId, from, to, jwtToken);
            
            if (transactions.isEmpty()) {
                log.info("No transactions found for user: {}", userId);
                return new DetectionResultDto(0, 0, 0, System.currentTimeMillis() - startTime, Collections.emptyList());
            }

            // Анализировать паттерны
            Map<String, List<TransactionDto>> groupedByMerchant = groupTransactionsByMerchant(transactions);
            
            int newSubscriptions = 0;
            int confirmedSubscriptions = 0;
            int priceChanges = 0;
            List<SubscriptionDto> newSubs = new ArrayList<>();

            // Для каждого мерчанта ищем повторяющиеся платежи
            for (Map.Entry<String, List<TransactionDto>> entry : groupedByMerchant.entrySet()) {
                String merchantName = entry.getKey();
                List<TransactionDto> merchantTransactions = entry.getValue();
                
                if (merchantTransactions.size() < MIN_RECURRING_PAYMENTS) {
                    continue;
                }

                merchantTransactions.sort(Comparator.comparing(TransactionDto::bookingDateTime));

                TransactionPattern pattern = analyzePattern(userId, merchantName, merchantTransactions);
                
                if (pattern != null) {
                    patternRepository.save(pattern);
                    
                    Optional<Subscription> existingSubscription = subscriptionRepository
                            .findByUserIdAndMerchantNameIgnoreCase(userId, merchantName);

                    if (existingSubscription.isPresent()) {
                        Subscription subscription = existingSubscription.get();
                        boolean priceChanged = updateSubscription(subscription, pattern, merchantTransactions);
                        subscriptionRepository.save(subscription);
                        confirmedSubscriptions++;
                        if (priceChanged) {
                            priceChanges++;
                        }
                    } else {
                        Subscription newSub = createSubscription(userId, pattern, merchantTransactions);
                        subscriptionRepository.save(newSub);
                        
                        alertService.createNewSubscriptionAlert(userId, newSub);
                        
                        newSubscriptions++;
                        newSubs.add(toDto(newSub));
                    }
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Detection completed for user {}: new={}, confirmed={}, priceChanges={}", 
                    userId, newSubscriptions, confirmedSubscriptions, priceChanges);

            return new DetectionResultDto(newSubscriptions, confirmedSubscriptions, priceChanges, duration, newSubs);
            
        } catch (Exception e) {
            log.error("Error detecting subscriptions for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to detect subscriptions", e);
        }
    }

    private String extractJwtToken() {
        // Извлекаем токен из Security Context или возвращаем пустую строку для тестов
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getCredentials() != null) {
                return auth.getCredentials().toString();
            }
        } catch (Exception e) {
            log.warn("Could not extract JWT token: {}", e.getMessage());
        }
        return "";
    }

    private Map<String, List<TransactionDto>> groupTransactionsByMerchant(List<TransactionDto> transactions) {
        return transactions.stream()
                .filter(t -> "DEBIT".equalsIgnoreCase(t.debitCreditIndicator()))
                .filter(t -> t.merchantName() != null && !t.merchantName().isBlank())
                .collect(Collectors.groupingBy(
                        t -> normalizeMerchantName(t.merchantName()),
                        Collectors.toList()
                ));
    }

    private String normalizeMerchantName(String merchantName) {
        return merchantName.trim()
                .replaceAll("\\s+", " ")
                .toUpperCase();
    }

    private TransactionPattern analyzePattern(UUID userId, String merchantName, List<TransactionDto> transactions) {
        if (transactions.size() < MIN_RECURRING_PAYMENTS) {
            return null;
        }

        List<BigDecimal> amounts = transactions.stream()
                .map(TransactionDto::amount)
                .collect(Collectors.toList());

        BigDecimal avgAmount = amounts.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(amounts.size()), 2, RoundingMode.HALF_UP);

        double stdDevAmount = calculateStandardDeviation(amounts, avgAmount);

        List<Long> daysBetween = new ArrayList<>();
        for (int i = 1; i < transactions.size(); i++) {
            long days = ChronoUnit.DAYS.between(
                    transactions.get(i - 1).bookingDateTime(),
                    transactions.get(i).bookingDateTime()
            );
            daysBetween.add(days);
        }

        double avgDaysBetween = daysBetween.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);

        double stdDevDays = calculateStandardDeviation(
                daysBetween.stream().map(BigDecimal::valueOf).collect(Collectors.toList()),
                BigDecimal.valueOf(avgDaysBetween)
        );

        if (stdDevAmount / avgAmount.doubleValue() > MAX_AMOUNT_DEVIATION ||
            stdDevDays / avgDaysBetween > 0.3) {
            return null;
        }

        TransactionPattern pattern = new TransactionPattern();
        pattern.setUserId(userId);
        pattern.setMerchantName(merchantName);
        pattern.setMerchantCategory(transactions.get(0).merchantCategoryCode());
        pattern.setOccurrenceCount(transactions.size());
        pattern.setAverageAmount(avgAmount);
        pattern.setStdDeviation(BigDecimal.valueOf(stdDevAmount));
        pattern.setAverageDaysBetween((int) Math.round(avgDaysBetween));
        pattern.setStdDevDays((int) Math.round(stdDevDays));
        
        // Сохраняем даты как JSON
        List<String> paymentDates = transactions.stream()
                .map(t -> t.bookingDateTime().toString())
                .sorted(Comparator.reverseOrder())
                .limit(10)
                .collect(Collectors.toList());
        try {
            pattern.setRecentPaymentDates(objectMapper.writeValueAsString(paymentDates));
        } catch (JsonProcessingException e) {
            log.warn("Could not serialize payment dates: {}", e.getMessage());
            pattern.setRecentPaymentDates("[]");
        }
        
        pattern.setFirstSeen(transactions.get(0).bookingDateTime());
        pattern.setLastSeen(transactions.get(transactions.size() - 1).bookingDateTime());
        pattern.setLastAnalyzed(LocalDateTime.now());

        return pattern;
    }

    private Subscription createSubscription(UUID userId, TransactionPattern pattern, List<TransactionDto> transactions) {
        Subscription subscription = new Subscription();
        subscription.setUserId(userId);
        subscription.setAccountId(transactions.get(0).accountId());
        subscription.setMerchantName(pattern.getMerchantName());
        subscription.setCategory(determineCategory(pattern.getMerchantCategory()));
        subscription.setAmount(pattern.getAverageAmount());
        subscription.setCurrency(transactions.get(0).currency());
        subscription.setFrequency(determineFrequency(pattern.getAverageDaysBetween()));
        subscription.setStatus(Subscription.SubscriptionStatus.DETECTED);
        subscription.setFirstPaymentDate(pattern.getFirstSeen());
        subscription.setLastPaymentDate(pattern.getLastSeen());
        subscription.setNextExpectedDate(calculateNextExpectedDate(pattern));
        subscription.setTotalPayments(pattern.getOccurrenceCount());
        
        BigDecimal totalSpent = transactions.stream()
                .map(TransactionDto::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        subscription.setTotalSpent(totalSpent);
        
        subscription.setPriceIncreased(false);
        subscription.setConfidenceScore(calculateConfidenceScore(pattern));

        return subscription;
    }

    private boolean updateSubscription(Subscription subscription, TransactionPattern pattern, List<TransactionDto> transactions) {
        subscription.setLastPaymentDate(pattern.getLastSeen());
        subscription.setNextExpectedDate(calculateNextExpectedDate(pattern));
        subscription.setTotalPayments(pattern.getOccurrenceCount());
        
        BigDecimal totalSpent = transactions.stream()
                .map(TransactionDto::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        subscription.setTotalSpent(totalSpent);
        
        boolean priceChanged = false;
        if (subscription.getAmount().compareTo(pattern.getAverageAmount()) != 0) {
            subscription.setPreviousAmount(subscription.getAmount());
            subscription.setAmount(pattern.getAverageAmount());
            subscription.setPriceIncreased(true);
            priceChanged = true;
        }
        
        subscription.setConfidenceScore(calculateConfidenceScore(pattern));
        
        if (subscription.getStatus() == Subscription.SubscriptionStatus.DETECTED && subscription.getConfidenceScore() >= 0.9) {
            subscription.setStatus(Subscription.SubscriptionStatus.CONFIRMED);
        }
        
        return priceChanged;
    }

    private SubscriptionCategory determineCategory(String mcc) {
        if (mcc == null) {
            return SubscriptionCategory.OTHER;
        }
        
        return switch (mcc) {
            case "5815", "5816", "5817" -> SubscriptionCategory.DIGITAL_SERVICES;
            case "4899", "4900" -> SubscriptionCategory.COMMUNICATION;
            case "5732", "5733" -> SubscriptionCategory.SOFTWARE;
            case "7929", "7941" -> SubscriptionCategory.ENTERTAINMENT;
            case "5812", "5813", "5814" -> SubscriptionCategory.FOOD_DELIVERY;
            default -> SubscriptionCategory.OTHER;
        };
    }

    private SubscriptionFrequency determineFrequency(Integer avgDays) {
        if (avgDays <= 8) {
            return SubscriptionFrequency.WEEKLY;
        } else if (avgDays <= 31) {
            return SubscriptionFrequency.MONTHLY;
        } else if (avgDays <= 92) {
            return SubscriptionFrequency.QUARTERLY;
        } else if (avgDays <= 185) {
            return SubscriptionFrequency.SEMI_ANNUALLY;
        } else {
            return SubscriptionFrequency.ANNUALLY;
        }
    }

    private LocalDateTime calculateNextExpectedDate(TransactionPattern pattern) {
        return pattern.getLastSeen().plusDays(pattern.getAverageDaysBetween());
    }

    private double calculateConfidenceScore(TransactionPattern pattern) {
        double score = 1.0;
        
        if (pattern.getOccurrenceCount() < 4) {
            score -= 0.2;
        }
        
        double amountVariation = pattern.getStdDeviation().doubleValue() / pattern.getAverageAmount().doubleValue();
        score -= amountVariation * 2;
        
        double periodVariation = pattern.getStdDevDays() / (double) pattern.getAverageDaysBetween();
        score -= periodVariation;
        
        return Math.max(0.0, Math.min(1.0, score));
    }

    private double calculateStandardDeviation(List<BigDecimal> values, BigDecimal mean) {
        if (values.size() < 2) {
            return 0.0;
        }
        
        double sumSquaredDiff = values.stream()
                .map(v -> v.subtract(mean).pow(2))
                .map(BigDecimal::doubleValue)
                .mapToDouble(Double::doubleValue)
                .sum();
        
        return Math.sqrt(sumSquaredDiff / values.size());
    }

    @Cacheable(value = "userSubscriptions", key = "#userId")
    public List<SubscriptionDto> getUserSubscriptions(UUID userId) {
        return subscriptionRepository.findByUserId(userId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<SubscriptionDto> getActiveSubscriptions(UUID userId) {
        return subscriptionRepository.findByUserIdAndStatusIn(
                userId, 
                List.of(Subscription.SubscriptionStatus.DETECTED, Subscription.SubscriptionStatus.CONFIRMED)
        ).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private SubscriptionDto toDto(Subscription subscription) {
        return SubscriptionDto.from(subscription);
    }
}
