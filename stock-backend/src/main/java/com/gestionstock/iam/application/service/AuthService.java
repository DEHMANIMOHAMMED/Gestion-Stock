package com.gestionstock.iam.application.service;

import com.gestionstock.audit.AuditLogService;
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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        String orgName = request.organisationName().trim();

        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already in use");
        }

        if (organisationRepository.existsByName(orgName)) {
            throw new IllegalArgumentException("Organisation already exists");
        }

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
                user.getRole().name()
        );
    }

    public OrganisationProfileResponse organisationProfile() {
        User user = currentUser();
        Organisation org = user.getOrganisation();
        return toProfileResponse(org);
    }

    public OrganisationProfileResponse updateOrganisationProfile(OrganisationProfileRequest request) {
        User user = currentUser();
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
        org.setOnboardingCompleted(true);

        return toProfileResponse(organisationRepository.save(org));
    }

    public AuthResponse loginWithGoogle(String idToken) {
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

            user = User.builder()
                    .email(email)
                    .password("")
                    .organisation(org)
                    .role(Role.ADMIN)
                    .build();

            userRepository.save(user);
            auditLogService.record(user, "AUTH_GOOGLE_REGISTER", "USER", user.getId(), "GOOGLE", "Google user auto-registered");
        } else {
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
                org.isOnboardingCompleted()
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
