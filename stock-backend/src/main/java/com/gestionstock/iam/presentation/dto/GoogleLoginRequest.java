package com.gestionstock.iam.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record GoogleLoginRequest(
        @NotBlank(message = "Google ID token is required")
        String idToken,

        @Pattern(regexp = "STARTER|PRO", message = "Plan must be STARTER or PRO")
        String planCode
) {}
