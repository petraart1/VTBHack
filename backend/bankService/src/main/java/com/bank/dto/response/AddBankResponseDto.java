package com.bank.dto.response;

/**
 * DTO для ответа при добавлении банка
 */
public record AddBankResponseDto(
        String bankId,
        String bankName,
        boolean success,
        String message
) {
}
