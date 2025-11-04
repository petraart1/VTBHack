package com.bank.dto.bank;

import jakarta.validation.constraints.NotBlank;

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
        
        // Опциональный client_id (team210-1 до team210-10)
        // Если не передан, будет использоваться дефолтное значение team210-1
        String clientId
) {
}

