package com.gestionstock.admin.presentation.controller;

import com.gestionstock.ai.infrastructure.entity.AiForecastEntity;
import com.gestionstock.ai.infrastructure.repository.AiForecastRepository;
import com.gestionstock.iam.infrastructure.entity.Organisation;
import com.gestionstock.iam.infrastructure.entity.Role;
import com.gestionstock.iam.infrastructure.entity.User;
import com.gestionstock.iam.infrastructure.repository.OrganisationRepository;
import com.gestionstock.iam.infrastructure.repository.UserRepository;
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
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ModelRegistryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganisationRepository organisationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductJpaRepository productRepository;

    @Autowired
    private AiForecastRepository forecastRepository;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        forecastRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        organisationRepository.deleteAll();
    }

    @Test
    void registryAggregatesModelsForAdminTenant() throws Exception {
        Organisation organisation = organisationRepository.save(Organisation.builder().name("Registry Org").build());
        User admin = userRepository.save(User.builder()
                .email("admin@registry.test")
                .password("not-used")
                .organisation(organisation)
                .role(Role.ADMIN)
                .build());
        ProductEntity oil = productRepository.save(ProductEntity.builder()
                .organisationId(organisation.getId())
                .name("Huile")
                .sku("MR-1")
                .minStock(5)
                .unit("pcs")
                .build());
        ProductEntity battery = productRepository.save(ProductEntity.builder()
                .organisationId(organisation.getId())
                .name("Batterie")
                .sku("MR-2")
                .minStock(3)
                .unit("pcs")
                .build());

        saveForecast(organisation.getId(), oil.getId(), "moving-average-v2", 12, 18, null);
        saveForecast(organisation.getId(), battery.getId(), "seasonality-weekday-v1", 70, 68, null);

        mockMvc.perform(get("/admin/model-registry")
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalForecasts").value(2))
                .andExpect(jsonPath("$.models", hasSize(2)))
                .andExpect(jsonPath("$.productsToRecalibrate").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.models[0].winningProducts").exists())
                .andExpect(jsonPath("$.models[?(@.modelName=='moving-average-v2')]").exists());
    }

    @Test
    void registryIsAdminOnly() throws Exception {
        Organisation organisation = organisationRepository.save(Organisation.builder().name("Registry User Org").build());
        User user = userRepository.save(User.builder()
                .email("user@registry.test")
                .password("not-used")
                .organisation(organisation)
                .role(Role.USER)
                .build());

        mockMvc.perform(get("/admin/model-registry")
                        .header("Authorization", "Bearer " + jwtService.generateToken(user)))
                .andExpect(status().isForbidden());
    }

    private void saveForecast(
            Long organisationId,
            Long productId,
            String selectedModel,
            int movingAverageError,
            int seasonalError,
            BigDecimal fastapiError
    ) {
        forecastRepository.save(AiForecastEntity.builder()
                .organisationId(organisationId)
                .productId(productId)
                .horizonDays(30)
                .predictedQuantity(BigDecimal.valueOf(42))
                .confidenceScore(BigDecimal.valueOf(80))
                .modelName(selectedModel)
                .selectedModel(selectedModel)
                .modelSelectionReason("Test model selection")
                .movingAverageError(BigDecimal.valueOf(movingAverageError))
                .seasonalError(BigDecimal.valueOf(seasonalError))
                .fastapiError(fastapiError)
                .generatedAt(LocalDateTime.now())
                .build());
    }
}
