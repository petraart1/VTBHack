package com.bank.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Конфигурационные свойства для банков
 * Загружаются из application.yaml
 */
@Data
@Component
@ConfigurationProperties(prefix = "bank")
public class BankProperties {

    /**
     * Маппинг bankId -> BankConfig
     * Пример:
     * bank:
     *   configs:
     *     vbank:
     *       name: "VBank"
     *       baseUrl: "https://vbank.open.bankingapi.ru"
     *       tokenEndpoint: "/oauth2/token"
     *       accountsEndpoint: "/accounts"
     */
    private Map<String, BankConfig> configs;

    /**
     * Конфигурация отдельного банка
     */
    @Data
    public static class BankConfig {
        private String name;
        private String baseUrl;
        private String tokenEndpoint;
        private String accountsEndpoint;
        private String transactionsEndpoint;
        private String balancesEndpoint;
        
        /**
         * Timeout для запросов к банку (в миллисекундах)
         * По умолчанию 10 секунд
         */
        private int connectTimeout = 10000;
        
        /**
         * Timeout для чтения ответа (в миллисекундах)
         * По умолчанию 30 секунд
         */
        private int readTimeout = 30000;
    }
}

