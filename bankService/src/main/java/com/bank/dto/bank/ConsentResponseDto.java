package com.bank.dto.bank;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO для ответа создания согласия
 * OpenBanking Russia: Response from POST /account-consents/request
 */
public record ConsentResponseDto(
        @JsonProperty("status")
        String status,          // "approved", "pending", "rejected"
        
        @JsonProperty("consent_id")
        String consentId,       // "consent-abc123"
        
        @JsonProperty("auto_approved")
        Boolean autoApproved    // true для VBank/ABank, false для SBank
) {
}

