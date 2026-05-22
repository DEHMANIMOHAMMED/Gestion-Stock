package com.gestionstock.procurement.presentation.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gestionstock.ai.infrastructure.repository.*;
import com.gestionstock.ai.infrastructure.entity.AiReorderRecommendationEntity;
import com.gestionstock.ai.infrastructure.entity.AiStockoutRiskEntity;
import com.gestionstock.iam.infrastructure.entity.Organisation;
import com.gestionstock.iam.infrastructure.entity.Role;
import com.gestionstock.iam.infrastructure.entity.User;
import com.gestionstock.iam.infrastructure.repository.OrganisationRepository;
import com.gestionstock.iam.infrastructure.repository.UserRepository;
import com.gestionstock.notification.infrastructure.repository.AdminNotificationJpaRepository;
import com.gestionstock.notification.infrastructure.repository.AdminNotificationActionJpaRepository;
import com.gestionstock.procurement.infrastructure.entity.SupplierEntity;
import com.gestionstock.procurement.infrastructure.entity.ProductSupplierEntity;
import com.gestionstock.procurement.infrastructure.repository.PurchaseOrderJpaRepository;
import com.gestionstock.procurement.infrastructure.repository.PurchaseOrderAuditLogJpaRepository;
import com.gestionstock.procurement.infrastructure.repository.ProcurementApprovalSettingsJpaRepository;
import com.gestionstock.procurement.infrastructure.repository.ProductSupplierJpaRepository;
import com.gestionstock.procurement.infrastructure.repository.SupplierJpaRepository;
import com.gestionstock.product.infrastructure.entity.ProductEntity;
import com.gestionstock.product.infrastructure.repository.ProductJpaRepository;
import com.gestionstock.security.JwtService;
import com.gestionstock.stock.infrastructure.repository.StockJpaRepository;
import com.gestionstock.stock.infrastructure.repository.StockMovementJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PurchaseOrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SupplierJpaRepository supplierRepository;

    @Autowired
    private PurchaseOrderJpaRepository purchaseOrderRepository;

    @Autowired
    private PurchaseOrderAuditLogJpaRepository auditLogRepository;

    @Autowired
    private ProcurementApprovalSettingsJpaRepository approvalSettingsRepository;

    @Autowired
    private AdminNotificationJpaRepository notificationRepository;

    @Autowired
    private AdminNotificationActionJpaRepository notificationActionRepository;

    @Autowired
    private ProductSupplierJpaRepository productSupplierRepository;

    @Autowired
    private ProductJpaRepository productRepository;

    @Autowired
    private StockJpaRepository stockRepository;

    @Autowired
    private StockMovementJpaRepository movementRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AiRunRepository aiRunRepository;

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
    private AiAuditLogRepository centralAuditLogRepository;

    @BeforeEach
    void setUp() {
        cleanup();
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        aiRunRepository.deleteAll();
        centralAuditLogRepository.deleteAll();
        anomalyRepository.deleteAll();
        reorderRepository.deleteAll();
        stockoutRiskRepository.deleteAll();
        forecastRepository.deleteAll();
        insightRepository.deleteAll();
        auditLogRepository.deleteAll();
        notificationActionRepository.deleteAll();
        notificationRepository.deleteAll();
        approvalSettingsRepository.deleteAll();
        purchaseOrderRepository.deleteAll();
        productSupplierRepository.deleteAll();
        supplierRepository.deleteAll();
        movementRepository.deleteAll();
        stockRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        organisationRepository.deleteAll();
    }

    @Test
    void receivePurchaseOrderIncrementsTenantStockAndCreatesMovement() throws Exception {
        Organisation organisation = organisationRepository.save(Organisation.builder().name("Procurement Org").build());
        User user = userRepository.save(User.builder()
                .email("admin@procurement.test")
                .password("not-used")
                .organisation(organisation)
                .role(Role.ADMIN)
                .build());
        ProductEntity product = productRepository.save(ProductEntity.builder()
                .organisationId(organisation.getId())
                .name("Brake Pads")
                .sku("BRK-001")
                .minStock(4)
                .unit("pcs")
                .build());
        SupplierEntity supplier = supplierRepository.save(SupplierEntity.builder()
                .organisationId(organisation.getId())
                .name("Garage Supply")
                .leadTimeDays(5)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build());

        String token = jwtService.generateToken(user);
        String body = """
                {
                  "supplierId": %d,
                  "lines": [
                    { "productId": %d, "quantity": 12, "unitCost": 9.50 }
                  ]
                }
                """.formatted(supplier.getId(), product.getId());

        String response = mockMvc.perform(post("/purchase-orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ORDERED"))
                .andExpect(jsonPath("$.lines", hasSize(1)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        Long orderId = json.get("id").asLong();

        mockMvc.perform(post("/purchase-orders/{id}/receive", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        var stock = stockRepository.findByProductIdAndOrganisationId(product.getId(), organisation.getId()).orElseThrow();
        assertThat(stock.getQuantity()).isEqualTo(12);
        assertThat(movementRepository.countByOrganisationId(organisation.getId())).isEqualTo(1);
    }

    @Test
    void createDraftPurchaseOrderFromAiRecommendation() throws Exception {
        Organisation organisation = organisationRepository.save(Organisation.builder().name("AI Procurement Org").build());
        User user = userRepository.save(User.builder()
                .email("admin@ai-procurement.test")
                .password("not-used")
                .organisation(organisation)
                .role(Role.ADMIN)
                .build());
        ProductEntity product = productRepository.save(ProductEntity.builder()
                .organisationId(organisation.getId())
                .name("Printer Paper")
                .sku("PAPER-001")
                .minStock(10)
                .unit("box")
                .build());
        SupplierEntity supplier = supplierRepository.save(SupplierEntity.builder()
                .organisationId(organisation.getId())
                .name("Office Supply")
                .leadTimeDays(3)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build());
        AiReorderRecommendationEntity recommendation = reorderRepository.save(AiReorderRecommendationEntity.builder()
                .organisationId(organisation.getId())
                .productId(product.getId())
                .recommendedQuantity(25)
                .leadTimeDays(3)
                .safetyStock(10)
                .reason("AI stock coverage recommendation")
                .status("PENDING")
                .generatedAt(LocalDateTime.now())
                .build());

        String body = """
                {
                  "recommendationId": %d,
                  "supplierId": %d
                }
                """.formatted(recommendation.getId(), supplier.getId());

        mockMvc.perform(post("/purchase-orders/from-recommendation")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.lines[0].productId").value(product.getId()))
                .andExpect(jsonPath("$.lines[0].quantity").value(25))
                .andExpect(jsonPath("$.lines[0].receivedQuantity").value(0));

        var updatedRecommendation = reorderRepository.findById(recommendation.getId()).orElseThrow();
        assertThat(updatedRecommendation.getStatus()).isEqualTo("APPROVED");
        assertThat(updatedRecommendation.getPurchaseOrderId()).isNotNull();
    }

    @Test
    void createDraftPurchaseOrderFromAiRecommendationUsesBestProductSupplierAutomatically() throws Exception {
        Organisation organisation = organisationRepository.save(Organisation.builder().name("Auto Supplier Org").build());
        User user = userRepository.save(User.builder()
                .email("admin@auto-supplier.test")
                .password("not-used")
                .organisation(organisation)
                .role(Role.ADMIN)
                .build());
        ProductEntity product = productRepository.save(ProductEntity.builder()
                .organisationId(organisation.getId())
                .name("Oil Filter")
                .sku("OIL-FLT")
                .minStock(5)
                .unit("pcs")
                .build());
        SupplierEntity preferredSupplier = supplierRepository.save(SupplierEntity.builder()
                .organisationId(organisation.getId())
                .name("Preferred Auto Parts")
                .leadTimeDays(10)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build());
        SupplierEntity bestSupplier = supplierRepository.save(SupplierEntity.builder()
                .organisationId(organisation.getId())
                .name("Best Scored Auto Parts")
                .leadTimeDays(2)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build());
        productSupplierRepository.save(ProductSupplierEntity.builder()
                .organisationId(organisation.getId())
                .productId(product.getId())
                .supplierId(preferredSupplier.getId())
                .unitCost(BigDecimal.valueOf(12.00))
                .minimumOrderQuantity(20)
                .preferred(true)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build());
        productSupplierRepository.save(ProductSupplierEntity.builder()
                .organisationId(organisation.getId())
                .productId(product.getId())
                .supplierId(bestSupplier.getId())
                .unitCost(BigDecimal.valueOf(3.75))
                .minimumOrderQuantity(40)
                .preferred(false)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build());
        AiReorderRecommendationEntity recommendation = reorderRepository.save(AiReorderRecommendationEntity.builder()
                .organisationId(organisation.getId())
                .productId(product.getId())
                .recommendedQuantity(25)
                .leadTimeDays(2)
                .safetyStock(8)
                .reason("Auto supplier recommendation")
                .status("PENDING")
                .generatedAt(LocalDateTime.now())
                .build());

        mockMvc.perform(post("/purchase-orders/from-recommendation")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recommendationId": %d
                                }
                                """.formatted(recommendation.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.supplierId").value(bestSupplier.getId()))
                .andExpect(jsonPath("$.lines[0].quantity").value(40))
                .andExpect(jsonPath("$.lines[0].unitCost").value(3.75));

        assertThat(reorderRepository.findById(recommendation.getId()).orElseThrow().getPurchaseOrderId()).isNotNull();
    }

    @Test
    void supplier360ReturnsSupplierMetricsProductsAndRecommendations() throws Exception {
        Organisation organisation = organisationRepository.save(Organisation.builder().name("Supplier 360 Org").build());
        User user = userRepository.save(User.builder()
                .email("admin@supplier360.test")
                .password("not-used")
                .organisation(organisation)
                .role(Role.ADMIN)
                .build());
        ProductEntity product = productRepository.save(ProductEntity.builder()
                .organisationId(organisation.getId())
                .name("Brake Fluid")
                .sku("BRK-FLUID")
                .minStock(6)
                .unit("bottle")
                .build());
        SupplierEntity supplier = supplierRepository.save(SupplierEntity.builder()
                .organisationId(organisation.getId())
                .name("Supplier 360 Parts")
                .leadTimeDays(3)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build());
        productSupplierRepository.save(ProductSupplierEntity.builder()
                .organisationId(organisation.getId())
                .productId(product.getId())
                .supplierId(supplier.getId())
                .unitCost(BigDecimal.valueOf(7.50))
                .minimumOrderQuantity(10)
                .preferred(true)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build());

        String token = jwtService.generateToken(user);
        String createResponse = mockMvc.perform(post("/purchase-orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "supplierId": %d,
                                  "expectedDeliveryDate": "%s",
                                  "lines": [
                                    { "productId": %d, "quantity": 12, "unitCost": 7.50 }
                                  ]
                                }
                                """.formatted(supplier.getId(), java.time.LocalDate.now().plusDays(1), product.getId())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long orderId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(post("/purchase-orders/{id}/receive", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/suppliers/{id}/360", supplier.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Supplier 360 Parts"))
                .andExpect(jsonPath("$.totalOrders").value(1))
                .andExpect(jsonPath("$.coveredProducts").value(1))
                .andExpect(jsonPath("$.orderedQuantity").value(12))
                .andExpect(jsonPath("$.receivedQuantity").value(12))
                .andExpect(jsonPath("$.products[0].productName").value("Brake Fluid"))
                .andExpect(jsonPath("$.recentOrders[0].id").value(orderId))
                .andExpect(jsonPath("$.recommendations", hasSize(1)));
    }

    @Test
    void draftPurchaseOrderCanBeUpdatedConfirmedAndPartiallyReceived() throws Exception {
        Organisation organisation = organisationRepository.save(Organisation.builder().name("Draft Workflow Org").build());
        User user = userRepository.save(User.builder()
                .email("admin@draft-workflow.test")
                .password("not-used")
                .organisation(organisation)
                .role(Role.ADMIN)
                .build());
        ProductEntity product = productRepository.save(ProductEntity.builder()
                .organisationId(organisation.getId())
                .name("Filters")
                .sku("FLT-001")
                .minStock(3)
                .unit("pcs")
                .build());
        SupplierEntity supplier = supplierRepository.save(SupplierEntity.builder()
                .organisationId(organisation.getId())
                .name("Filter Supply")
                .leadTimeDays(4)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build());
        AiReorderRecommendationEntity recommendation = reorderRepository.save(AiReorderRecommendationEntity.builder()
                .organisationId(organisation.getId())
                .productId(product.getId())
                .recommendedQuantity(30)
                .leadTimeDays(4)
                .safetyStock(6)
                .reason("Increase coverage")
                .status("PENDING")
                .generatedAt(LocalDateTime.now())
                .build());
        String token = jwtService.generateToken(user);

        String draftResponse = mockMvc.perform(post("/purchase-orders/from-recommendation")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recommendationId": %d,
                                  "supplierId": %d
                                }
                                """.formatted(recommendation.getId(), supplier.getId())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long orderId = objectMapper.readTree(draftResponse).get("id").asLong();

        String updatedResponse = mockMvc.perform(put("/purchase-orders/{id}", orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "supplierId": %d,
                                  "lines": [
                                    { "productId": %d, "quantity": 30, "unitCost": 4.25 }
                                  ]
                                }
                                """.formatted(supplier.getId(), product.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.lines[0].quantity").value(30))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(objectMapper.readTree(updatedResponse).get("lines").get(0).get("receivedQuantity").asInt()).isZero();

        mockMvc.perform(post("/purchase-orders/{id}/approve", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/purchase-orders/{id}/audit", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].action").value("CREATED_DRAFT_FROM_AI"))
                .andExpect(jsonPath("$[1].action").value("APPROVED"))
                .andExpect(jsonPath("$[1].actorEmail").value("admin@draft-workflow.test"))
                .andExpect(jsonPath("$[1].actorRole").value("ADMIN"))
                .andExpect(jsonPath("$[1].orderTotal").value(127.50));

        mockMvc.perform(get("/purchase-orders/{id}/pdf", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentType()).isEqualTo("application/pdf"))
                .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray().length).isGreaterThan(100));

        String confirmedResponse = mockMvc.perform(post("/purchase-orders/{id}/confirm", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ORDERED"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long lineId = objectMapper.readTree(confirmedResponse).get("lines").get(0).get("id").asLong();

        mockMvc.perform(post("/purchase-orders/{id}/receive", orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lines": [
                                    { "lineId": %d, "quantity": 10 }
                                  ]
                                }
                                """.formatted(lineId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ORDERED"))
                .andExpect(jsonPath("$.lines[0].receivedQuantity").value(10));

        assertThat(stockRepository.findByProductIdAndOrganisationId(product.getId(), organisation.getId()).orElseThrow().getQuantity())
                .isEqualTo(10);

        mockMvc.perform(post("/purchase-orders/{id}/receive", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.lines[0].receivedQuantity").value(30));

        assertThat(stockRepository.findByProductIdAndOrganisationId(product.getId(), organisation.getId()).orElseThrow().getQuantity())
                .isEqualTo(30);
        assertThat(movementRepository.countByOrganisationId(organisation.getId())).isEqualTo(2);
        assertThat(centralAuditLogRepository.findTop100ByOrganisationIdOrderByCreatedAtDesc(organisation.getId()).stream()
                .map(log -> log.getAction())
                .toList())
                .contains("PURCHASE_ORDER_CREATED_FROM_AI", "PURCHASE_ORDER_UPDATED_DRAFT",
                        "PURCHASE_ORDER_CONFIRMED", "PURCHASE_ORDER_RECEIVED_PARTIAL", "PURCHASE_ORDER_RECEIVED_FULL");

        mockMvc.perform(get("/purchase-orders/accounting-export")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).contains("order_id,status,supplier"))
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).contains("Filter Supply"));

        mockMvc.perform(get("/purchase-orders/{id}/audit", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[2].action").value("CONFIRMED"))
                .andExpect(jsonPath("$[3].action").value("RECEIVED"));
    }

    @Test
    void usersCannotManagePurchaseOrders() throws Exception {
        Organisation organisation = organisationRepository.save(Organisation.builder().name("Approval Org").build());
        User admin = userRepository.save(User.builder()
                .email("admin@approval.test")
                .password("not-used")
                .organisation(organisation)
                .role(Role.ADMIN)
                .build());
        User regularUser = userRepository.save(User.builder()
                .email("user@approval.test")
                .password("not-used")
                .organisation(organisation)
                .role(Role.USER)
                .build());
        ProductEntity product = productRepository.save(ProductEntity.builder()
                .organisationId(organisation.getId())
                .name("Premium Stock")
                .sku("PREMIUM-001")
                .minStock(1)
                .unit("unit")
                .build());
        SupplierEntity supplier = supplierRepository.save(SupplierEntity.builder()
                .organisationId(organisation.getId())
                .name("Approval Supply")
                .leadTimeDays(4)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build());
        productSupplierRepository.save(ProductSupplierEntity.builder()
                .organisationId(organisation.getId())
                .productId(product.getId())
                .supplierId(supplier.getId())
                .unitCost(BigDecimal.valueOf(75.00))
                .minimumOrderQuantity(1)
                .preferred(true)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build());
        AiReorderRecommendationEntity recommendation = reorderRepository.save(AiReorderRecommendationEntity.builder()
                .organisationId(organisation.getId())
                .productId(product.getId())
                .recommendedQuantity(30)
                .leadTimeDays(4)
                .safetyStock(6)
                .reason("High value order")
                .status("PENDING")
                .generatedAt(LocalDateTime.now())
                .build());

        String userToken = jwtService.generateToken(regularUser);
        String adminToken = jwtService.generateToken(admin);

        mockMvc.perform(post("/purchase-orders")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "supplierId": %d,
                                  "lines": [
                                    { "productId": %d, "quantity": 20, "unitCost": 75.00 }
                                  ]
                                }
                                """.formatted(supplier.getId(), product.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("ADMIN role required"));

        mockMvc.perform(post("/purchase-orders/from-recommendation")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recommendationId": %d,
                                  "supplierId": %d
                                }
                                """.formatted(recommendation.getId(), supplier.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("ADMIN role required"));

        String orderResponse = mockMvc.perform(post("/purchase-orders")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "supplierId": %d,
                                  "lines": [
                                    { "productId": %d, "quantity": 4, "unitCost": 75.00 }
                                  ]
                                }
                                """.formatted(supplier.getId(), product.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ORDERED"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long orderId = objectMapper.readTree(orderResponse).get("id").asLong();

        mockMvc.perform(post("/purchase-orders/{id}/cancel", orderId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("ADMIN role required"));

        mockMvc.perform(post("/purchase-orders/{id}/cancel", orderId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(get("/purchase-orders/{id}/audit", orderId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].actorRole").value("ADMIN"))
                .andExpect(jsonPath("$[1].actorRole").value("ADMIN"));
    }

    @Test
    void adminCanUseApprovalCenterAndUpdateApprovalThreshold() throws Exception {
        Organisation organisation = organisationRepository.save(Organisation.builder().name("Approval Center Org").build());
        User admin = userRepository.save(User.builder()
                .email("admin@approval-center.test")
                .password("not-used")
                .organisation(organisation)
                .role(Role.ADMIN)
                .build());
        User regularUser = userRepository.save(User.builder()
                .email("user@approval-center.test")
                .password("not-used")
                .organisation(organisation)
                .role(Role.USER)
                .build());
        ProductEntity product = productRepository.save(ProductEntity.builder()
                .organisationId(organisation.getId())
                .name("Urgent Filter")
                .sku("URG-FLT")
                .minStock(2)
                .unit("pcs")
                .build());
        SupplierEntity supplier = supplierRepository.save(SupplierEntity.builder()
                .organisationId(organisation.getId())
                .name("Urgent Supply")
                .leadTimeDays(2)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build());
        productSupplierRepository.save(ProductSupplierEntity.builder()
                .organisationId(organisation.getId())
                .productId(product.getId())
                .supplierId(supplier.getId())
                .unitCost(BigDecimal.valueOf(75.00))
                .minimumOrderQuantity(1)
                .preferred(true)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build());
        stockoutRiskRepository.save(AiStockoutRiskEntity.builder()
                .organisationId(organisation.getId())
                .productId(product.getId())
                .estimatedStockoutDate(LocalDate.now().plusDays(3))
                .riskScore(BigDecimal.valueOf(91.50))
                .riskLevel("HIGH")
                .reason("Rupture probable sous 3 jours")
                .generatedAt(LocalDateTime.now())
                .build());

        String adminToken = jwtService.generateToken(admin);
        String userToken = jwtService.generateToken(regularUser);

        mockMvc.perform(get("/purchase-orders/approval-settings")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalThreshold").value(1000))
                .andExpect(jsonPath("$.defaultValue").value(true));

        mockMvc.perform(put("/purchase-orders/approval-settings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approvalThreshold\":2000}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("ADMIN role required"));

        mockMvc.perform(put("/purchase-orders/approval-settings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approvalThreshold\":2000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalThreshold").value(2000))
                .andExpect(jsonPath("$.defaultValue").value(false));

        String createResponse = mockMvc.perform(post("/purchase-orders")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "supplierId": %d,
                                  "lines": [
                                    { "productId": %d, "quantity": 20, "unitCost": 75.00 }
                                  ]
                                }
                                """.formatted(supplier.getId(), product.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ORDERED"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long orderedId = objectMapper.readTree(createResponse).get("id").asLong();

        AiReorderRecommendationEntity recommendation = reorderRepository.save(AiReorderRecommendationEntity.builder()
                .organisationId(organisation.getId())
                .productId(product.getId())
                .recommendedQuantity(30)
                .leadTimeDays(2)
                .safetyStock(5)
                .reason("Urgent AI order")
                .status("PENDING")
                .generatedAt(LocalDateTime.now())
                .build());
        String draftResponse = mockMvc.perform(post("/purchase-orders/from-recommendation")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recommendationId": %d,
                                  "supplierId": %d
                                }
                                """.formatted(recommendation.getId(), supplier.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long draftId = objectMapper.readTree(draftResponse).get("id").asLong();

        mockMvc.perform(get("/purchase-orders/approval-center")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("ADMIN role required"));

        mockMvc.perform(get("/purchase-orders/approval-center")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(draftId))
                .andExpect(jsonPath("$[0].supplierName").value("Urgent Supply"))
                .andExpect(jsonPath("$[0].orderTotal").value(2250.00))
                .andExpect(jsonPath("$[0].maxRiskLevel").value("HIGH"))
                .andExpect(jsonPath("$[0].urgency").value("CRITICAL"))
                .andExpect(jsonPath("$[0].riskReason").value("Rupture probable sous 3 jours"));

        mockMvc.perform(get("/notifications/unread-count")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(2));

        mockMvc.perform(get("/notifications")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].type").value("AI_CRITICAL_STOCKOUT"))
                .andExpect(jsonPath("$[1].type").value("PURCHASE_ORDER_THRESHOLD"));

        var notifications = notificationRepository.findAll();
        var criticalNotification = notifications.stream()
                .filter(notification -> "AI_CRITICAL_STOCKOUT".equals(notification.getType()))
                .findFirst()
                .orElseThrow();
        var thresholdNotification = notifications.stream()
                .filter(notification -> "PURCHASE_ORDER_THRESHOLD".equals(notification.getType()))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(post("/notifications/{id}/action", criticalNotification.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"DISMISS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Critical notifications require a dismissal reason"));

        mockMvc.perform(post("/notifications/{id}/action", criticalNotification.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "DISMISS",
                                  "reason": "Commande traitee par le fournisseur en direct"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISMISSED"))
                .andExpect(jsonPath("$.dismissalReason").value("Commande traitee par le fournisseur en direct"));

        mockMvc.perform(post("/notifications/{id}/action", thresholdNotification.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"APPROVE_ORDER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIONED"))
                .andExpect(jsonPath("$.actionTaken").value("APPROVE_ORDER"));

        mockMvc.perform(get("/notifications/{id}/actions", thresholdNotification.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].action").value("APPROVE_ORDER"))
                .andExpect(jsonPath("$[0].actorUserId").value(admin.getId()));

        assertThat(centralAuditLogRepository.findTop100ByOrganisationIdOrderByCreatedAtDesc(organisation.getId()).stream()
                .map(log -> log.getAction())
                .toList())
                .contains("APPROVAL_THRESHOLD_UPDATED", "PURCHASE_ORDER_CREATED", "PURCHASE_ORDER_CREATED_FROM_AI",
                        "NOTIFICATION_CREATED", "NOTIFICATION_DISMISSED", "NOTIFICATION_ACTIONED");

        assertThat(orderedId).isNotNull();
    }

    @Test
    void purchaseOrdersRequireAuthentication() throws Exception {
        mockMvc.perform(get("/purchase-orders"))
                .andExpect(status().isUnauthorized());
    }
}
