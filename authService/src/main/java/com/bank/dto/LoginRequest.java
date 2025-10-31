package com.bank.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Size(min = 1, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @Email(message = "Invalid email")
        String email,

        @NotBlank(message = "Password must not be empty")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String password
) {
    public LoginRequest {
        boolean hasUsername = username != null && !username.isBlank();
        boolean hasEmail = email != null && !email.isBlank();

        if (!hasUsername && !hasEmail) {
            throw new IllegalArgumentException("Provide username or email");
        }
        if (hasUsername && hasEmail) {
            throw new IllegalArgumentException("Provide only one: username or email");
        }
        if (hasUsername) {
            if (username.length() < 1 || username.length() > 50) {
                throw new IllegalArgumentException("Username must be between 1 and 50 characters");
            }
        } else {
            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException("Email must be provided");
            }
        }
    }
}
