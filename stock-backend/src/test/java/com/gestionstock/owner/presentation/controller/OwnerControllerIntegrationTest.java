package com.gestionstock.owner.presentation.controller;

import com.gestionstock.ai.infrastructure.repository.AiAuditLogRepository;
import com.gestionstock.ai.infrastructure.repository.AiAnomalyRepository;
import com.gestionstock.ai.infrastructure.repository.AiForecastRepository;
import com.gestionstock.ai.infrastructure.repository.AiInsightRepository;
import com.gestionstock.ai.infrastructure.repository.AiReorderRecommendationRepository;
import com.gestionstock.ai.infrastructure.repository.AiStockoutRiskRepository;
import com.gestionstock.billing.infrastructure.entity.BillingSubscriptionEntity;
import com.gestionstock.billing.infrastructure.repository.BillingSubscriptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gestionstock.iam.infrastructure.entity.Organisation;
import com.gestionstock.iam.infrastructure.entity.Role;
import com.gestionstock.iam.infrastructure.entity.User;
import com.gestionstock.iam.infrastructure.repository.OrganisationRepository;
import com.gestionstock.iam.infrastructure.repository.UserRepository;
import com.gestionstock.owner.infrastructure.repository.LegalSettingsRepository;
import com.gestionstock.product.infrastructure.entity.ProductEntity;
import com.gestionstock.product.infrastructure.repository.ProductJpaRepository;
import com.gestionstock.security.JwtService;
import com.gestionstock.stock.domain.model.MovementType;
import com.gestionstock.stock.infrastructure.entity.StockMovementEntity;
import com.gestionstock.stock.infrastructure.repository.StockJpaRepository;
import com.gestionstock.stock.infrastructure.repository.StockMovementJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OwnerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductJpaRepository productRepository;

    @Autowired
    private StockMovementJpaRepository stockMovementRepository;

    @Autowired
    private StockJpaRepository stockRepository;

    @Autowired
    private LegalSettingsRepository legalSettingsRepository;

    @Autowired
    private BillingSubscriptionRepository billingSubscriptionRepository;

    @Autowired
    private AiAuditLogRepository auditLogRepository;

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
        auditLogRepository.deleteAll();
        anomalyRepository.deleteAll();
        reorderRepository.deleteAll();
        stockoutRiskRepository.deleteAll();
        forecastRepository.deleteAll();
        insightRepository.deleteAll();
        legalSettingsRepository.deleteAll();
        billingSubscriptionRepository.deleteAll();
        stockMovementRepository.deleteAll();
        stockRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        organisationRepository.deleteAll();
    }

    @Test
    void ownerCanReadGlobalDashboard() throws Exception {
        Organisation platform = organisationRepository.save(Organisation.builder()
                .name("StockPilot Platform Test")
                .status("ACTIVE")
                .onboardingCompleted(true)
                .build());
        Organisation customer = organisationRepository.save(Organisation.builder()
                .name("Customer Org")
                .status("TRIAL")
                .onboardingCompleted(true)
                .build());
        User owner = userRepository.save(User.builder()
                .email("owner@test.local")
                .password("not-used")
                .organisation(platform)
                .role(Role.OWNER)
                .build());
        userRepository.save(User.builder()
                .email("admin@customer.test")
                .password("not-used")
                .organisation(customer)
                .role(Role.ADMIN)
                .build());
        BillingSubscriptionEntity subscription = new BillingSubscriptionEntity();
        subscription.setOrganisation(customer);
        subscription.setPlanCode("PRO");
        subscription.setStatus("TRIALING");
        billingSubscriptionRepository.save(subscription);
        ProductEntity product = productRepository.save(ProductEntity.builder()
                .organisationId(customer.getId())
                .name("Customer Product")
                .sku("CUST-001")
                .minStock(2)
                .unit("pcs")
                .build());
        stockMovementRepository.save(StockMovementEntity.builder()
                .organisationId(customer.getId())
                .productId(product.getId())
                .quantity(3)
                .type(MovementType.IN)
                .createdAt(LocalDateTime.now())
                .build());

        mockMvc.perform(get("/owner/dashboard")
                        .header("Authorization", "Bearer " + jwtService.generateToken(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationsCount").value(2))
                .andExpect(jsonPath("$.usersCount").value(2))
                .andExpect(jsonPath("$.productsCount").value(1))
                .andExpect(jsonPath("$.stockMovementsCount").value(1))
                .andExpect(jsonPath("$.organizations[?(@.name == 'Customer Org')].productsCount").value(1))
                .andExpect(jsonPath("$.organizations[?(@.name == 'Customer Org')].planCode").value("PRO"))
                .andExpect(jsonPath("$.organizations[?(@.name == 'Customer Org')].subscriptionStatus").value("TRIALING"));
    }

    @Test
    void adminAndUserCannotAccessOwnerDashboard() throws Exception {
        Organisation organisation = organisationRepository.save(Organisation.builder()
                .name("Tenant Org")
                .status("ACTIVE")
                .build());
        User admin = userRepository.save(User.builder()
                .email("admin@tenant.test")
                .password("not-used")
                .organisation(organisation)
                .role(Role.ADMIN)
                .build());
        User user = userRepository.save(User.builder()
                .email("user@tenant.test")
                .password("not-used")
                .organisation(organisation)
                .role(Role.USER)
                .build());

        mockMvc.perform(get("/owner/dashboard")
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/owner/dashboard")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanUpdateLegalSettings() throws Exception {
        Organisation platform = organisationRepository.save(Organisation.builder()
                .name("Legal Platform")
                .status("ACTIVE")
                .onboardingCompleted(true)
                .build());
        User owner = userRepository.save(User.builder()
                .email("legal-owner@test.local")
                .password("not-used")
                .organisation(platform)
                .role(Role.OWNER)
                .build());

        mockMvc.perform(put("/owner/legal-settings")
                        .header("Authorization", "Bearer " + jwtService.generateToken(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LegalRequest(
                                "StockPilot SAS",
                                "Mentions legales de test",
                                "Politique de confidentialite",
                                "Conditions generales",
                                "https://stockpilot.local/legal",
                                "https://stockpilot.local/privacy",
                                "https://stockpilot.local/terms"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("StockPilot SAS"))
                .andExpect(jsonPath("$.updatedByEmail").value("legal-owner@test.local"));

        mockMvc.perform(get("/owner/legal-settings")
                        .header("Authorization", "Bearer " + jwtService.generateToken(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legalNotice").value("Mentions legales de test"));
    }

    @Test
    void ownerSupportActionsRequireReasonAndCreateAuditLog() throws Exception {
        Organisation platform = organisationRepository.save(Organisation.builder()
                .name("Support Platform")
                .status("ACTIVE")
                .build());
        Organisation customer = organisationRepository.save(Organisation.builder()
                .name("Support Customer")
                .status("ACTIVE")
                .build());
        User owner = userRepository.save(User.builder()
                .email("support-owner@test.local")
                .password("not-used")
                .organisation(platform)
                .role(Role.OWNER)
                .build());
        User target = userRepository.save(User.builder()
                .email("target@customer.test")
                .password("not-used")
                .organisation(customer)
                .role(Role.ADMIN)
                .enabled(true)
                .build());

        mockMvc.perform(patch("/owner/users/{id}/status", target.getId())
                        .header("Authorization", "Bearer " + jwtService.generateToken(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/owner/users/{id}/status", target.getId())
                        .header("Authorization", "Bearer " + jwtService.generateToken(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false,\"reason\":\"Demande support client\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        org.assertj.core.api.Assertions.assertThat(auditLogRepository.findAll())
                .anyMatch(log -> "OWNER_SUPPORT_USER_DISABLED".equals(log.getAction())
                        && "support-owner@test.local".equals(log.getActorEmail())
                        && log.getSummary().contains("Demande support client"));
    }

    private record LegalRequest(
            String companyName,
            String legalNotice,
            String privacyPolicy,
            String terms,
            String legalDocumentUrl,
            String privacyDocumentUrl,
            String termsDocumentUrl
    ) {
    }
}
