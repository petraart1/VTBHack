package com.bank.dto.request;

import com.bank.config.BankingConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO для запроса синхронизации банка или создания согласия
 */
public record BankSyncRequestDto(
        @NotBlank(message = "username is required")
        String username,

        @NotBlank(message = "password is required")
        String password,

        @NotBlank(message = BankingConstants.CLIENT_ID_REQUIRED_MESSAGE)
        @Pattern(regexp = BankingConstants.CLIENT_ID_PATTERN,
                message = BankingConstants.INVALID_CLIENT_ID_FORMAT_MESSAGE)
        String clientId,  // client_id из предопределенного списка (team210-1 до team210-10)

        String consentId  // опциональный - для тестирования/отладки с существующим согласием
) {
}
