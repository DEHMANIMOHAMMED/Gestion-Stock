package com.gestionstock.product.presentation.controller;

import com.gestionstock.ai.infrastructure.repository.*;
import com.gestionstock.iam.infrastructure.entity.Organisation;
import com.gestionstock.iam.infrastructure.entity.Role;
import com.gestionstock.iam.infrastructure.entity.User;
import com.gestionstock.iam.infrastructure.repository.OrganisationRepository;
import com.gestionstock.iam.infrastructure.repository.UserRepository;
import com.gestionstock.product.infrastructure.entity.ProductEntity;
import com.gestionstock.product.infrastructure.repository.ProductJpaRepository;
import com.gestionstock.security.JwtService;
import com.gestionstock.stock.infrastructure.repository.StockMovementJpaRepository;
import com.gestionstock.stock.infrastructure.repository.StockJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerIntegrationTest {

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
    private JwtService jwtService;

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

    @BeforeEach
    void setUp() {
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
    void productsEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void productsEndpointOnlyReturnsCurrentTenantProducts() throws Exception {
        Organisation org1 = organisationRepository.save(Organisation.builder().name("Org 1").build());
        Organisation org2 = organisationRepository.save(Organisation.builder().name("Org 2").build());

        User user = userRepository.save(User.builder()
                .email("admin@org1.test")
                .password("not-used")
                .organisation(org1)
                .role(Role.ADMIN)
                .build());

        productJpaRepository.save(ProductEntity.builder()
                .organisationId(org1.getId())
                .name("Org 1 Product")
                .sku("ORG1-SKU")
                .minStock(1)
                .unit("pcs")
                .build());

        productJpaRepository.save(ProductEntity.builder()
                .organisationId(org2.getId())
                .name("Org 2 Product")
                .sku("ORG2-SKU")
                .minStock(1)
                .unit("pcs")
                .build());

        mockMvc.perform(get("/products")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sku").value("ORG1-SKU"));
    }
}
