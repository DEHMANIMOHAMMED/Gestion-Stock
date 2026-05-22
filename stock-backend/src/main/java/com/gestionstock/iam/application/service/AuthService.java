package com.gestionstock.iam.application.service;

import com.gestionstock.audit.AuditLogService;
import com.gestionstock.billing.infrastructure.entity.BillingSubscriptionEntity;
import com.gestionstock.billing.infrastructure.repository.BillingSubscriptionRepository;
import com.gestionstock.iam.google.GoogleTokenVerifier;
import com.gestionstock.iam.infrastructure.entity.Organisation;
import com.gestionstock.iam.infrastructure.entity.Role;
import com.gestionstock.iam.infrastructure.entity.User;
import com.gestionstock.iam.infrastructure.repository.OrganisationRepository;
import com.gestionstock.iam.infrastructure.repository.UserRepository;
import com.gestionstock.iam.presentation.dto.*;
import com.gestionstock.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OrganisationRepository organisationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final AuditLogService auditLogService;
    private final BillingSubscriptionRepository billingSubscriptionRepository;

    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        String orgName = request.organisationName().trim();

        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already in use");
        }

        if (organisationRepository.existsByName(orgName)) {
            throw new IllegalArgumentException("Organisation already exists");
        }
        String planCode = request.planCode().trim().toUpperCase(Locale.ROOT);

        Organisation org = Organisation.builder()
                .name(orgName)
                .build();
        organisationRepository.save(org);

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .organisation(org)
                .role(Role.ADMIN)
                .build();

        userRepository.save(user);
        createTrialSubscription(org, planCode);
        auditLogService.record(user, "AUTH_REGISTER", "USER", user.getId(), "LOCAL", "New organisation admin registered");

        String token = jwtService.generateToken(user);

        return new AuthResponse(
                token,
                user.getId(),
                org.getId(),
                user.getRole().name()
        );
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            email,
                            request.password()
                    )
            );
        } catch (AuthenticationException exception) {
            userRepository.findByEmail(email).ifPresent(user ->
                    auditLogService.record(user, "AUTH_LOGIN_FAILED", "USER", user.getId(), "LOCAL", "Failed password login")
            );
            throw exception;
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!user.isEnabled()) {
            throw new AccessDeniedException("Account is disabled");
        }
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        auditLogService.record(user, "AUTH_LOGIN_SUCCESS", "USER", user.getId(), "LOCAL", "Password login succeeded");

        String token = jwtService.generateToken(user);

        return new AuthResponse(
                token,
                user.getId(),
                user.getOrganisation().getId(),
                user.getRole().name()
        );
    }

    public MeResponse me() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = switch (principal) {
            case User authenticatedUser -> authenticatedUser;
            case String email when !"anonymousUser".equals(email) -> userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
            default -> throw new IllegalStateException("Authentication required");
        };

        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getOrganisation().getId(),
                user.getOrganisation().getName(),
                user.getOrganisation().isOnboardingCompleted(),
                user.getRole().name(),
                planCodeFor(user.getOrganisation())
        );
    }

    public OrganisationProfileResponse organisationProfile() {
        User user = currentUser();
        Organisation org = user.getOrganisation();
        return toProfileResponse(org);
    }

    public OrganisationProfileResponse updateOrganisationProfile(OrganisationProfileRequest request) {
        User user = currentUser();
        if (user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("ADMIN role required to update organisation settings");
        }
        Organisation org = user.getOrganisation();

        String requestedName = request.name().trim();
        if (!org.getName().equalsIgnoreCase(requestedName) && organisationRepository.existsByName(requestedName)) {
            throw new IllegalArgumentException("Organisation already exists");
        }

        org.setName(requestedName);
        org.setIndustry(request.industry().trim());
        org.setSizeRange(request.sizeRange().trim());
        org.setPhone(blankToNull(request.phone()));
        org.setAddress(blankToNull(request.address()));
        org.setCity(request.city().trim());
        org.setCountry(request.country().trim());
        org.setCurrency(request.currency().trim().toUpperCase());
        org.setLogoUrl(blankToNull(request.logoUrl()));
        org.setTaxId(blankToNull(request.taxId()));
        org.setWebsite(blankToNull(request.website()));
        org.setStockAlertEmail(blankToNull(request.stockAlertEmail()));
        org.setDefaultLeadTimeDays(request.defaultLeadTimeDays() == null ? 7 : request.defaultLeadTimeDays());
        org.setOnboardingCompleted(true);

        return toProfileResponse(organisationRepository.save(org));
    }

    public void changePassword(ChangePasswordRequest request) {
        User user = currentUser();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is invalid");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        auditLogService.record(user, "AUTH_PASSWORD_CHANGED", "USER", user.getId(), "LOCAL", "User changed password");
    }

    public AuthResponse loginWithGoogle(String idToken, String requestedPlanCode) {
        var payload = googleTokenVerifier.verify(idToken);
        if (payload == null) {
            throw new IllegalArgumentException("Invalid Google token");
        }

        String email = payload.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            Organisation org = Organisation.builder()
                    .name("Google-" + email)
                    .build();
            organisationRepository.save(org);
            createTrialSubscription(org, normalizePlanCode(requestedPlanCode));

            user = User.builder()
                    .email(email)
                    .password("")
                    .organisation(org)
                    .role(Role.ADMIN)
                    .build();

            userRepository.save(user);
            auditLogService.record(user, "AUTH_GOOGLE_REGISTER", "USER", user.getId(), "GOOGLE", "Google user auto-registered");
        } else {
            if (!user.isEnabled()) {
                throw new AccessDeniedException("Account is disabled");
            }
            auditLogService.record(user, "AUTH_GOOGLE_LOGIN_SUCCESS", "USER", user.getId(), "GOOGLE", "Google login succeeded");
        }

        String jwt = jwtService.generateGoogleToken(user);

        return new AuthResponse(
                jwt,
                user.getId(),
                user.getOrganisation().getId(),
                user.getRole().name()
        );
    }

    private User currentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return switch (principal) {
            case User authenticatedUser -> authenticatedUser;
            case String email when !"anonymousUser".equals(email) -> userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
            default -> throw new IllegalStateException("Authentication required");
        };
    }

    private void createTrialSubscription(Organisation organisation, String planCode) {
        BillingSubscriptionEntity subscription = new BillingSubscriptionEntity();
        subscription.setOrganisation(organisation);
        subscription.setPlanCode(planCode);
        subscription.setStatus("TRIALING");
        subscription.setTrialEndsAt(LocalDateTime.now().plusDays(14));
        billingSubscriptionRepository.save(subscription);
    }

    private String normalizePlanCode(String requestedPlanCode) {
        if (requestedPlanCode == null || requestedPlanCode.isBlank()) {
            return "STARTER";
        }
        String planCode = requestedPlanCode.trim().toUpperCase(Locale.ROOT);
        if (!"STARTER".equals(planCode) && !"PRO".equals(planCode)) {
            throw new IllegalArgumentException("Unsupported billing plan");
        }
        return planCode;
    }

    private String planCodeFor(Organisation organisation) {
        return billingSubscriptionRepository.findByOrganisationId(organisation.getId())
                .map(BillingSubscriptionEntity::getPlanCode)
                .orElse("TRIAL");
    }

    private OrganisationProfileResponse toProfileResponse(Organisation org) {
        return new OrganisationProfileResponse(
                org.getId(),
                org.getName(),
                org.getIndustry(),
                org.getSizeRange(),
                org.getPhone(),
                org.getAddress(),
                org.getCity(),
                org.getCountry(),
                org.getCurrency(),
                org.getLogoUrl(),
                org.getTaxId(),
                org.getWebsite(),
                org.getStockAlertEmail(),
                org.getDefaultLeadTimeDays(),
                org.isOnboardingCompleted()
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
