package com.gestionstock.config;

import com.gestionstock.iam.infrastructure.entity.Organisation;
import com.gestionstock.iam.infrastructure.entity.Role;
import com.gestionstock.iam.infrastructure.entity.User;
import com.gestionstock.iam.infrastructure.repository.OrganisationRepository;
import com.gestionstock.iam.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OwnerAccountBootstrap implements CommandLineRunner {

    private static final String PLATFORM_ORGANISATION_NAME = "StockPilot Platform";

    private final OrganisationRepository organisationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${stockpilot.owner.enabled:false}")
    private boolean ownerEnabled;

    @Value("${stockpilot.owner.email:}")
    private String ownerEmail;

    @Value("${stockpilot.owner.password:}")
    private String ownerPassword;

    @Override
    public void run(String... args) {
        if (!ownerEnabled) {
            return;
        }
        if (ownerEmail == null || ownerEmail.isBlank() || ownerPassword == null || ownerPassword.isBlank()) {
            throw new IllegalStateException("Owner bootstrap is enabled but owner email or password is missing");
        }

        String normalizedEmail = ownerEmail.trim().toLowerCase();
        Organisation platform = organisationRepository.findByName(PLATFORM_ORGANISATION_NAME)
                .orElseGet(() -> organisationRepository.save(Organisation.builder()
                        .name(PLATFORM_ORGANISATION_NAME)
                        .industry("SaaS")
                        .sizeRange("1-10")
                        .city("Paris")
                        .country("France")
                        .currency("EUR")
                        .status("ACTIVE")
                        .onboardingCompleted(true)
                        .build()));

        User owner = userRepository.findByEmail(normalizedEmail)
                .orElseGet(User::new);
        owner.setEmail(normalizedEmail);
        owner.setPassword(passwordEncoder.encode(ownerPassword));
        owner.setOrganisation(platform);
        owner.setRole(Role.OWNER);
        owner.setEnabled(true);
        userRepository.save(owner);
    }
}
