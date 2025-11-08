package com.bank.service;

import com.bank.dto.response.AlternativeDto;
import com.bank.model.Subscription;
import com.bank.model.SubscriptionAlternative;
import com.bank.model.SubscriptionFrequency;
import com.bank.repository.SubscriptionAlternativeRepository;
import com.bank.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlternativeService {

    private final SubscriptionAlternativeRepository alternativeRepository;
    private final SubscriptionRepository subscriptionRepository;

    private static final Map<String, List<AlternativeData>> ALTERNATIVES_DB = new HashMap<>();

    static {
        ALTERNATIVES_DB.put("SPOTIFY", List.of(
                new AlternativeData("Яндекс Музыка", "Российский сервис потоковой музыки с большой библиотекой", 
                        BigDecimal.valueOf(199), "RUB", 4),
                new AlternativeData("VK Музыка", "Бесплатный доступ к музыке для пользователей VK", 
                        BigDecimal.valueOf(0), "RUB", 4),
                new AlternativeData("Apple Music", "Музыкальный сервис от Apple с качественным звуком", 
                        BigDecimal.valueOf(169), "RUB", 5)
        ));

        ALTERNATIVES_DB.put("NETFLIX", List.of(
                new AlternativeData("Кинопоиск HD", "Российский видеосервис с большой библиотекой фильмов и сериалов", 
                        BigDecimal.valueOf(399), "RUB", 4),
                new AlternativeData("IVI", "Российский онлайн-кинотеатр с подпиской", 
                        BigDecimal.valueOf(299), "RUB", 4),
                new AlternativeData("OKKO", "Видеосервис от Сбера", 
                        BigDecimal.valueOf(349), "RUB", 4)
        ));

        ALTERNATIVES_DB.put("DROPBOX", List.of(
                new AlternativeData("Яндекс Диск", "Облачное хранилище от Яндекса с 10 ГБ бесплатно", 
                        BigDecimal.valueOf(149), "RUB", 4),
                new AlternativeData("Google Drive", "Облачное хранилище от Google", 
                        BigDecimal.valueOf(159), "RUB", 5),
                new AlternativeData("Облако Mail.ru", "Российское облачное хранилище", 
                        BigDecimal.valueOf(99), "RUB", 4)
        ));

        ALTERNATIVES_DB.put("VPN", List.of(
                new AlternativeData("Surfshark", "Недорогой VPN с неограниченным количеством устройств", 
                        BigDecimal.valueOf(299), "RUB", 4),
                new AlternativeData("ProtonVPN", "Безопасный VPN от создателей ProtonMail", 
                        BigDecimal.valueOf(399), "RUB", 5),
                new AlternativeData("Mullvad VPN", "Анонимный VPN без привязки к email", 
                        BigDecimal.valueOf(349), "RUB", 4)
        ));
    }

    @Transactional
    public List<AlternativeDto> findAlternatives(UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        log.info("Finding alternatives for subscription: {}", subscription.getMerchantName());

        List<SubscriptionAlternative> existingAlternatives = 
                alternativeRepository.findBySubscriptionIdOrderBySavingsDesc(subscriptionId);

        if (!existingAlternatives.isEmpty()) {
            return existingAlternatives.stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
        }

        List<SubscriptionAlternative> newAlternatives = generateAlternatives(subscription);
        
        if (!newAlternatives.isEmpty()) {
            alternativeRepository.saveAll(newAlternatives);
        }

        return newAlternatives.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private List<SubscriptionAlternative> generateAlternatives(Subscription subscription) {
        String merchantNameUpper = subscription.getMerchantName().toUpperCase();
        
        for (Map.Entry<String, List<AlternativeData>> entry : ALTERNATIVES_DB.entrySet()) {
            if (merchantNameUpper.contains(entry.getKey())) {
                return entry.getValue().stream()
                        .map(data -> createAlternative(subscription, data))
                        .filter(alt -> alt.getSavings().compareTo(BigDecimal.ZERO) > 0)
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

    private SubscriptionAlternative createAlternative(Subscription subscription, AlternativeData data) {
        SubscriptionAlternative alternative = new SubscriptionAlternative();
        alternative.setSubscriptionId(subscription.getId());
        alternative.setAlternativeName(data.name);
        alternative.setDescription(data.description);
        alternative.setPrice(data.price);
        alternative.setCurrency(data.currency);
        alternative.setFrequency(subscription.getFrequency().name());
        
        BigDecimal savings = subscription.getAmount().subtract(data.price);
        alternative.setSavings(savings);
        
        alternative.setReferralLink("https://example.com/" + data.name.toLowerCase().replace(" ", "-"));
        alternative.setRating(data.rating);

        return alternative;
    }

    @Cacheable(value = "bestAlternative", key = "#subscriptionId")
    public Optional<AlternativeDto> getBestAlternative(UUID subscriptionId) {
        return alternativeRepository.findTopBySubscriptionIdOrderBySavingsDesc(subscriptionId)
                .map(this::toDto);
    }

    private AlternativeDto toDto(SubscriptionAlternative alternative) {
        return new AlternativeDto(
                alternative.getId(),
                alternative.getSubscriptionId(),
                alternative.getAlternativeName(),
                alternative.getDescription(),
                alternative.getPrice(),
                alternative.getCurrency(),
                alternative.getFrequency(),
                alternative.getSavings(),
                alternative.getReferralLink(),
                alternative.getRating()
        );
    }

    private record AlternativeData(
            String name,
            String description,
            BigDecimal price,
            String currency,
            Integer rating
    ) {}
}
