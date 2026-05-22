package com.gestionstock.billing.application.dto;

public record BillingCheckoutResponse(
        String checkoutUrl,
        String provider,
        String message
) {
}
