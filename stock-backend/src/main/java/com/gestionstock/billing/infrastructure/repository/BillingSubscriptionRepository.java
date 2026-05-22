package com.gestionstock.billing.infrastructure.repository;

import com.gestionstock.billing.infrastructure.entity.BillingSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillingSubscriptionRepository extends JpaRepository<BillingSubscriptionEntity, Long> {

    Optional<BillingSubscriptionEntity> findByOrganisationId(Long organisationId);

    Optional<BillingSubscriptionEntity> findByStripeSubscriptionId(String stripeSubscriptionId);

    Optional<BillingSubscriptionEntity> findByStripeCustomerId(String stripeCustomerId);
}
