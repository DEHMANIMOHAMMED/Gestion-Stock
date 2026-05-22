package com.gestionstock.ai.presentation.controller;

import com.gestionstock.ai.infrastructure.repository.*;
import com.gestionstock.ai.infrastructure.entity.AiAuditLogEntity;
import com.gestionstock.iam.infrastructure.entity.Organisation;
import com.gestionstock.iam.infrastructure.entity.Role;
import com.gestionstock.iam.infrastructure.entity.User;
import com.gestionstock.iam.infrastructure.repository.OrganisationRepository;
import com.gestionstock.iam.infrastructure.repository.UserRepository;
import com.gestionstock.product.infrastructure.entity.ProductEntity;
import com.gestionstock.product.infrastructure.repository.ProductJpaRepository;
import com.gestionstock.sales.domain.model.SaleStatus;
import com.gestionstock.sales.domain.model.SalesChannel;
import com.gestionstock.sales.infrastructure.entity.SaleEntity;
import com.gestionstock.sales.infrastructure.entity.SaleLineEntity;
import com.gestionstock.sales.infrastructure.repository.SaleJpaRepository;
import com.gestionstock.sales.infrastructure.repository.SaleLineJpaRepository;
import com.gestionstock.security.JwtService;
import com.gestionstock.stock.domain.model.MovementType;
import com.gestionstock.stock.infrastructure.entity.StockEntity;
import com.gestionstock.stock.infrastructure.entity.StockMovementEntity;
import com.gestionstock.stock.infrastructure.repository.StockJpaRepository;
import com.gestionstock.stock.infrastructure.repository.StockMovementJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.math.BigDecimal;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AiControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private StockJpaRepository stockJpaRepository;

    @Autowired
    private StockMovementJpaRepository stockMovementJpaRepository;

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
    private AiRunRepository runRepository;

    @Autowired
    private AiAuditLogRepository auditLogRepository;

    @Autowired
    private AiCopilotMessageRepository copilotMessageRepository;

    @Autowired
    private AiCopilotConversationRepository copilotConversationRepository;

    @Autowired
    private SaleJpaRepository saleJpaRepository;

    @Autowired
    private SaleLineJpaRepository saleLineJpaRepository;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        copilotMessageRepository.deleteAll();
        copilotConversationRepository.deleteAll();
        saleLineJpaRepository.deleteAll();
        saleJpaRepository.deleteAll();
        runRepository.deleteAll();
        auditLogRepository.deleteAll();
        anomalyRepository.deleteAll();
        reorderRepository.deleteAll();
        stockoutRiskRepository.deleteAll();
        forecastRepository.deleteAll();
        insightRepository.deleteAll();
        stockMovementJpaRepository.deleteAll();
        stockJpaRepository.deleteAll();
        productJpaRepository.deleteAll();
        userRepository.deleteAll();
        organisationRepository.deleteAll();
    }

    @Test
    void manualAiRunIsQueuedForTenant() throws Exception {
        Organisation organisation = organisationRepository.save(Organisation.builder().name("Run Org").build());
        User user = userRepository.save(User.builder()
                .email("admin@run-org.test")
                .password("not-used")
                .organisation(organisation)
                .role(Role.ADMIN)
                .build());

        mockMvc.perform(post("/ai/runs")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.triggerType").value("MANUAL"))
                .andExpect(jsonPath("$.status").exists());

        waitForAsyncAiRuns();
    }

    @Test
    void aiDashboardGeneratesTenantScopedDecisionData() throws Exception {
        Organisation organisation = organisationRepository.save(Organisation.builder().name("AI Org").build());
        User user = userRepository.save(User.builder()
                .email("admin@ai-org.test")
                .password("not-used")
                .organisation(organisation)
                .role(Role.ADMIN)
                .build());

        ProductEntity product = productJpaRepository.save(ProductEntity.builder()
                .organisationId(organisation.getId())
                .name("Scanner")
                .sku("AI-SCAN-1")
                .category("Hardware")
                .minStock(10)
                .unit("pcs")
                .build());

        stockJpaRepository.save(StockEntity.builder()
                .organisationId(organisation.getId())
                .productId(product.getId())
                .quantity(3)
                .build());

        stockMovementJpaRepository.save(StockMovementEntity.builder()
                .organisationId(organisation.getId())
                .productId(product.getId())
                .quantity(12)
                .type(MovementType.OUT)
                .createdAt(LocalDateTime.now().minusDays(2))
                .build());

        mockMvc.perform(get("/ai/dashboard")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forecasts", hasSize(3)))
                .andExpect(jsonPath("$.stockoutRisks", hasSize(1)))
                .andExpect(jsonPath("$.stockoutRisks[0].productName").value("Scanner"))
                .andExpect(jsonPath("$.reorderRecommendations", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.insights", hasSize(1)));
    }

    @Test
    void aiDashboardExposesForecastQualityFromSalesDemand() throws Exception {
        Organisation organisation = organisationRepository.save(Organisation.builder().name("Forecast Quality Org").build());
        User user = userRepository.save(User.builder()
                .email("admin@forecast-quality.test")
                .password("not-used")
                .organisation(organisation)
                .role(Role.ADMIN)
                .build());
        ProductEntity product = productJpaRepository.save(ProductEntity.builder()
                .organisationId(organisation.getId())
                .name("Produit tendance")
                .sku("FQ-1")
                .category("Retail")
                .minStock(5)
                .unit("pcs")
                .build());
        stockJpaRepository.save(StockEntity.builder()
                .organisationId(organisation.getId())
                .productId(product.getId())
                .quantity(80)
                .build());
        saveSale(organisation.getId(), product.getId(), "FQ-OLD", 5, LocalDateTime.now().minusDays(40));
        saveSale(organisation.getId(), product.getId(), "FQ-NEW-1", 8, LocalDateTime.now().minusDays(8));
        saveSale(organisation.getId(), product.getId(), "FQ-NEW-2", 9, LocalDateTime.now().minusDays(2));

        mockMvc.perform(get("/ai/dashboard")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forecasts[0].confidenceLevel").exists())
                .andExpect(jsonPath("$.forecasts[0].backtestErrorPercent").exists())
                .andExpect(jsonPath("$.forecasts[0].demandTrendPercent").exists())
                .andExpect(jsonPath("$.forecasts[0].salesVolume30Days").value(17))
                .andExpect(jsonPath("$.forecasts[0].demandSignal").value("Demande en hausse"))
                .andExpect(jsonPath("$.forecasts[0].selectedModel").exists())
                .andExpect(jsonPath("$.forecasts[0].modelSelectionReason").exists())
                .andExpect(jsonPath("$.forecasts[0].movingAverageError").exists())
                .andExpect(jsonPath("$.forecasts[0].seasonalError").exists());

        mockMvc.perform(get("/ai/forecasts/backtest")
                        .param("productId", product.getId().toString())
                        .param("horizon", "30")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value(product.getId()))
                .andExpect(jsonPath("$[0].points", hasSize(30)))
                .andExpect(jsonPath("$[0].mae").exists())
                .andExpect(jsonPath("$[0].mape").exists())
                .andExpect(jsonPath("$[0].reliabilityScore").exists())
                .andExpect(jsonPath("$[0].qualityLevel").exists());
    }

    @Test
    void aiEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/ai/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void copilotReturnsTenantScopedActionSuggestions() throws Exception {
        Organisation organisation = organisationRepository.save(Organisation.builder().name("Copilot Action Org").build());
        User user = userRepository.saveAndFlush(User.builder()
                .email("admin@copilot-action.test")
                .password("not-used")
                .organisation(organisation)
                .role(Role.ADMIN)
                .build());

        ProductEntity product = productJpaRepository.save(ProductEntity.builder()
                .organisationId(organisation.getId())
                .name("Cartouche urgente")
                .sku("COP-ACT-1")
                .category("Retail")
                .minStock(10)
                .unit("pcs")
                .build());

        stockJpaRepository.save(StockEntity.builder()
                .organisationId(organisation.getId())
                .productId(product.getId())
                .quantity(2)
                .build());

        stockMovementJpaRepository.save(StockMovementEntity.builder()
                .organisationId(organisation.getId())
                .productId(product.getId())
                .quantity(8)
                .type(MovementType.OUT)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build());

        mockMvc.perform(post("/ai/copilot")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"Quelles commandes dois-je creer aujourd'hui ?\",\"conversationId\":null}")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actions", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.actions[0].type").value("CREATE_PURCHASE_ORDER"))
                .andExpect(jsonPath("$.actions[0].requiresAdminConfirmation").value(true));
    }

    @Test
    void auditLogsAreFilteredAndAdminOnly() throws Exception {
        Organisation organisation = organisationRepository.save(Organisation.builder().name("Audit Org").build());
        User admin = userRepository.save(User.builder()
                .email("admin@audit-org.test")
                .password("not-used")
                .organisation(organisation)
                .role(Role.ADMIN)
                .build());
        User user = userRepository.save(User.builder()
                .email("user@audit-org.test")
                .password("not-used")
                .organisation(organisation)
                .role(Role.USER)
                .build());

        auditLogRepository.save(AiAuditLogEntity.builder()
                .organisationId(organisation.getId())
                .userId(admin.getId())
                .actorEmail(admin.getEmail())
                .action("COPILOT_ASK")
                .targetType("COPILOT_CONVERSATION")
                .targetId(42L)
                .source("LOCAL_FALLBACK")
                .summary("Question sur les ruptures")
                .createdAt(LocalDateTime.now())
                .build());
        auditLogRepository.save(AiAuditLogEntity.builder()
                .organisationId(organisation.getId())
                .userId(admin.getId())
                .actorEmail(admin.getEmail())
                .action("RECOMMENDATION_EXPLAIN")
                .targetType("AI_REORDER_RECOMMENDATION")
                .targetId(84L)
                .source("OPENAI")
                .summary("Explication commande")
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .build());

        mockMvc.perform(get("/ai/audit-logs")
                        .param("action", "COPILOT")
                        .param("module", "AI")
                        .param("severity", "INFO")
                        .param("from", java.time.LocalDate.now().minusDays(1).toString())
                        .param("to", java.time.LocalDate.now().toString())
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].action").value("COPILOT_ASK"))
                .andExpect(jsonPath("$[0].actorEmail").value("admin@audit-org.test"));

        mockMvc.perform(get("/ai/audit-logs/export/csv")
                        .param("module", "AI")
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin)))
                .andExpect(status().isOk())
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentAsString())
                        .contains("id,createdAt,actorEmail,module,severity,action,targetType,targetId,source,summary")
                        .contains("COPILOT_ASK"));

        mockMvc.perform(get("/ai/audit-logs/export/pdf")
                        .param("module", "AI")
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin)))
                .andExpect(status().isOk())
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentAsByteArray().length)
                        .isGreaterThan(100));

        mockMvc.perform(get("/ai/audit-logs")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isForbidden());
    }

    private void waitForAsyncAiRuns() throws InterruptedException {
        for (int attempt = 0; attempt < 50; attempt++) {
            boolean running = runRepository.findAll().stream()
                    .anyMatch(run -> "QUEUED".equals(run.getStatus()) || "RUNNING".equals(run.getStatus()));
            if (!running) {
                return;
            }
            Thread.sleep(100);
        }
    }

    private void saveSale(Long organisationId, Long productId, String reference, int quantity, LocalDateTime soldAt) {
        SaleEntity sale = saleJpaRepository.save(SaleEntity.builder()
                .organisationId(organisationId)
                .reference(reference)
                .customerName("Client")
                .channel(SalesChannel.STORE)
                .status(SaleStatus.COMPLETED)
                .totalAmount(BigDecimal.valueOf(quantity * 10L))
                .soldAt(soldAt)
                .createdAt(soldAt)
                .build());
        saleLineJpaRepository.save(SaleLineEntity.builder()
                .saleId(sale.getId())
                .organisationId(organisationId)
                .productId(productId)
                .quantity(quantity)
                .unitPrice(BigDecimal.TEN)
                .lineTotal(BigDecimal.valueOf(quantity * 10L))
                .build());
    }
}
