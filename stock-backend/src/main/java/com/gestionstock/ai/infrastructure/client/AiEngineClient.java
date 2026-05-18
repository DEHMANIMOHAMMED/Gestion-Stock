package com.gestionstock.ai.infrastructure.client;

import com.gestionstock.product.domain.model.Product;
import com.gestionstock.stock.domain.model.Stock;
import com.gestionstock.stock.domain.model.StockMovement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class AiEngineClient {

    private final boolean enabled;
    private final RestClient restClient;

    public AiEngineClient(
            @Value("${ai.service.enabled:false}") boolean enabled,
            @Value("${ai.service.url:http://127.0.0.1:8000}") String serviceUrl
    ) {
        this.enabled = enabled;
        this.restClient = RestClient.builder()
                .baseUrl(serviceUrl)
                .build();
    }

    public Optional<DecisionResponse> generate(
            Long organisationId,
            List<Product> products,
            List<Stock> stocks,
            List<StockMovement> movements
    ) {
        if (!enabled) {
            return Optional.empty();
        }

        DecisionRequest request = new DecisionRequest(
                organisationId,
                products.stream().map(ProductSnapshot::from).toList(),
                stocks.stream().map(StockSnapshot::from).toList(),
                movements.stream().map(MovementSnapshot::from).toList(),
                List.of(7, 30, 90),
                7
        );

        try {
            DecisionResponse response = restClient.post()
                    .uri("/internal/decisions/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(DecisionResponse.class);
            return Optional.ofNullable(response);
        } catch (RestClientException exception) {
            return Optional.empty();
        }
    }

    public Optional<CopilotResponse> answerCopilot(CopilotRequest request) {
        if (!enabled) {
            return Optional.empty();
        }
        try {
            CopilotResponse response = restClient.post()
                    .uri("/internal/copilot/answer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(CopilotResponse.class);
            return Optional.ofNullable(response);
        } catch (RestClientException exception) {
            return Optional.empty();
        }
    }

    public Optional<RecommendationExplanationResponse> explainRecommendation(RecommendationExplanationRequest request) {
        if (!enabled) {
            return Optional.empty();
        }
        try {
            RecommendationExplanationResponse response = restClient.post()
                    .uri("/internal/recommendations/explain")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(RecommendationExplanationResponse.class);
            return Optional.ofNullable(response);
        } catch (RestClientException exception) {
            return Optional.empty();
        }
    }

    public record DecisionRequest(
            Long organisationId,
            List<ProductSnapshot> products,
            List<StockSnapshot> stocks,
            List<MovementSnapshot> movements,
            List<Integer> horizons,
            Integer leadTimeDays
    ) {
    }

    public record ProductSnapshot(
            Long id,
            String name,
            String sku,
            String category,
            Integer minStock,
            String unit
    ) {
        private static ProductSnapshot from(Product product) {
            return new ProductSnapshot(
                    product.id(),
                    product.name(),
                    product.sku(),
                    product.category(),
                    product.minStock(),
                    product.unit()
            );
        }
    }

    public record StockSnapshot(
            Long productId,
            Integer quantity
    ) {
        private static StockSnapshot from(Stock stock) {
            return new StockSnapshot(stock.productId(), stock.quantity());
        }
    }

    public record MovementSnapshot(
            Long id,
            Long productId,
            Integer quantity,
            String type,
            LocalDateTime createdAt
    ) {
        private static MovementSnapshot from(StockMovement movement) {
            return new MovementSnapshot(
                    movement.id(),
                    movement.productId(),
                    movement.quantity(),
                    movement.type().name(),
                    movement.createdAt()
            );
        }
    }

    public record DecisionResponse(
            List<ForecastDecision> forecasts,
            List<StockoutRiskDecision> stockoutRisks,
            List<ReorderDecision> reorderRecommendations,
            List<AnomalyDecision> anomalies,
            List<InsightDecision> insights
    ) {
    }

    public record ForecastDecision(
            Long productId,
            Integer horizonDays,
            BigDecimal predictedQuantity,
            BigDecimal confidenceScore,
            String modelName
    ) {
    }

    public record StockoutRiskDecision(
            Long productId,
            LocalDate estimatedStockoutDate,
            BigDecimal riskScore,
            String riskLevel,
            String reason
    ) {
    }

    public record ReorderDecision(
            Long productId,
            Integer recommendedQuantity,
            Integer leadTimeDays,
            Integer safetyStock,
            String reason,
            String status
    ) {
    }

    public record AnomalyDecision(
            Long productId,
            Long stockMovementId,
            String anomalyType,
            String severity,
            BigDecimal score,
            String explanation
    ) {
    }

    public record InsightDecision(
            String title,
            String content,
            String insightType,
            String priority
    ) {
    }

    public record CopilotRequest(
            Long organisationId,
            String question,
            List<CopilotContextItem> contextItems
    ) {
    }

    public record CopilotContextItem(
            String type,
            String title,
            String content,
            Long productId,
            Long supplierId,
            Long purchaseOrderId
    ) {
    }

    public record CopilotResponse(
            String answer,
            List<String> bullets,
            List<Long> relatedProductIds,
            List<CopilotCitation> citations,
            String source
    ) {
    }

    public record CopilotCitation(
            String type,
            String label,
            Long productId,
            Long supplierId,
            Long purchaseOrderId
    ) {
    }

    public record RecommendationExplanationRequest(
            Long organisationId,
            Long recommendationId,
            String productName,
            String sku,
            Integer recommendedQuantity,
            Integer currentStock,
            Integer minStock,
            BigDecimal averageDailyDemand,
            BigDecimal stockCoverageDays,
            BigDecimal riskScore,
            String riskLevel,
            String supplierName,
            BigDecimal supplierScore,
            String supplierExplanation,
            String reason
    ) {
    }

    public record RecommendationExplanationResponse(
            Long recommendationId,
            String summary,
            List<String> drivers,
            List<String> risks,
            String nextAction,
            String source
    ) {
    }
}
