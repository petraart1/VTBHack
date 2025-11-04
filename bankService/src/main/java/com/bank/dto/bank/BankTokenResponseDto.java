package com.bank.dto.bank;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO для ответа OAuth2 token endpoint внешнего банка
 * Соответствует стандарту RFC 6749
 */
public record BankTokenResponseDto(
        @JsonProperty("access_token")
        String accessToken,
        
        @JsonProperty("token_type")
        String tokenType,
        
        @JsonProperty("expires_in")
        Long expiresIn,
        
        @JsonProperty("refresh_token")
        String refreshToken,
        
        @JsonProperty("scope")
        String scope
) {
}

