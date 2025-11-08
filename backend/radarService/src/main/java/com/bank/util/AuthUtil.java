package com.bank.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public class AuthUtil {

    private AuthUtil() {
        // Utility class
    }

    /**
     * Извлекает userId из JWT токена
     */
    public static UUID extractUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new RuntimeException("Authentication is required");
        }

        Object principal = authentication.getPrincipal();
        
        if (principal instanceof Jwt jwt) {
            String userIdStr = jwt.getClaimAsString("userId");
            if (userIdStr == null || userIdStr.isBlank()) {
                throw new RuntimeException("userId claim not found in JWT");
            }
            
            try {
                return UUID.fromString(userIdStr);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid userId format in JWT: " + userIdStr, e);
            }
        }
        
        throw new RuntimeException("Unsupported principal type: " + principal.getClass().getName());
    }
}

