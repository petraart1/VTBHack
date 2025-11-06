package com.bank.dto.common;

import java.util.UUID;

/**
 * Учетные данные пользователя для доступа к банку
 * НЕ хранятся в базе, существуют только в runtime
 */
public record BankCredentials(
        UUID userId,
        String bankId,
        String username,
        String password,
        String clientId  // Предопределенный client_id (team210-1 до team210-10), может быть null
) {
    /**
     * Конструктор без clientId (для обратной совместимости)
     */
    public BankCredentials(UUID userId, String bankId, String username, String password) {
        this(userId, bankId, username, password, null);
    }
}
