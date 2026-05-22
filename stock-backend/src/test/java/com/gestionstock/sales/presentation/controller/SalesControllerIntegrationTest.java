package com.gestionstock.sales.presentation.controller;

import com.gestionstock.ai.infrastructure.entity.AiForecastEntity;
import com.gestionstock.ai.infrastructure.repository.AiForecastRepository;
import com.gestionstock.iam.infrastructure.entity.Organisation;
import com.gestionstock.iam.infrastructure.entity.Role;
import com.gestionstock.iam.infrastructure.entity.User;
import com.gestionstock.iam.infrastructure.repository.OrganisationRepository;
import com.gestionstock.iam.infrastructure.repository.UserRepository;
import com.gestionstock.notification.infrastructure.repository.AdminNotificationActionJpaRepository;
import com.gestionstock.notification.infrastructure.repository.AdminNotificationJpaRepository;
import com.gestionstock.notification.infrastructure.repository.AdminNotificationPreferencesJpaRepository;
import com.gestionstock.product.infrastructure.entity.ProductEntity;
import com.gestionstock.product.infrastructure.repository.ProductJpaRepository;
import com.gestionstock.sales.infrastructure.repository.SaleJpaRepository;
import com.gestionstock.sales.infrastructure.repository.SaleLineJpaRepository;
import com.gestionstock.security.JwtService;
import com.gestionstock.stock.domain.model.MovementType;
import com.gestionstock.stock.infrastructure.entity.StockEntity;
import com.gestionstock.stock.infrastructure.repository.StockJpaRepository;
import com.gestionstock.stock.infrastructure.repository.StockMovementJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SalesControllerIntegrationTest {

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
    private SaleJpaRepository saleJpaRepository;

    @Autowired
    private SaleLineJpaRepository saleLineJpaRepository;

    @Autowired
    private AiForecastRepository aiForecastRepository;

    @Autowired
    private AdminNotificationJpaRepository adminNotificationJpaRepository;

    @Autowired
    private AdminNotificationActionJpaRepository adminNotificationActionJpaRepository;

    @Autowired
    private AdminNotificationPreferencesJpaRepository adminNotificationPreferencesJpaRepository;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        adminNotificationActionJpaRepository.deleteAll();
        adminNotificationPreferencesJpaRepository.deleteAll();
        adminNotificationJpaRepository.deleteAll();
        saleLineJpaRepository.deleteAll();
        saleJpaRepository.deleteAll();
        aiForecastRepository.deleteAll();
        stockMovementJpaRepository.deleteAll();
        stockJpaRepository.deleteAll();
        productJpaRepository.deleteAll();
        userRepository.deleteAll();
        organisationRepository.deleteAll();
    }

    @Test
    void createSaleDecrementsStockAndCreatesOutMovement() throws Exception {
        Organisation organisation = organisationRepository.save(Organisation.builder().name("Sales Org").build());
        User user = userRepository.save(User.builder()
                .email("admin@sales.test")
                .password("not-used")
                .organisation(organisation)
                .role(Role.ADMIN)
                .build());
        ProductEntity product = productJpaRepository.save(ProductEntity.builder()
                .organisationId(organisation.getId())
                .name("Filtre huile")
                .sku("SALE-1")
                .minStock(2)
                .unit("pcs")
                .build());
        stockJpaRepository.save(StockEntity.builder()
                .organisationId(organisation.getId())
                .productId(product.getId())
                .quantity(10)
                .build());

        mockMvc.perform(post("/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwtService.generateToken(user))
                        .content("""
                                {
                                  "customerName": "Garage Atlas",
                                  "channel": "STORE",
                                  "lines": [
                                    { "productId": %d, "quantity": 3, "unitPrice": 15.50 }
                                  ]
                                }
                                """.formatted(product.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").exists())
                .andExpect(jsonPath("$.totalAmount").value(46.50))
                .andExpect(jsonPath("$.lines", hasSize(1)));

        assertThat(stockJpaRepository.findByProductIdAndOrganisationId(product.getId(), organisation.getId()).orElseThrow().getQuantity())
                .isEqualTo(7);
        assertThat(stockMovementJpaRepository.countByOrganisationIdAndProductIdAndType(
                organisation.getId(),
                product.getId(),
                MovementType.OUT
        )).isEqualTo(1);
    }

    @Test
    void createSaleRejectsInsufficientStock() throws Exception {
        Organisation organisation = organisationRepository.save(Organisation.builder().name("Sales Reject Org").build());
        User user = userRepository.save(User.builder()
                .email("admin@sales-reject.test")
                .password("not-used")
                .organisation(organisation)
                .role(Role.ADMIN)
                .build());
        ProductEntity product = productJpaRepository.save(ProductEntity.builder()
                .organisationId(organisation.getId())
                .name("Plaquettes frein")
                .sku("SALE-2")
                .minStock(2)
                .unit("pcs")
                .build());
        stockJpaRepository.save(StockEntity.builder()
                .organisationId(organisation.getId())
                .productId(product.getId())
                .quantity(1)
                .build());

        mockMvc.perform(post("/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwtService.generateToken(user))
                        .content("""
                                {
                                  "customerName": "Client comptoir",
                                  "channel": "STORE",
                                  "lines": [
                                    { "productId": %d, "quantity": 2, "unitPrice": 40.00 }
                                  ]
                                }
                                """.formatted(product.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Insufficient stock for product " + product.getId()));
    }

    @Test
    void salesAreTenantScoped() throws Exception {
        Organisation org1 = organisationRepository.save(Organisation.builder().name("Sales Tenant 1").build());
        Organisation org2 = organisationRepository.save(Organisation.builder().name("Sales Tenant 2").build());
        User user = userRepository.save(User.builder()
                .email("admin@sales-tenant.test")
                .password("not-used")
                .organisation(org1)
                .role(Role.ADMIN)
                .build());
        ProductEntity product1 = productJpaRepository.save(ProductEntity.builder()
                .organisationId(org1.getId())
                .name("Produit T1")
                .sku("SALE-T1")
                .minStock(1)
                .unit("pcs")
                .build());
        ProductEntity product2 = productJpaRepository.save(ProductEntity.builder()
                .organisationId(org2.getId())
                .name("Produit T2")
                .sku("SALE-T2")
                .minStock(1)
                .unit("pcs")
                .build());
        stockJpaRepository.save(StockEntity.builder().organisationId(org1.getId()).productId(product1.getId()).quantity(5).build());
        stockJpaRepository.save(StockEntity.builder().organisationId(org2.getId()).productId(product2.getId()).quantity(5).build());

        mockMvc.perform(post("/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwtService.generateToken(user))
                        .content("""
                                {
                                  "customerName": "Client",
                                  "channel": "WEB",
                                  "lines": [
                                    { "productId": %d, "quantity": 1, "unitPrice": 10.00 }
                                  ]
                                }
                                """.formatted(product1.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/sales")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].lines[0].sku").value("SALE-T1"));
    }

    @Test
    void analyticsExposeDemandSeriesChannelsAndTrends() throws Exception {
        Organisation organisation = organisationRepository.save(Organisation.builder().name("Sales Analytics Org").build());
        User user = userRepository.save(User.builder()
                .email("admin@sales-analytics.test")
                .password("not-used")
                .organisation(organisation)
                .role(Role.ADMIN)
                .build());
        ProductEntity product = productJpaRepository.save(ProductEntity.builder()
                .organisationId(organisation.getId())
                .name("Huile moteur")
                .sku("AN-1")
                .minStock(5)
                .unit("pcs")
                .build());
        stockJpaRepository.save(StockEntity.builder()
                .organisationId(organisation.getId())
                .productId(product.getId())
                .quantity(6)
                .build());

        createSale(user, product.getId(), 2, "STORE");
        createSale(user, product.getId(), 4, "WEB");

        mockMvc.perform(get("/sales/analytics")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailySeries", hasSize(30)))
                .andExpect(jsonPath("$.channels", hasSize(2)))
                .andExpect(jsonPath("$.topRisingProducts[0].sku").value("AN-1"))
                .andExpect(jsonPath("$.highDemandLowStockProducts[0].sku").value("AN-1"));
    }

    @Test
    void analyticsExposeForecastComparisonSeasonalityAndModelAlerts() throws Exception {
        Organisation organisation = organisationRepository.save(Organisation.builder()
                .name("Sales Forecast Org")
                .industry("garage")
                .build());
        User user = userRepository.save(User.builder()
                .email("admin@sales-forecast.test")
                .password("not-used")
                .organisation(organisation)
                .role(Role.ADMIN)
                .build());
        ProductEntity product = productJpaRepository.save(ProductEntity.builder()
                .organisationId(organisation.getId())
                .name("Batterie 12V")
                .sku("FC-1")
                .minStock(5)
                .unit("pcs")
                .build());
        stockJpaRepository.save(StockEntity.builder()
                .organisationId(organisation.getId())
                .productId(product.getId())
                .quantity(30)
                .build());
        aiForecastRepository.save(AiForecastEntity.builder()
                .organisationId(organisation.getId())
                .productId(product.getId())
                .horizonDays(30)
                .predictedQuantity(BigDecimal.valueOf(5))
                .confidenceScore(BigDecimal.valueOf(0.82))
                .modelName("local-moving-average-v2")
                .generatedAt(LocalDateTime.now())
                .build());

        createSale(user, product.getId(), 3, "STORE");
        createSale(user, product.getId(), 4, "WEB");

        mockMvc.perform(get("/sales/analytics")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seasonality.industry").value("garage"))
                .andExpect(jsonPath("$.seasonality.weekdaySeries", hasSize(7)))
                .andExpect(jsonPath("$.forecastComparisons[0].sku").value("FC-1"))
                .andExpect(jsonPath("$.forecastComparisons[0].actualUnits30Days").value(7))
                .andExpect(jsonPath("$.forecastComparisons[0].status").value("Sous-estime"))
                .andExpect(jsonPath("$.forecastAlerts[0].sku").value("FC-1"));
    }

    private void createSale(User user, Long productId, int quantity, String channel) throws Exception {
        mockMvc.perform(post("/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwtService.generateToken(user))
                        .content("""
                                {
                                  "customerName": "Client",
                                  "channel": "%s",
                                  "lines": [
                                    { "productId": %d, "quantity": %d, "unitPrice": 10.00 }
                                  ]
                                }
                                """.formatted(channel, productId, quantity)))
                .andExpect(status().isOk());
    }
}
