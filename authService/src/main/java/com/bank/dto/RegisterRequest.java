package com.bank.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username must not be empty")
        @Size(min = 1, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @NotBlank(message = "Email must not be empty")
        @Email(message = "Invalid email")
        String email,

        @NotBlank(message = "Password must not be empty")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String password
) {
}
