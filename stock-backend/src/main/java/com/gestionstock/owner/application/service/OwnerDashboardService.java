package com.gestionstock.owner.application.service;

import com.gestionstock.billing.infrastructure.entity.BillingSubscriptionEntity;
import com.gestionstock.billing.infrastructure.repository.BillingSubscriptionRepository;
import com.gestionstock.iam.infrastructure.entity.Organisation;
import com.gestionstock.iam.infrastructure.entity.Role;
import com.gestionstock.iam.infrastructure.entity.User;
import com.gestionstock.iam.infrastructure.repository.OrganisationRepository;
import com.gestionstock.iam.infrastructure.repository.UserRepository;
import com.gestionstock.owner.application.dto.OwnerDashboardResponse;
import com.gestionstock.owner.application.dto.OwnerOrganizationResponse;
import com.gestionstock.product.infrastructure.repository.ProductJpaRepository;
import com.gestionstock.security.AuthenticatedUserProvider;
import com.gestionstock.stock.infrastructure.repository.StockMovementJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OwnerDashboardService {

    private final OrganisationRepository organisationRepository;
    private final UserRepository userRepository;
    private final ProductJpaRepository productRepository;
    private final StockMovementJpaRepository stockMovementRepository;
    private final BillingSubscriptionRepository billingSubscriptionRepository;
    private final LegalSettingsService legalSettingsService;
    private final AuthenticatedUserProvider userProvider;

    @Transactional(readOnly = true)
    public OwnerDashboardResponse dashboard() {
        List<OwnerOrganizationResponse> organizations = organizations();
        return new OwnerDashboardResponse(
                organizations.size(),
                userRepository.count(),
                productRepository.count(),
                stockMovementRepository.count(),
                organizations.stream().filter(org -> "ACTIVE".equals(org.status())).count(),
                organizations.stream().filter(org -> "TRIAL".equals(org.status())).count(),
                organizations,
                legalSettingsService.currentSettingsForOwnerDashboard()
        );
    }

    @Transactional(readOnly = true)
    public List<OwnerOrganizationResponse> organizations() {
        requireOwner();
        return organisationRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Organisation::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }

    private void requireOwner() {
        User user = userProvider.requireUser();
        if (user.getRole() != Role.OWNER) {
            throw new AccessDeniedException("Only owner can access SaaS platform metrics");
        }
    }

    private OwnerOrganizationResponse toResponse(Organisation organisation) {
        Long organisationId = organisation.getId();
        BillingSubscriptionEntity subscription = billingSubscriptionRepository.findByOrganisationId(organisationId).orElse(null);
        return new OwnerOrganizationResponse(
                organisationId,
                organisation.getName(),
                organisation.getIndustry(),
                organisation.getSizeRange(),
                organisation.getCity(),
                organisation.getCountry(),
                organisation.getStatus(),
                subscription == null ? "TRIAL" : subscription.getPlanCode(),
                subscription == null ? "TRIALING" : subscription.getStatus(),
                organisation.isOnboardingCompleted(),
                organisation.getCreatedAt(),
                userRepository.countByOrganisationId(organisationId),
                productRepository.countByOrganisationId(organisationId),
                stockMovementRepository.countByOrganisationId(organisationId)
        );
    }
}
