package com.gestionstock.billing.application.dto;

import java.time.LocalDateTime;

public record BillingSubscriptionResponse(
        String planCode,
        String status,
        boolean stripeConfigured,
        String stripeCustomerId,
        String stripeSubscriptionId,
        LocalDateTime currentPeriodEnd,
        LocalDateTime trialEndsAt,
        boolean cancelAtPeriodEnd
) {
}
