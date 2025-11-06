package com.bank.dto.request;

import com.bank.config.BankingConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO для запроса добавления нового банка
 * Пользователь вводит логин и пароль через UI
 */
public record AddBankRequestDto(
        @NotBlank(message = "bankId is required")
        String bankId,

        @NotBlank(message = "username is required")
        String username,

        @NotBlank(message = "password is required")
        String password,

        // Обязательный client_id (team001-1 до team300-10)
        // Должен соответствовать формату teamXXX-Y
        @NotBlank(message = "clientId is required")
        @Pattern(regexp = BankingConstants.CLIENT_ID_PATTERN,
                message = BankingConstants.INVALID_CLIENT_ID_FORMAT_MESSAGE)
        String clientId
) {
}
