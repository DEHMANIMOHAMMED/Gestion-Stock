package com.gestionstock.iam.application.service;

import com.gestionstock.audit.AuditLogService;
import com.gestionstock.billing.infrastructure.repository.BillingSubscriptionRepository;
import com.gestionstock.iam.infrastructure.entity.Role;
import com.gestionstock.iam.infrastructure.entity.User;
import com.gestionstock.iam.infrastructure.repository.UserRepository;
import com.gestionstock.iam.presentation.dto.CreateOrganisationUserRequest;
import com.gestionstock.iam.presentation.dto.OrganisationUserResponse;
import com.gestionstock.iam.presentation.dto.ResetOrganisationUserPasswordRequest;
import com.gestionstock.iam.presentation.dto.UpdateOrganisationUserRoleRequest;
import com.gestionstock.security.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganisationUserService {

    private static final int STARTER_USER_LIMIT = 3;

    private final UserRepository userRepository;
    private final BillingSubscriptionRepository billingSubscriptionRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissionService permissionService;
    private final AuditLogService auditLogService;

    public List<OrganisationUserResponse> listUsers() {
        User admin = permissionService.requireAdmin();
        Long organisationId = admin.getOrganisation().getId();
        return userRepository.findByOrganisationIdOrderByEmailAsc(organisationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public OrganisationUserResponse createUser(CreateOrganisationUserRequest request) {
        User admin = permissionService.requireAdmin();
        String email = normalizeEmail(request.email());
        Role role = parseTenantRole(request.role());

        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already in use");
        }
        enforceUserLimit(admin);

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .organisation(admin.getOrganisation())
                .role(role)
                .build();

        User saved = userRepository.save(user);
        auditLogService.record(admin, "ORG_USER_CREATED", "USER", saved.getId(), "BACKEND",
                "Created organisation user " + saved.getEmail() + " with role " + saved.getRole());
        return toResponse(saved);
    }

    public OrganisationUserResponse updateRole(Long userId, UpdateOrganisationUserRoleRequest request) {
        User admin = permissionService.requireAdmin();
        Role role = parseTenantRole(request.role());

        if (admin.getId().equals(userId)) {
            throw new IllegalArgumentException("Admins cannot change their own role");
        }

        User user = userRepository.findByIdAndOrganisationId(userId, admin.getOrganisation().getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found in current organisation"));

        Role previousRole = user.getRole();
        user.setRole(role);
        User saved = userRepository.save(user);
        auditLogService.record(admin, "ORG_USER_ROLE_UPDATED", "USER", saved.getId(), "BACKEND",
                "Changed user role from " + previousRole + " to " + saved.getRole());
        return toResponse(saved);
    }

    public OrganisationUserResponse setEnabled(Long userId, boolean enabled) {
        User admin = permissionService.requireAdmin();
        if (admin.getId().equals(userId)) {
            throw new IllegalArgumentException("Admins cannot disable their own account");
        }
        User user = userRepository.findByIdAndOrganisationId(userId, admin.getOrganisation().getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found in current organisation"));
        user.setEnabled(enabled);
        User saved = userRepository.save(user);
        auditLogService.record(admin, enabled ? "ORG_USER_ENABLED" : "ORG_USER_DISABLED", "USER", saved.getId(), "BACKEND",
                (enabled ? "Enabled" : "Disabled") + " organisation user " + saved.getEmail());
        return toResponse(saved);
    }

    public OrganisationUserResponse resetPassword(Long userId, ResetOrganisationUserPasswordRequest request) {
        User admin = permissionService.requireAdmin();
        User user = userRepository.findByIdAndOrganisationId(userId, admin.getOrganisation().getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found in current organisation"));
        user.setPassword(passwordEncoder.encode(request.temporaryPassword()));
        User saved = userRepository.save(user);
        auditLogService.record(admin, "ORG_USER_PASSWORD_RESET", "USER", saved.getId(), "BACKEND",
                "Reset temporary password for organisation user " + saved.getEmail());
        return toResponse(saved);
    }

    private Role parseTenantRole(String value) {
        Role role = Role.valueOf(value.trim().toUpperCase());
        if (role == Role.OWNER) {
            throw new IllegalArgumentException("OWNER role cannot be managed from organisation users");
        }
        return role;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private void enforceUserLimit(User admin) {
        String planCode = billingSubscriptionRepository.findByOrganisationId(admin.getOrganisation().getId())
                .map(subscription -> subscription.getPlanCode() == null ? "TRIAL" : subscription.getPlanCode())
                .orElse("TRIAL");
        if (!"STARTER".equalsIgnoreCase(planCode)) {
            return;
        }
        long currentUsers = userRepository.countByOrganisationId(admin.getOrganisation().getId());
        if (currentUsers >= STARTER_USER_LIMIT) {
            throw new IllegalArgumentException("Limite Starter atteinte: 3 utilisateurs maximum. Passez en Pro pour ajouter plus de comptes.");
        }
    }

    private OrganisationUserResponse toResponse(User user) {
        return new OrganisationUserResponse(
                user.getId(),
                user.getEmail(),
                user.getOrganisation().getId(),
                user.getOrganisation().getName(),
                user.getRole().name(),
                user.isEnabled(),
                user.getLastLoginAt()
        );
    }
}
