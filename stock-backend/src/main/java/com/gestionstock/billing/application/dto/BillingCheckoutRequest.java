package com.gestionstock.billing.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BillingCheckoutRequest(
        @NotBlank @Pattern(regexp = "STARTER|PRO", message = "Plan must be STARTER or PRO") String planCode,
        String successUrl,
        String cancelUrl
) {
}
