package com.bank.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CancellationRequestDto(
        @NotBlank(message = "Reason is required")
        String reason,
        String feedback
) {
}

