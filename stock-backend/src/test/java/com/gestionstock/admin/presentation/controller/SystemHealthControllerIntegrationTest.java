package com.gestionstock.admin.presentation.controller;

import com.gestionstock.iam.infrastructure.entity.Organisation;
import com.gestionstock.iam.infrastructure.entity.Role;
import com.gestionstock.iam.infrastructure.entity.User;
import com.gestionstock.iam.infrastructure.repository.OrganisationRepository;
import com.gestionstock.iam.infrastructure.repository.UserRepository;
import com.gestionstock.product.infrastructure.entity.ProductEntity;
import com.gestionstock.product.infrastructure.repository.ProductJpaRepository;
import com.gestionstock.security.JwtService;
import com.gestionstock.stock.infrastructure.entity.StockEntity;
import com.gestionstock.stock.infrastructure.repository.StockJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SystemHealthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductJpaRepository productRepository;

    @Autowired
    private StockJpaRepository stockRepository;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        stockRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        organisationRepository.deleteAll();
    }

    @Test
    void adminCanReadTenantSystemHealth() throws Exception {
        Organisation organisation = organisationRepository.save(Organisation.builder().name("Ops Org").build());
        User admin = userRepository.save(User.builder()
                .email("admin@ops-org.test")
                .password("not-used")
                .organisation(organisation)
                .role(Role.ADMIN)
                .build());

        ProductEntity product = productRepository.save(ProductEntity.builder()
                .organisationId(organisation.getId())
                .name("Thermal paper")
                .sku("OPS-001")
                .category("Retail")
                .minStock(5)
                .unit("pcs")
                .build());
        stockRepository.save(StockEntity.builder()
                .organisationId(organisation.getId())
                .productId(product.getId())
                .quantity(12)
                .build());

        mockMvc.perform(get("/admin/system-health")
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallStatus").exists())
                .andExpect(jsonPath("$.services[0].name").value("Database"))
                .andExpect(jsonPath("$.productsCount").value(1))
                .andExpect(jsonPath("$.stocksCount").value(1));
    }

    @Test
    void userCannotReadSystemHealth() throws Exception {
        Organisation organisation = organisationRepository.save(Organisation.builder().name("Ops User Org").build());
        User user = userRepository.save(User.builder()
                .email("user@ops-org.test")
                .password("not-used")
                .organisation(organisation)
                .role(Role.USER)
                .build());

        mockMvc.perform(get("/admin/system-health")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isForbidden());
    }
}
