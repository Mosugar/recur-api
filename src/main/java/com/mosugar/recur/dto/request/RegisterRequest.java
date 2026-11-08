package com.mosugar.recur.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email(message = "must be a valid email address")
        @NotBlank
        String email,

        @NotBlank
        @Size(min = 8, message = "must be at least 8 characters")
        String password,

        @NotBlank
        @Size(max = 100, message = "must be at most 100 characters")
        String displayName
) {}
