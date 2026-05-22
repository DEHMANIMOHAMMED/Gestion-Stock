package com.gestionstock.security;

import com.gestionstock.billing.infrastructure.repository.BillingSubscriptionRepository;
import com.gestionstock.iam.infrastructure.entity.Role;
import com.gestionstock.iam.infrastructure.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlanAccessService {

    private final AuthenticatedUserProvider userProvider;
    private final BillingSubscriptionRepository billingSubscriptionRepository;

    @Value("${stockpilot.billing.allow-missing-subscription:false}")
    private boolean allowMissingSubscription;

    public void requireProPlan() {
        User user = userProvider.requireUser();
        if (user.getRole() == Role.OWNER) {
            return;
        }
        if (user.getRole() == Role.USER) {
            throw new AccessDeniedException("ADMIN role required");
        }
        String planCode = billingSubscriptionRepository.findByOrganisationId(user.getOrganisation().getId())
                .map(subscription -> subscription.getPlanCode() == null ? "TRIAL" : subscription.getPlanCode())
                .orElseGet(() -> allowMissingSubscription ? "PRO" : "TRIAL");
        if (!"PRO".equalsIgnoreCase(planCode)) {
            throw new AccessDeniedException("PRO plan required for this action");
        }
    }
}
