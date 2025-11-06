package com.bank.config;

import java.time.format.DateTimeFormatter;

/**
 * Константы для банковского сервиса
 * Содержит все общие константы, используемые в приложении
 */
public final class BankingConstants {

    // Pagination
    public static final int DEFAULT_PAGE_SIZE = 50;
    public static final int MAX_PAGE_SIZE = 500;

    // Date/Time formatting
    public static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    // Client ID patterns
    public static final String CLIENT_ID_PATTERN = "^team\\d+-\\d+$";
    public static final String CLIENT_ID_PATTERN_DESCRIPTION = "team210-1 to team210-10";

    // Cache names
    public static final String ACCOUNTS_CACHE = "accounts";
    public static final String BANK_CONFIGS_CACHE = "bankConfigs";
    public static final String CONSENTS_CACHE = "consents";
    public static final String TOKENS_CACHE = "tokens";

    // Error messages
    public static final String BANK_NOT_FOUND_MESSAGE = "Unknown bank: ";
    public static final String CLIENT_ID_REQUIRED_MESSAGE = "clientId is required. Must be in format: " + CLIENT_ID_PATTERN_DESCRIPTION;
    public static final String INVALID_CLIENT_ID_FORMAT_MESSAGE = "Invalid clientId format. Must be like: team210-1";

    private BankingConstants() {
        // Utility class
    }
}
