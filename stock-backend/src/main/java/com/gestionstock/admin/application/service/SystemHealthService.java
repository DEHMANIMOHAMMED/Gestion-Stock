package com.gestionstock.admin.application.service;

import com.gestionstock.admin.application.dto.SystemHealthResponse;
import com.gestionstock.admin.application.dto.SystemHealthServiceResponse;
import com.gestionstock.ai.application.dto.AiRunResponse;
import com.gestionstock.ai.application.service.AiRunService;
import com.gestionstock.iam.infrastructure.entity.Role;
import com.gestionstock.iam.infrastructure.entity.User;
import com.gestionstock.notification.infrastructure.repository.AdminNotificationJpaRepository;
import com.gestionstock.procurement.infrastructure.repository.PurchaseOrderJpaRepository;
import com.gestionstock.product.infrastructure.repository.ProductJpaRepository;
import com.gestionstock.security.AuthenticatedUserProvider;
import com.gestionstock.security.TenantContext;
import com.gestionstock.stock.infrastructure.repository.StockJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemHealthService {

    private final DataSource dataSource;
    private final AiRunService aiRunService;
    private final ProductJpaRepository productRepository;
    private final StockJpaRepository stockRepository;
    private final PurchaseOrderJpaRepository purchaseOrderRepository;
    private final AdminNotificationJpaRepository notificationRepository;
    private final AuthenticatedUserProvider userProvider;

    @Value("${ai.service.url:http://127.0.0.1:8000}")
    private String aiServiceUrl;

    public SystemHealthResponse health() {
        User user = userProvider.requireUser();
        if (user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only admins can access system health");
        }

        Long organisationId = TenantContext.requireOrganisationId();
        List<SystemHealthServiceResponse> services = new ArrayList<>();
        services.add(databaseHealth());
        services.add(aiServiceHealth());

        String overallStatus = services.stream().allMatch(service -> "UP".equals(service.status())) ? "UP" : "DEGRADED";
        AiRunResponse latestAiRun = aiRunService.latestRun();

        return new SystemHealthResponse(
                overallStatus,
                LocalDateTime.now(),
                services,
                latestAiRun,
                notificationRepository.countByOrganisationIdAndReadAtIsNull(organisationId),
                productRepository.findByOrganisationId(organisationId).size(),
                stockRepository.findByOrganisationId(organisationId).size(),
                purchaseOrderRepository.findByOrganisationIdOrderByCreatedAtDesc(organisationId).size()
        );
    }

    private SystemHealthServiceResponse databaseHealth() {
        Instant start = Instant.now();
        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(2);
            return new SystemHealthServiceResponse(
                    "Database",
                    valid ? "UP" : "DOWN",
                    elapsed(start),
                    valid ? "Connection valide" : "Connection refusee"
            );
        } catch (Exception exception) {
            return new SystemHealthServiceResponse("Database", "DOWN", elapsed(start), exception.getMessage());
        }
    }

    private SystemHealthServiceResponse aiServiceHealth() {
        Instant start = Instant.now();
        try {
            String response = RestClient.builder()
                    .baseUrl(aiServiceUrl)
                    .build()
                    .get()
                    .uri("/health")
                    .retrieve()
                    .body(String.class);
            return new SystemHealthServiceResponse("AI service", "UP", elapsed(start), response == null ? "OK" : response);
        } catch (RestClientException exception) {
            return new SystemHealthServiceResponse("AI service", "DOWN", elapsed(start), exception.getMessage());
        }
    }

    private long elapsed(Instant start) {
        return Duration.between(start, Instant.now()).toMillis();
    }
}
