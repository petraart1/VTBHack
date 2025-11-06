package com.bank.dto.request;

import com.bank.config.BankingConstants;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/**
 * DTO для запроса создания согласия на доступ к счетам
 * OpenBanking Russia: POST /account-consents/request
 */
public record ConsentRequestDto(
        @JsonProperty("client_id")
        @NotBlank(message = BankingConstants.CLIENT_ID_REQUIRED_MESSAGE)
        @Pattern(regexp = BankingConstants.CLIENT_ID_PATTERN,
                message = BankingConstants.INVALID_CLIENT_ID_FORMAT_MESSAGE)
        String clientId,

        @JsonProperty("permissions")
        @NotEmpty(message = "permissions cannot be empty")
        List<String> permissions,

        @JsonProperty("reason")
        @NotBlank(message = "reason is required")
        String reason,

        @JsonProperty("requesting_bank")
        @NotBlank(message = "requesting_bank is required")
        String requestingBank,

        @JsonProperty("requesting_bank_name")
        @NotBlank(message = "requesting_bank_name is required")
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
