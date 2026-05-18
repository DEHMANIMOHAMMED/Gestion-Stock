package com.gestionstock.iam.application.service;

import com.gestionstock.iam.infrastructure.entity.Organisation;
import com.gestionstock.iam.infrastructure.entity.Role;
import com.gestionstock.iam.infrastructure.entity.User;
import com.gestionstock.iam.infrastructure.repository.OrganisationRepository;
import com.gestionstock.iam.infrastructure.repository.UserRepository;
import com.gestionstock.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final UserRepository userRepository;
    private final OrganisationRepository organisationRepository;
    private final JwtService jwtService;

    public Object processGoogleLogin(OAuth2User googleUser) {

        String email = googleUser.getAttribute("email");
        String name = googleUser.getAttribute("name");
        String normalizedEmail = email.trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail).orElse(null);

        if (user == null) {
            Organisation googleOrg = Organisation.builder()
                    .name("Google-" + normalizedEmail)
                    .build();
            organisationRepository.save(googleOrg);

            user = User.builder()
                    .email(normalizedEmail)
                    .password("")
                    .role(Role.ADMIN)
                    .organisation(googleOrg)
                    .build();

            userRepository.save(user);
        }

        String token = jwtService.generateGoogleToken(user);

        return new GoogleAuthResponse(
                token,
                normalizedEmail,
                name,
                user.getOrganisation().getId()
        );
    }

    private record GoogleAuthResponse(
            String token,
            String email,
            String name,
            Long organisationId
    ) {}
}
