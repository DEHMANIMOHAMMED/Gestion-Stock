package com.gestionstock.iam.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gestionstock.ai.infrastructure.repository.*;
import com.gestionstock.billing.infrastructure.entity.BillingSubscriptionEntity;
import com.gestionstock.billing.infrastructure.repository.BillingSubscriptionRepository;
import com.gestionstock.iam.infrastructure.entity.Organisation;
import com.gestionstock.iam.infrastructure.entity.Role;
import com.gestionstock.iam.infrastructure.entity.User;
import com.gestionstock.iam.infrastructure.repository.OrganisationRepository;
import com.gestionstock.iam.infrastructure.repository.UserRepository;
import com.gestionstock.product.infrastructure.repository.ProductJpaRepository;
import com.gestionstock.security.JwtService;
import com.gestionstock.stock.infrastructure.repository.StockJpaRepository;
import com.gestionstock.stock.infrastructure.repository.StockMovementJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrganisationUserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BillingSubscriptionRepository billingSubscriptionRepository;

    @Autowired
    private ProductJpaRepository productRepository;

    @Autowired
    private StockJpaRepository stockRepository;

    @Autowired
    private StockMovementJpaRepository stockMovementRepository;

    @Autowired
    private AiForecastRepository forecastRepository;

    @Autowired
    private AiStockoutRiskRepository stockoutRiskRepository;

    @Autowired
    private AiReorderRecommendationRepository reorderRepository;

    @Autowired
    private AiAnomalyRepository anomalyRepository;

    @Autowired
    private AiInsightRepository insightRepository;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        anomalyRepository.deleteAll();
        reorderRepository.deleteAll();
        stockoutRiskRepository.deleteAll();
        forecastRepository.deleteAll();
        insightRepository.deleteAll();
        stockMovementRepository.deleteAll();
        stockRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        billingSubscriptionRepository.deleteAll();
        organisationRepository.deleteAll();
    }

    @Test
    void adminCanListOnlyCurrentOrganisationUsers() throws Exception {
        Organisation org = organisationRepository.save(Organisation.builder().name("Users Org").build());
        Organisation otherOrg = organisationRepository.save(Organisation.builder().name("Other Users Org").build());
        User admin = saveUser("admin@users.test", Role.ADMIN, org);
        saveUser("member@users.test", Role.USER, org);
        saveUser("external@users.test", Role.USER, otherOrg);

        mockMvc.perform(get("/organisation-users")
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].organisationId").value(org.getId()));
    }

    @Test
    void adminCanCreateTenantUserButCannotCreateOwner() throws Exception {
        Organisation org = organisationRepository.save(Organisation.builder().name("Create Users Org").build());
        User admin = saveUser("admin-create@users.test", Role.ADMIN, org);

        mockMvc.perform(post("/organisation-users")
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePayload(
                                "new-user@users.test",
                                "Password123!",
                                "USER"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new-user@users.test"))
                .andExpect(jsonPath("$.role").value("USER"));

        assertThat(userRepository.findByEmail("new-user@users.test")).isPresent();

        mockMvc.perform(post("/organisation-users")
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePayload(
                                "owner-attempt@users.test",
                                "Password123!",
                                "OWNER"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void starterPlanCannotCreateMoreThanThreeUsers() throws Exception {
        Organisation org = organisationRepository.save(Organisation.builder().name("Starter Users Org").build());
        User admin = saveUser("admin-starter@users.test", Role.ADMIN, org);
        saveUser("first@starter.test", Role.USER, org);
        saveUser("second@starter.test", Role.USER, org);
        saveSubscription(org, "STARTER");

        mockMvc.perform(post("/organisation-users")
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePayload(
                                "blocked@starter.test",
                                "Password123!",
                                "USER"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Limite Starter atteinte: 3 utilisateurs maximum. Passez en Pro pour ajouter plus de comptes."));
    }

    @Test
    void userCannotManageOrganisationUsers() throws Exception {
        Organisation org = organisationRepository.save(Organisation.builder().name("Forbidden Users Org").build());
        User user = saveUser("simple@users.test", Role.USER, org);

        mockMvc.perform(get("/organisation-users")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotUpdateUserFromAnotherTenantOrOwnRole() throws Exception {
        Organisation org = organisationRepository.save(Organisation.builder().name("Role Users Org").build());
        Organisation otherOrg = organisationRepository.save(Organisation.builder().name("Other Role Users Org").build());
        User admin = saveUser("admin-role@users.test", Role.ADMIN, org);
        User external = saveUser("external-role@users.test", Role.USER, otherOrg);

        mockMvc.perform(patch("/organisation-users/{id}/role", external.getId())
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RolePayload("ADMIN"))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/organisation-users/{id}/role", admin.getId())
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RolePayload("USER"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCanDisableAndResetPasswordForTenantUser() throws Exception {
        Organisation org = organisationRepository.save(Organisation.builder().name("Lifecycle Users Org").build());
        User admin = saveUser("admin-lifecycle@users.test", Role.ADMIN, org);
        User member = saveUser("member-lifecycle@users.test", Role.USER, org);

        mockMvc.perform(patch("/organisation-users/{id}/enabled?enabled=false", member.getId())
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        assertThat(userRepository.findById(member.getId()).orElseThrow().isEnabled()).isFalse();

        mockMvc.perform(post("/organisation-users/{id}/reset-password", member.getId())
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordPayload("NewPassword123!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(member.getEmail()));
    }

    private User saveUser(String email, Role role, Organisation organisation) {
        return userRepository.save(User.builder()
                .email(email)
                .password("not-used")
                .organisation(organisation)
                .role(role)
                .build());
    }

    private void saveSubscription(Organisation organisation, String planCode) {
        BillingSubscriptionEntity subscription = new BillingSubscriptionEntity();
        subscription.setOrganisation(organisation);
        subscription.setPlanCode(planCode);
        subscription.setStatus("TRIALING");
        billingSubscriptionRepository.save(subscription);
    }

    private record CreatePayload(String email, String password, String role) {
    }

    private record RolePayload(String role) {
    }

    private record ResetPasswordPayload(String temporaryPassword) {
    }
}
