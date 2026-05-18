package com.gestionstock.admin.presentation.controller;

import com.gestionstock.ai.infrastructure.entity.AiAuditLogEntity;
import com.gestionstock.ai.infrastructure.entity.AiReorderRecommendationEntity;
import com.gestionstock.ai.infrastructure.entity.AiStockoutRiskEntity;
import com.gestionstock.ai.infrastructure.repository.AiAuditLogRepository;
import com.gestionstock.ai.infrastructure.repository.AiReorderRecommendationRepository;
import com.gestionstock.ai.infrastructure.repository.AiStockoutRiskRepository;
import com.gestionstock.iam.infrastructure.entity.Organisation;
import com.gestionstock.iam.infrastructure.entity.Role;
import com.gestionstock.iam.infrastructure.entity.User;
import com.gestionstock.iam.infrastructure.repository.OrganisationRepository;
import com.gestionstock.iam.infrastructure.repository.UserRepository;
import com.gestionstock.notification.infrastructure.entity.AdminNotificationEntity;
import com.gestionstock.notification.infrastructure.repository.AdminNotificationJpaRepository;
import com.gestionstock.procurement.domain.model.PurchaseOrderStatus;
import com.gestionstock.procurement.infrastructure.entity.PurchaseOrderEntity;
import com.gestionstock.procurement.infrastructure.entity.SupplierEntity;
import com.gestionstock.procurement.infrastructure.repository.PurchaseOrderJpaRepository;
import com.gestionstock.procurement.infrastructure.repository.SupplierJpaRepository;
import com.gestionstock.product.infrastructure.entity.ProductEntity;
import com.gestionstock.product.infrastructure.repository.ProductJpaRepository;
import com.gestionstock.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExecutiveTimelineControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductJpaRepository productRepository;

    @Autowired
    private SupplierJpaRepository supplierRepository;

    @Autowired
    private PurchaseOrderJpaRepository purchaseOrderRepository;

    @Autowired
    private AdminNotificationJpaRepository notificationRepository;

    @Autowired
    private AiStockoutRiskRepository stockoutRiskRepository;

    @Autowired
    private AiReorderRecommendationRepository recommendationRepository;

    @Autowired
    private AiAuditLogRepository auditLogRepository;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        notificationRepository.deleteAll();
        recommendationRepository.deleteAll();
        stockoutRiskRepository.deleteAll();
        purchaseOrderRepository.deleteAll();
        supplierRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        organisationRepository.deleteAll();
    }

    @Test
    void adminCanReadTodayDecisionTimeline() throws Exception {
        Organisation organisation = organisationRepository.save(Organisation.builder().name("Timeline Org").build());
        User admin = userRepository.save(User.builder()
                .email("admin@timeline.test")
                .password("not-used")
                .organisation(organisation)
                .role(Role.ADMIN)
                .build());
        ProductEntity product = productRepository.save(ProductEntity.builder()
                .organisationId(organisation.getId())
                .name("Filtres premium")
                .sku("TL-001")
                .category("Garage")
                .minStock(8)
                .unit("pcs")
                .build());
        SupplierEntity supplier = supplierRepository.save(SupplierEntity.builder()
                .organisationId(organisation.getId())
                .name("Supplier Timeline")
                .email("supplier@timeline.test")
                .phone("0102030405")
                .leadTimeDays(4)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build());
        PurchaseOrderEntity order = purchaseOrderRepository.save(PurchaseOrderEntity.builder()
                .organisationId(organisation.getId())
                .supplierId(supplier.getId())
                .status(PurchaseOrderStatus.DRAFT)
                .expectedDeliveryDate(LocalDate.now().plusDays(4))
                .createdAt(LocalDateTime.now())
                .lines(new ArrayList<>())
                .build());

        notificationRepository.save(AdminNotificationEntity.builder()
                .organisationId(organisation.getId())
                .type("APPROVAL")
                .severity("CRITICAL")
                .title("Commande a valider")
                .message("Commande fournisseur au-dessus du seuil.")
                .purchaseOrderId(order.getId())
                .supplierId(supplier.getId())
                .deduplicationKey("timeline-" + order.getId())
                .status("OPEN")
                .createdAt(LocalDateTime.now())
                .build());
        stockoutRiskRepository.save(AiStockoutRiskEntity.builder()
                .organisationId(organisation.getId())
                .productId(product.getId())
                .estimatedStockoutDate(LocalDate.now().plusDays(2))
                .riskScore(BigDecimal.valueOf(88))
                .riskLevel("HIGH")
                .reason("Demande acceleree")
                .generatedAt(LocalDateTime.now())
                .build());
        recommendationRepository.save(AiReorderRecommendationEntity.builder()
                .organisationId(organisation.getId())
                .productId(product.getId())
                .recommendedQuantity(40)
                .leadTimeDays(4)
                .safetyStock(12)
                .reason("Couverture insuffisante")
                .status("PENDING")
                .generatedAt(LocalDateTime.now())
                .build());
        auditLogRepository.save(AiAuditLogEntity.builder()
                .organisationId(organisation.getId())
                .userId(admin.getId())
                .actorEmail(admin.getEmail())
                .action("PURCHASE_ORDER_CREATED")
                .targetType("PURCHASE_ORDER")
                .targetId(order.getId())
                .source("BACKEND")
                .summary("Commande fournisseur creee depuis une recommandation IA")
                .createdAt(LocalDateTime.now())
                .build());

        mockMvc.perform(get("/admin/executive-timeline")
                        .param("date", LocalDate.now().toString())
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.criticalRisks").value(1))
                .andExpect(jsonPath("$.openNotifications").value(1))
                .andExpect(jsonPath("$.pendingOrders").value(1))
                .andExpect(jsonPath("$.pendingRecommendations").value(1))
                .andExpect(jsonPath("$.executiveSummary").exists())
                .andExpect(jsonPath("$.keyDecisions", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.items", hasSize(greaterThan(3))))
                .andExpect(jsonPath("$.actions", hasSize(greaterThan(2))));

        mockMvc.perform(get("/admin/executive-timeline/export/csv")
                        .param("date", LocalDate.now().toString())
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin)))
                .andExpect(status().isOk())
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentAsString())
                        .contains("date,priorityScore,type,severity,title,description,occurredAt")
                        .contains("AI_RISK"));

        mockMvc.perform(get("/admin/executive-timeline/export/pdf")
                        .param("date", LocalDate.now().toString())
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin)))
                .andExpect(status().isOk())
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentAsByteArray().length)
                        .isGreaterThan(100));
    }

    @Test
    void userCannotReadExecutiveTimeline() throws Exception {
        Organisation organisation = organisationRepository.save(Organisation.builder().name("Timeline User Org").build());
        User user = userRepository.save(User.builder()
                .email("user@timeline.test")
                .password("not-used")
                .organisation(organisation)
                .role(Role.USER)
                .build());

        mockMvc.perform(get("/admin/executive-timeline")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isForbidden());
    }
}
