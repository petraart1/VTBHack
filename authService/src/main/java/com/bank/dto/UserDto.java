package com.bank.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record UserDto(
        Long id,
        String username,
        String email,
        @JsonProperty("created_at") LocalDateTime createdAt
) {
}
