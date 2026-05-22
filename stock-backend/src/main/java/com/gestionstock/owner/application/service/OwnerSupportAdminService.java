package com.gestionstock.owner.application.service;

import com.gestionstock.billing.infrastructure.entity.BillingSubscriptionEntity;
import com.gestionstock.billing.infrastructure.repository.BillingSubscriptionRepository;
import com.gestionstock.audit.AuditLogService;
import com.gestionstock.iam.infrastructure.entity.Organisation;
import com.gestionstock.iam.infrastructure.entity.Role;
import com.gestionstock.iam.infrastructure.entity.User;
import com.gestionstock.iam.infrastructure.repository.OrganisationRepository;
import com.gestionstock.iam.infrastructure.repository.UserRepository;
import com.gestionstock.owner.application.dto.OwnerSupportSubscriptionResponse;
import com.gestionstock.owner.application.dto.OwnerSupportUserResponse;
import com.gestionstock.security.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OwnerSupportAdminService {

    private final PermissionService permissionService;
    private final UserRepository userRepository;
    private final OrganisationRepository organisationRepository;
    private final BillingSubscriptionRepository subscriptionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<OwnerSupportUserResponse> users(Long organisationId) {
        permissionService.requireOwner();
        return userRepository.findByOrganisationIdOrderByEmailAsc(organisationId)
                .stream()
                .map(this::toUserResponse)
                .toList();
    }

    @Transactional
    public OwnerSupportUserResponse updateUserStatus(Long userId, boolean enabled, String reason) {
        User owner = permissionService.requireOwner();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getRole() == Role.OWNER) {
            throw new AccessDeniedException("Owner account cannot be disabled from support actions");
        }
        user.setEnabled(enabled);
        User saved = userRepository.save(user);
        auditLogService.record(
                saved.getOrganisation().getId(),
                owner.getId(),
                owner.getEmail(),
                enabled ? "OWNER_SUPPORT_USER_ENABLED" : "OWNER_SUPPORT_USER_DISABLED",
                "USER",
                saved.getId(),
                "OWNER_SUPPORT",
                "Owner support changed user status for " + saved.getEmail() + ". Reason: " + reason.trim()
        );
        return toUserResponse(saved);
    }

    @Transactional
    public OwnerSupportUserResponse changePassword(Long userId, String newPassword, String reason) {
        User owner = permissionService.requireOwner();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getRole() == Role.OWNER) {
            throw new AccessDeniedException("Owner password cannot be changed from support actions");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        User saved = userRepository.save(user);
        auditLogService.record(
                saved.getOrganisation().getId(),
                owner.getId(),
                owner.getEmail(),
                "OWNER_SUPPORT_PASSWORD_RESET",
                "USER",
                saved.getId(),
                "OWNER_SUPPORT",
                "Owner support reset password for " + saved.getEmail() + ". Reason: " + reason.trim()
        );
        return toUserResponse(saved);
    }

    @Transactional
    public OwnerSupportSubscriptionResponse cancelSubscription(Long organisationId, String reason) {
        User owner = permissionService.requireOwner();
        Organisation organisation = organisationRepository.findById(organisationId)
                .orElseThrow(() -> new IllegalArgumentException("Organisation not found"));
        BillingSubscriptionEntity subscription = subscriptionRepository.findByOrganisationId(organisationId)
                .orElseGet(() -> {
                    BillingSubscriptionEntity created = new BillingSubscriptionEntity();
                    created.setOrganisation(organisation);
                    created.setPlanCode("TRIAL");
                    created.setStatus("TRIALING");
                    created.setTrialEndsAt(LocalDateTime.now().plusDays(14));
                    return subscriptionRepository.save(created);
                });
        subscription.setCancelAtPeriodEnd(true);
        subscription.setStatus("CANCEL_AT_PERIOD_END");
        if (subscription.getCurrentPeriodEnd() == null) {
            subscription.setCurrentPeriodEnd(subscription.getTrialEndsAt() == null
                    ? LocalDateTime.now().plusDays(30)
                    : subscription.getTrialEndsAt());
        }
        BillingSubscriptionEntity saved = subscriptionRepository.save(subscription);
        auditLogService.record(
                organisationId,
                owner.getId(),
                owner.getEmail(),
                "OWNER_SUPPORT_SUBSCRIPTION_CANCELLED",
                "BILLING_SUBSCRIPTION",
                saved.getId(),
                "OWNER_SUPPORT",
                "Owner support scheduled subscription cancellation. Reason: " + reason.trim()
        );
        return new OwnerSupportSubscriptionResponse(
                organisationId,
                saved.getPlanCode(),
                saved.getStatus(),
                saved.isCancelAtPeriodEnd()
        );
    }

    private OwnerSupportUserResponse toUserResponse(User user) {
        return new OwnerSupportUserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.isEnabled(),
                user.getLastLoginAt()
        );
    }
}
