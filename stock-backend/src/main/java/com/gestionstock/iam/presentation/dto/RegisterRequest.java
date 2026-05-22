package com.gestionstock.iam.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Organisation name is required")
        @Size(max = 120, message = "Organisation name must be at most 120 characters")
        String organisationName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 120, message = "Password must be between 8 and 120 characters")
        String password,

        @NotBlank(message = "Plan is required")
        @Pattern(regexp = "STARTER|PRO", message = "Plan must be STARTER or PRO")
        String planCode
) {}
