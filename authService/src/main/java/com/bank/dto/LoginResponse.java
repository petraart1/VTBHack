package com.bank.dto;

public record LoginResponse(
        Long id,
        String username,
        String email,
        String accessToken,
        String tokenType
) {
}
