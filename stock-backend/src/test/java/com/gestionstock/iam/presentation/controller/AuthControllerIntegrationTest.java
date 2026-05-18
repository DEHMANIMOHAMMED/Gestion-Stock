package com.gestionstock.iam.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gestionstock.ai.infrastructure.repository.*;
import com.gestionstock.iam.infrastructure.repository.OrganisationRepository;
import com.gestionstock.iam.infrastructure.repository.UserRepository;
import com.gestionstock.product.infrastructure.repository.ProductJpaRepository;
import com.gestionstock.stock.infrastructure.repository.StockJpaRepository;
import com.gestionstock.stock.infrastructure.repository.StockMovementJpaRepository;
import com.gestionstock.iam.presentation.dto.LoginRequest;
import com.gestionstock.iam.presentation.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganisationRepository organisationRepository;

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
    void registeredUserCanLoginWithSamePassword() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "Login Org",
                "admin@login-org.test",
                "Password123!"
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString());

        LoginRequest loginRequest = new LoginRequest(
                "admin@login-org.test",
                "Password123!"
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void duplicateRegistrationReturnsProblemDetail() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "Duplicate Org",
                "admin@duplicate.test",
                "Password123!"
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Email already in use"));
    }

    @Test
    void meReturnsCurrentAuthenticatedUser() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "Me Org",
                "admin@me-org.test",
                "Password123!"
        );

        String response = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(response).get("token").asText();

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@me-org.test"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.organisationName").value("Me Org"))
                .andExpect(jsonPath("$.onboardingCompleted").value(false));
    }

    @Test
    void authenticatedUserCanCompleteOrganisationProfile() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "Profile Org",
                "admin@profile-org.test",
                "Password123!"
        );

        String response = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = objectMapper.readTree(response).get("token").asText();

        mockMvc.perform(put("/auth/organisation-profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Profile Org",
                                  "industry": "Pharmacie",
                                  "sizeRange": "2-10",
                                  "phone": "+33123456789",
                                  "address": "12 Rue Demo",
                                  "city": "Paris",
                                  "country": "France",
                                  "currency": "EUR"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.industry").value("Pharmacie"))
                .andExpect(jsonPath("$.city").value("Paris"))
                .andExpect(jsonPath("$.onboardingCompleted").value(true));

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onboardingCompleted").value(true));
    }
}
