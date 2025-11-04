package com.bank.dto.bank;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO для запроса создания согласия на доступ к счетам
 * OpenBanking Russia: POST /account-consents/request
 */
public record ConsentRequestDto(
        @JsonProperty("client_id")
        String clientId,
        
        @JsonProperty("permissions")
        List<String> permissions,
        
        @JsonProperty("reason")
        String reason,
        
        @JsonProperty("requesting_bank")
        String requestingBank,
        
        @JsonProperty("requesting_bank_name")
        String requestingBankName
) {
    public static ConsentRequestDto createDefault(String clientId, String requestingBank) {
        return new ConsentRequestDto(
                clientId,
                List.of("ReadAccountsDetail", "ReadBalances", "ReadTransactionsDetail"),
                "Агрегация счетов для RadarSubscriptions",
                requestingBank,
                requestingBank + " App"
        );
    }
}

