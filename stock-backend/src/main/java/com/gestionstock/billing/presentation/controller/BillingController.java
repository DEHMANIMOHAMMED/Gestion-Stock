package com.gestionstock.billing.presentation.controller;

import com.gestionstock.billing.application.dto.BillingCheckoutRequest;
import com.gestionstock.billing.application.dto.BillingCheckoutResponse;
import com.gestionstock.billing.application.dto.BillingSubscriptionResponse;
import com.gestionstock.billing.application.service.BillingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/billing")
public class BillingController {

    private final BillingService billingService;

    @GetMapping("/subscription")
    public ResponseEntity<BillingSubscriptionResponse> subscription() {
        return ResponseEntity.ok(billingService.currentSubscription());
    }

    @PostMapping("/checkout-session")
    public ResponseEntity<BillingCheckoutResponse> checkoutSession(@Valid @RequestBody BillingCheckoutRequest request) {
        return ResponseEntity.ok(billingService.createCheckoutSession(request));
    }

    @PostMapping("/subscription/cancel")
    public ResponseEntity<BillingSubscriptionResponse> cancelSubscription() {
        return ResponseEntity.ok(billingService.cancelAtPeriodEnd());
    }

    @PostMapping("/stripe/webhook")
    public ResponseEntity<Void> stripeWebhook(
            @RequestBody String payload,
            @RequestHeader(name = "Stripe-Signature", required = false) String signature
    ) {
        billingService.handleStripeWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }
}
