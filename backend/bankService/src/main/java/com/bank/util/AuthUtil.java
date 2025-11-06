package com.bank.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
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
     * В JwtAuthFilter userId сохраняется в credentials
     *
     * @param authentication объект Authentication из Spring Security
     * @return UUID пользователя
     */
    public static UUID extractUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("Authentication is missing or invalid");
        }

        try {
            // В JwtAuthFilter userId сохраняется в credentials
            Object credentials = authentication.getCredentials();
            if (credentials instanceof String userIdStr) {
                UUID userId = UUID.fromString(userIdStr);
                log.debug("Extracted userId {} from JWT claims", userId);
                return userId;
            }

            // Fallback для обратной совместимости (старый способ)
            String email = authentication.getName();
            UUID userId = UUID.nameUUIDFromBytes(("user:" + email).getBytes());
            log.warn("Using fallback userId generation from email: {} -> {}", email, userId);
            return userId;

        } catch (Exception e) {
            log.error("Failed to extract userId from authentication", e);
            throw new IllegalStateException("Cannot extract userId from authentication", e);
        }
    }
}
