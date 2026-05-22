package com.gestionstock.billing.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gestionstock.billing.infrastructure.repository.BillingSubscriptionRepository;
import com.gestionstock.iam.infrastructure.entity.Organisation;
import com.gestionstock.iam.infrastructure.entity.Role;
import com.gestionstock.iam.infrastructure.entity.User;
import com.gestionstock.iam.infrastructure.repository.OrganisationRepository;
import com.gestionstock.iam.infrastructure.repository.UserRepository;
import com.gestionstock.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BillingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BillingSubscriptionRepository subscriptionRepository;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @AfterEach
    void tearDown() {
        subscriptionRepository.deleteAll();
    }

    @Test
    void adminCanReadTrialSubscription() throws Exception {
        User admin = user(Role.ADMIN, "admin@billing.test");

        mockMvc.perform(get("/billing/subscription")
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planCode").value("TRIAL"))
                .andExpect(jsonPath("$.status").value("TRIALING"))
                .andExpect(jsonPath("$.stripeConfigured").value(false));
    }

    @Test
    void checkoutIsExplicitlyDisabledWhenStripeIsNotConfigured() throws Exception {
        User admin = user(Role.ADMIN, "admin-checkout@billing.test");

        mockMvc.perform(post("/billing/checkout-session")
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckoutRequest("PRO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("STRIPE_NOT_CONFIGURED"))
                .andExpect(jsonPath("$.checkoutUrl").isEmpty());
    }

    @Test
    void userCannotAccessBilling() throws Exception {
        User user = user(Role.USER, "user@billing.test");

        mockMvc.perform(get("/billing/subscription")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isForbidden());
    }

    private User user(Role role, String email) {
        String uniqueEmail = System.nanoTime() + "-" + email;
        Organisation organisation = organisationRepository.save(Organisation.builder()
                .name("Billing Org " + uniqueEmail)
                .status("ACTIVE")
                .onboardingCompleted(true)
                .build());
        return userRepository.save(User.builder()
                .email(uniqueEmail)
                .password("not-used")
                .organisation(organisation)
                .role(role)
                .build());
    }

    private record CheckoutRequest(String planCode) {
    }
}
