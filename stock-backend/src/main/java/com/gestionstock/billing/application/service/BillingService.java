package com.gestionstock.billing.application.service;

import com.gestionstock.billing.application.dto.BillingCheckoutRequest;
import com.gestionstock.billing.application.dto.BillingCheckoutResponse;
import com.gestionstock.billing.application.dto.BillingSubscriptionResponse;
import com.gestionstock.billing.infrastructure.entity.BillingSubscriptionEntity;
import com.gestionstock.billing.infrastructure.repository.BillingSubscriptionRepository;
import com.gestionstock.iam.infrastructure.entity.Organisation;
import com.gestionstock.iam.infrastructure.entity.User;
import com.gestionstock.security.PermissionService;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.checkout.Session;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingService {

    private final BillingSubscriptionRepository subscriptionRepository;
    private final PermissionService permissionService;

    @Value("${stockpilot.billing.stripe-secret-key:}")
    private String stripeSecretKey;

    @Value("${stockpilot.billing.stripe-webhook-secret:}")
    private String stripeWebhookSecret;

    @Value("${stockpilot.billing.success-url:http://127.0.0.1:4200/billing?payment=success}")
    private String defaultSuccessUrl;

    @Value("${stockpilot.billing.cancel-url:http://127.0.0.1:4200/billing?payment=cancel}")
    private String defaultCancelUrl;

    @Value("${stockpilot.billing.starter-price-id:}")
    private String starterPriceId;

    @Value("${stockpilot.billing.pro-price-id:}")
    private String proPriceId;

    @Transactional(readOnly = true)
    public BillingSubscriptionResponse currentSubscription() {
        User admin = permissionService.requireAdmin();
        BillingSubscriptionEntity subscription = subscriptionRepository
                .findByOrganisationId(admin.getOrganisation().getId())
                .orElseGet(() -> trialSubscription(admin.getOrganisation()));
        return toResponse(subscription);
    }

    @Transactional
    public BillingCheckoutResponse createCheckoutSession(BillingCheckoutRequest request) {
        User admin = permissionService.requireAdmin();
        String planCode = request.planCode().trim().toUpperCase(Locale.ROOT);
        String priceId = priceIdFor(planCode);
        if (!stripeConfigured() || priceId.isBlank()) {
            return new BillingCheckoutResponse(
                    null,
                    "STRIPE_NOT_CONFIGURED",
                    "Configure Stripe keys and price IDs to enable online checkout."
            );
        }

        BillingSubscriptionEntity subscription = subscriptionRepository
                .findByOrganisationId(admin.getOrganisation().getId())
                .orElseGet(() -> subscriptionRepository.save(trialSubscription(admin.getOrganisation())));

        try {
            Stripe.apiKey = stripeSecretKey;
            SessionCreateParams.Builder builder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setClientReferenceId(admin.getOrganisation().getId().toString())
                    .setSuccessUrl(nonBlankOrDefault(request.successUrl(), defaultSuccessUrl))
                    .setCancelUrl(nonBlankOrDefault(request.cancelUrl(), defaultCancelUrl))
                    .putMetadata("organisationId", admin.getOrganisation().getId().toString())
                    .putMetadata("planCode", planCode)
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setPrice(priceId)
                            .setQuantity(1L)
                            .build());

            if (subscription.getStripeCustomerId() != null && !subscription.getStripeCustomerId().isBlank()) {
                builder.setCustomer(subscription.getStripeCustomerId());
            } else {
                builder.setCustomerEmail(admin.getEmail());
            }

            Session session = Session.create(builder.build());
            subscription.setPlanCode(planCode);
            subscription.setStatus("CHECKOUT_PENDING");
            subscriptionRepository.save(subscription);
            return new BillingCheckoutResponse(session.getUrl(), "STRIPE", "Checkout session created.");
        } catch (StripeException exception) {
            throw new IllegalStateException("Stripe checkout failed: " + exception.getMessage());
        }
    }

    @Transactional
    public BillingSubscriptionResponse cancelAtPeriodEnd() {
        User admin = permissionService.requireAdmin();
        BillingSubscriptionEntity subscription = subscriptionRepository
                .findByOrganisationId(admin.getOrganisation().getId())
                .orElseGet(() -> subscriptionRepository.save(trialSubscription(admin.getOrganisation())));

        if (subscription.getStripeSubscriptionId() != null
                && !subscription.getStripeSubscriptionId().isBlank()
                && stripeConfigured()) {
            try {
                Stripe.apiKey = stripeSecretKey;
                Subscription stripeSubscription = Subscription.retrieve(subscription.getStripeSubscriptionId());
                Subscription updated = stripeSubscription.update(SubscriptionUpdateParams.builder()
                        .setCancelAtPeriodEnd(true)
                        .build());
                updateFromStripeSubscription(updated);
                return currentSubscription();
            } catch (StripeException exception) {
                throw new IllegalStateException("Stripe cancellation failed: " + exception.getMessage());
            }
        }

        subscription.setCancelAtPeriodEnd(true);
        subscription.setStatus("CANCEL_AT_PERIOD_END");
        if (subscription.getCurrentPeriodEnd() == null) {
            subscription.setCurrentPeriodEnd(subscription.getTrialEndsAt() == null
                    ? LocalDateTime.now().plusDays(30)
                    : subscription.getTrialEndsAt());
        }
        return toResponse(subscriptionRepository.save(subscription));
    }

    @Transactional
    public void handleStripeWebhook(String payload, String signature) {
        if (stripeWebhookSecret == null || stripeWebhookSecret.isBlank()) {
            throw new AccessDeniedException("Stripe webhook secret is not configured");
        }
        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, stripeWebhookSecret);
        } catch (SignatureVerificationException exception) {
            throw new AccessDeniedException("Invalid Stripe signature");
        }

        Object object = event.getDataObjectDeserializer().getObject().orElse(null);
        log.info("Stripe webhook received: {}", event.getType());
        if (object instanceof Session session && "checkout.session.completed".equals(event.getType())) {
            activateFromCheckoutSession(session);
        } else if (object instanceof Subscription subscription
                && ("customer.subscription.updated".equals(event.getType())
                || "customer.subscription.deleted".equals(event.getType()))) {
            updateFromStripeSubscription(subscription);
        } else if (object instanceof Invoice invoice && "invoice.payment_failed".equals(event.getType())) {
            markPaymentFailed(invoice);
        } else {
            log.debug("Stripe webhook ignored: {}", event.getType());
        }
    }

    private void activateFromCheckoutSession(Session session) {
        Map<String, String> metadata = session.getMetadata();
        String organisationIdValue = metadata == null ? null : metadata.get("organisationId");
        if (organisationIdValue == null || organisationIdValue.isBlank()) {
            return;
        }
        Long organisationId = Long.valueOf(organisationIdValue);
        BillingSubscriptionEntity subscription = subscriptionRepository.findByOrganisationId(organisationId).orElse(null);
        if (subscription == null) {
            return;
        }
        String planCode = metadata.getOrDefault("planCode", subscription.getPlanCode());
        subscription.setPlanCode(planCode);
        subscription.setStatus("ACTIVE");
        subscription.setStripeCustomerId(session.getCustomer());
        subscription.setStripeSubscriptionId(session.getSubscription());
        subscriptionRepository.save(subscription);
        log.info("Stripe checkout completed for organisation {}", organisationId);
    }

    private void updateFromStripeSubscription(Subscription stripeSubscription) {
        BillingSubscriptionEntity subscription = subscriptionRepository
                .findByStripeSubscriptionId(stripeSubscription.getId())
                .orElseGet(() -> subscriptionRepository.findByStripeCustomerId(stripeSubscription.getCustomer()).orElse(null));
        if (subscription == null) {
            return;
        }
        subscription.setStripeCustomerId(stripeSubscription.getCustomer());
        subscription.setStripeSubscriptionId(stripeSubscription.getId());
        subscription.setStatus(stripeSubscription.getStatus() == null
                ? subscription.getStatus()
                : stripeSubscription.getStatus().toUpperCase(Locale.ROOT));
        subscription.setCancelAtPeriodEnd(Boolean.TRUE.equals(stripeSubscription.getCancelAtPeriodEnd()));
        Long currentPeriodEnd = stripeSubscription.getItems() == null || stripeSubscription.getItems().getData().isEmpty()
                ? null
                : stripeSubscription.getItems().getData().stream()
                .findFirst()
                .map(SubscriptionItem::getCurrentPeriodEnd)
                .orElse(null);
        if (currentPeriodEnd != null) {
            subscription.setCurrentPeriodEnd(LocalDateTime.ofInstant(Instant.ofEpochSecond(currentPeriodEnd), ZoneOffset.UTC));
        }
        subscriptionRepository.save(subscription);
        log.info("Stripe subscription {} synced with status {}", stripeSubscription.getId(), subscription.getStatus());
    }

    private void markPaymentFailed(Invoice invoice) {
        if (invoice.getCustomer() == null || invoice.getCustomer().isBlank()) {
            return;
        }
        BillingSubscriptionEntity subscription = subscriptionRepository
                .findByStripeCustomerId(invoice.getCustomer())
                .orElse(null);
        if (subscription == null) {
            return;
        }
        subscription.setStatus("PAYMENT_FAILED");
        subscriptionRepository.save(subscription);
        log.warn("Stripe payment failed for customer {}", invoice.getCustomer());
    }

    private BillingSubscriptionEntity trialSubscription(Organisation organisation) {
        BillingSubscriptionEntity subscription = new BillingSubscriptionEntity();
        subscription.setOrganisation(organisation);
        subscription.setPlanCode("TRIAL");
        subscription.setStatus("TRIALING");
        subscription.setTrialEndsAt(LocalDateTime.now().plusDays(14));
        return subscription;
    }

    private BillingSubscriptionResponse toResponse(BillingSubscriptionEntity subscription) {
        return new BillingSubscriptionResponse(
                subscription.getPlanCode(),
                subscription.getStatus(),
                stripeConfigured(),
                subscription.getStripeCustomerId(),
                subscription.getStripeSubscriptionId(),
                subscription.getCurrentPeriodEnd(),
                subscription.getTrialEndsAt(),
                subscription.isCancelAtPeriodEnd()
        );
    }

    private String priceIdFor(String planCode) {
        return switch (planCode) {
            case "STARTER" -> starterPriceId == null ? "" : starterPriceId;
            case "PRO" -> proPriceId == null ? "" : proPriceId;
            default -> throw new IllegalArgumentException("Unsupported billing plan");
        };
    }

    private boolean stripeConfigured() {
        return stripeSecretKey != null && !stripeSecretKey.isBlank();
    }

    private String nonBlankOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
