package com.bank.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;

import java.util.UUID;

/**
 * Утилитный класс для работы с аутентификацией
 */
@Slf4j
public class AuthUtil {

    /**
     * Извлекает userId из Authentication
     * В JWT токене subject (email) используется как principal
     * TODO: В будущем добавить получение userId по email через authService API или добавить userId в JWT claims
     *
     * @param authentication объект Authentication из Spring Security
     * @return UUID пользователя
     */
    public static UUID extractUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("Authentication is missing or invalid");
        }

        // В JwtAuthFilter мы устанавливаем email как principal (authentication.getName())
        String email = authentication.getName();
        
        // TODO: Временное решение - преобразуем email в UUID детерминированным способом
        // В будущем нужно получать userId из authService по email или добавить userId в JWT claims
        // Для MVP используем UUID.nameUUIDFromBytes для создания детерминированного UUID из email
        UUID userId = UUID.nameUUIDFromBytes(("user:" + email).getBytes());
        
        log.debug("Extracted userId {} from email {}", userId, email);
        return userId;
    }
}
