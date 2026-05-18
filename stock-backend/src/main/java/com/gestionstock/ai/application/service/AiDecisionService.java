package com.gestionstock.ai.application.service;

import com.gestionstock.ai.application.dto.*;
import com.gestionstock.ai.infrastructure.entity.*;
import com.gestionstock.ai.infrastructure.client.AiEngineClient;
import com.gestionstock.ai.infrastructure.repository.*;
import com.gestionstock.product.domain.model.Product;
import com.gestionstock.product.domain.repository.ProductRepository;
import com.gestionstock.procurement.domain.model.ProductSupplier;
import com.gestionstock.procurement.domain.model.Supplier;
import com.gestionstock.procurement.domain.service.ProductSupplierService;
import com.gestionstock.security.TenantContext;
import com.gestionstock.stock.domain.model.MovementType;
import com.gestionstock.stock.domain.model.Stock;
import com.gestionstock.stock.domain.model.StockMovement;
import com.gestionstock.stock.domain.repository.StockMovementRepository;
import com.gestionstock.stock.domain.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiDecisionService {

    private static final List<Integer> FORECAST_HORIZONS = List.of(7, 30, 90);
    private static final int DEFAULT_LEAD_TIME_DAYS = 7;

    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final StockMovementRepository movementRepository;
    private final AiForecastRepository forecastRepository;
    private final AiStockoutRiskRepository stockoutRiskRepository;
    private final AiReorderRecommendationRepository reorderRepository;
    private final AiAnomalyRepository anomalyRepository;
    private final AiInsightRepository insightRepository;
    private final AiRunRepository runRepository;
    private final AiEngineClient aiEngineClient;
    private final ProductSupplierService productSupplierService;

    @Transactional
    @CacheEvict(value = "aiDashboard", key = "#root.target.tenantCacheKey()")
    public AiDashboardResponse regenerateDashboard() {
        Long organisationId = TenantContext.requireOrganisationId();
        regenerate(organisationId);
        return dashboardFromStorage(organisationId);
    }

    @Transactional
    @Cacheable(value = "aiDashboard", key = "#root.target.tenantCacheKey()")
    public AiDashboardResponse getDashboard() {
        Long organisationId = TenantContext.requireOrganisationId();
        ensureInitialSnapshot(organisationId);
        return dashboardFromStorage(organisationId);
    }

    @CacheEvict(value = "aiDashboard", key = "#organisationId")
    public void executeRun(Long runId, Long organisationId) {
        AiRunEntity run = runRepository.findById(runId).orElseThrow();
        run.setStatus("RUNNING");
        run.setStartedAt(LocalDateTime.now());
        runRepository.save(run);

        try {
            GenerationSummary summary = regenerate(organisationId);
            run.setStatus("SUCCEEDED");
            run.setCompletedAt(LocalDateTime.now());
            run.setErrorMessage(null);
            run.setForecastsCount(summary.forecastsCount());
            run.setRisksCount(summary.risksCount());
            run.setRecommendationsCount(summary.recommendationsCount());
            run.setAnomaliesCount(summary.anomaliesCount());
            run.setInsightsCount(summary.insightsCount());
            run.setModelSource(summary.modelSource());
        } catch (RuntimeException exception) {
            run.setStatus("FAILED");
            run.setCompletedAt(LocalDateTime.now());
            run.setErrorMessage(exception.getMessage());
        } finally {
            runRepository.save(run);
        }
    }

    @Transactional
    public List<AiForecastResponse> getForecasts(Integer horizonDays) {
        Long organisationId = TenantContext.requireOrganisationId();
        ensureInitialSnapshot(organisationId);
        List<AiForecastEntity> forecasts = horizonDays == null
                ? forecastRepository.findByOrganisationIdOrderByGeneratedAtDesc(organisationId)
                : forecastRepository.findByOrganisationIdAndHorizonDaysOrderByGeneratedAtDesc(organisationId, horizonDays);
        return toForecastResponses(organisationId, forecasts);
    }

    @Transactional
    public List<AiStockoutRiskResponse> getStockoutRisks() {
        Long organisationId = TenantContext.requireOrganisationId();
        ensureInitialSnapshot(organisationId);
        return toRiskResponses(organisationId, stockoutRiskRepository.findByOrganisationIdOrderByRiskScoreDesc(organisationId));
    }

    @Transactional
    public List<AiReorderRecommendationResponse> getReorderRecommendations() {
        Long organisationId = TenantContext.requireOrganisationId();
        ensureInitialSnapshot(organisationId);
        return toReorderResponses(organisationId, reorderRepository.findByOrganisationIdOrderByRecommendedQuantityDesc(organisationId));
    }

    @Transactional
    public List<AiAnomalyResponse> getAnomalies() {
        Long organisationId = TenantContext.requireOrganisationId();
        ensureInitialSnapshot(organisationId);
        return toAnomalyResponses(organisationId, anomalyRepository.findByOrganisationIdOrderByDetectedAtDesc(organisationId));
    }

    @Transactional
    public List<AiInsightResponse> getInsights() {
        Long organisationId = TenantContext.requireOrganisationId();
        ensureInitialSnapshot(organisationId);
        return toInsightResponses(insightRepository.findByOrganisationIdOrderByGeneratedAtDesc(organisationId));
    }

    public Long tenantCacheKey() {
        return TenantContext.requireOrganisationId();
    }

    private void ensureInitialSnapshot(Long organisationId) {
        if (forecastRepository.findByOrganisationIdOrderByGeneratedAtDesc(organisationId).isEmpty()) {
            regenerate(organisationId);
        }
    }

    private GenerationSummary regenerate(Long organisationId) {
        List<Product> products = productRepository.findAll(organisationId);
        Map<Long, Stock> stockByProductId = stockRepository.findAll(organisationId)
                .stream()
                .collect(Collectors.toMap(Stock::productId, Function.identity()));
        List<StockMovement> movements = movementRepository.findHistory(organisationId, null, null, 0, 500);
        Map<Long, List<StockMovement>> movementsByProduct = movements.stream()
                .collect(Collectors.groupingBy(StockMovement::productId));
        LocalDateTime generatedAt = LocalDateTime.now();

        forecastRepository.deleteByOrganisationId(organisationId);
        stockoutRiskRepository.deleteByOrganisationId(organisationId);
        reorderRepository.deleteByOrganisationId(organisationId);
        anomalyRepository.deleteByOrganisationId(organisationId);
        insightRepository.deleteByOrganisationId(organisationId);

        var externalDecision = aiEngineClient.generate(
                organisationId,
                products,
                stockByProductId.values().stream().toList(),
                movements
        );
        if (externalDecision.isPresent()) {
            return persistExternalDecision(organisationId, externalDecision.get(), generatedAt);
        }

        for (Product product : products) {
            int currentStock = stockByProductId.getOrDefault(product.id(), emptyStock(organisationId, product.id())).quantity();
            double dailyDemand = dailyDemand(movementsByProduct.getOrDefault(product.id(), List.of()));

            for (Integer horizon : FORECAST_HORIZONS) {
                forecastRepository.save(toForecast(organisationId, product.id(), horizon, dailyDemand, generatedAt));
            }

            AiStockoutRiskEntity risk = toStockoutRisk(organisationId, product, currentStock, dailyDemand, generatedAt);
            stockoutRiskRepository.save(risk);

            int recommendedQuantity = recommendedQuantity(product, currentStock, dailyDemand);
            if (recommendedQuantity > 0) {
                reorderRepository.save(toRecommendation(organisationId, product, recommendedQuantity, dailyDemand, currentStock, generatedAt));
            }
        }

        movements.stream()
                .filter(movement -> isAnomalous(movement, movementsByProduct.getOrDefault(movement.productId(), List.of())))
                .limit(50)
                .map(movement -> toAnomaly(organisationId, movement, generatedAt))
                .forEach(anomalyRepository::save);

        saveInsights(organisationId, products, stockByProductId, generatedAt);

        return new GenerationSummary(
                forecastRepository.findByOrganisationIdOrderByGeneratedAtDesc(organisationId).size(),
                stockoutRiskRepository.findByOrganisationIdOrderByRiskScoreDesc(organisationId).size(),
                reorderRepository.findByOrganisationIdOrderByRecommendedQuantityDesc(organisationId).size(),
                anomalyRepository.findByOrganisationIdOrderByDetectedAtDesc(organisationId).size(),
                insightRepository.findByOrganisationIdOrderByGeneratedAtDesc(organisationId).size(),
                "LOCAL_FALLBACK"
        );
    }

    private GenerationSummary persistExternalDecision(
            Long organisationId,
            AiEngineClient.DecisionResponse decision,
            LocalDateTime generatedAt
    ) {
        decision.forecasts().forEach(forecast -> forecastRepository.save(AiForecastEntity.builder()
                .organisationId(organisationId)
                .productId(forecast.productId())
                .horizonDays(forecast.horizonDays())
                .predictedQuantity(forecast.predictedQuantity())
                .confidenceScore(forecast.confidenceScore())
                .modelName(forecast.modelName())
                .generatedAt(generatedAt)
                .build()));

        decision.stockoutRisks().forEach(risk -> stockoutRiskRepository.save(AiStockoutRiskEntity.builder()
                .organisationId(organisationId)
                .productId(risk.productId())
                .estimatedStockoutDate(risk.estimatedStockoutDate())
                .riskScore(risk.riskScore())
                .riskLevel(risk.riskLevel())
                .reason(risk.reason())
                .generatedAt(generatedAt)
                .build()));

        decision.reorderRecommendations().forEach(recommendation -> reorderRepository.save(AiReorderRecommendationEntity.builder()
                .organisationId(organisationId)
                .productId(recommendation.productId())
                .recommendedQuantity(recommendation.recommendedQuantity())
                .leadTimeDays(recommendation.leadTimeDays())
                .safetyStock(recommendation.safetyStock())
                .reason(recommendation.reason())
                .status(recommendation.status() == null ? "PENDING" : recommendation.status())
                .generatedAt(generatedAt)
                .build()));

        decision.anomalies().forEach(anomaly -> anomalyRepository.save(AiAnomalyEntity.builder()
                .organisationId(organisationId)
                .productId(anomaly.productId())
                .stockMovementId(anomaly.stockMovementId())
                .anomalyType(anomaly.anomalyType())
                .severity(anomaly.severity())
                .score(anomaly.score())
                .explanation(anomaly.explanation())
                .detectedAt(generatedAt)
                .build()));

        decision.insights().forEach(insight -> insightRepository.save(AiInsightEntity.builder()
                .organisationId(organisationId)
                .title(insight.title())
                .content(insight.content())
                .insightType(insight.insightType())
                .priority(insight.priority())
                .generatedAt(generatedAt)
                .build()));

        return new GenerationSummary(
                decision.forecasts().size(),
                decision.stockoutRisks().size(),
                decision.reorderRecommendations().size(),
                decision.anomalies().size(),
                decision.insights().size(),
                "FASTAPI"
        );
    }

    private AiDashboardResponse dashboardFromStorage(Long organisationId) {
        return new AiDashboardResponse(
                toForecastResponses(organisationId, forecastRepository.findByOrganisationIdOrderByGeneratedAtDesc(organisationId)),
                toRiskResponses(organisationId, stockoutRiskRepository.findByOrganisationIdOrderByRiskScoreDesc(organisationId)),
                toReorderResponses(organisationId, reorderRepository.findByOrganisationIdOrderByRecommendedQuantityDesc(organisationId)),
                toAnomalyResponses(organisationId, anomalyRepository.findByOrganisationIdOrderByDetectedAtDesc(organisationId)),
                toInsightResponses(insightRepository.findByOrganisationIdOrderByGeneratedAtDesc(organisationId))
        );
    }

    private AiForecastEntity toForecast(Long organisationId, Long productId, int horizonDays, double dailyDemand, LocalDateTime generatedAt) {
        BigDecimal predictedQuantity = BigDecimal.valueOf(dailyDemand * horizonDays).setScale(2, RoundingMode.HALF_UP);
        BigDecimal confidenceScore = BigDecimal.valueOf(dailyDemand == 0 ? 55 : 72).setScale(2, RoundingMode.HALF_UP);
        return AiForecastEntity.builder()
                .organisationId(organisationId)
                .productId(productId)
                .horizonDays(horizonDays)
                .predictedQuantity(predictedQuantity)
                .confidenceScore(confidenceScore)
                .modelName("moving-average-mvp")
                .generatedAt(generatedAt)
                .build();
    }

    private AiStockoutRiskEntity toStockoutRisk(Long organisationId, Product product, int currentStock, double dailyDemand, LocalDateTime generatedAt) {
        int minStock = minStock(product);
        double ratio = minStock == 0 ? 0 : Math.max(0, minStock - currentStock) / (double) minStock;
        BigDecimal riskScore = BigDecimal.valueOf(currentStock == 0 ? 100 : Math.min(100, ratio * 85 + dailyDemand * 5))
                .setScale(2, RoundingMode.HALF_UP);
        String riskLevel = riskScore.doubleValue() >= 70 ? "HIGH" : riskScore.doubleValue() >= 35 ? "MEDIUM" : "LOW";
        LocalDate stockoutDate = dailyDemand <= 0 ? null : LocalDate.now().plusDays(Math.max(0, (long) Math.floor(currentStock / dailyDemand)));

        return AiStockoutRiskEntity.builder()
                .organisationId(organisationId)
                .productId(product.id())
                .estimatedStockoutDate(stockoutDate)
                .riskScore(riskScore)
                .riskLevel(riskLevel)
                .reason("Stock actuel " + currentStock + ", seuil " + minStock + ", demande moyenne " + round(dailyDemand) + " unite/jour.")
                .generatedAt(generatedAt)
                .build();
    }

    private AiReorderRecommendationEntity toRecommendation(Long organisationId, Product product, int quantity, double dailyDemand, int currentStock, LocalDateTime generatedAt) {
        int safetyStock = Math.max(minStock(product), (int) Math.ceil(dailyDemand * 3));
        return AiReorderRecommendationEntity.builder()
                .organisationId(organisationId)
                .productId(product.id())
                .recommendedQuantity(quantity)
                .leadTimeDays(DEFAULT_LEAD_TIME_DAYS)
                .safetyStock(safetyStock)
                .reason("Commande basee sur lead time " + DEFAULT_LEAD_TIME_DAYS + " jours, stock " + currentStock + " et stock de securite " + safetyStock + ".")
                .status("PENDING")
                .generatedAt(generatedAt)
                .build();
    }

    private AiAnomalyEntity toAnomaly(Long organisationId, StockMovement movement, LocalDateTime detectedAt) {
        return AiAnomalyEntity.builder()
                .organisationId(organisationId)
                .productId(movement.productId())
                .stockMovementId(movement.id())
                .anomalyType("UNUSUAL_MOVEMENT")
                .severity(movement.quantity() >= 50 ? "HIGH" : "MEDIUM")
                .score(BigDecimal.valueOf(Math.min(100, movement.quantity() * 2L)).setScale(2, RoundingMode.HALF_UP))
                .explanation("Mouvement " + movement.type().name() + " de " + movement.quantity() + " unites au-dessus du comportement recent.")
                .detectedAt(detectedAt)
                .build();
    }

    private void saveInsights(Long organisationId, List<Product> products, Map<Long, Stock> stockByProductId, LocalDateTime generatedAt) {
        long outOfStock = products.stream().filter(product -> quantity(product, stockByProductId) == 0).count();
        long lowStock = products.stream().filter(product -> quantity(product, stockByProductId) <= minStock(product)).count();

        insightRepository.save(AiInsightEntity.builder()
                .organisationId(organisationId)
                .title("Priorite stock")
                .content(lowStock + " produit(s) demandent une action. " + outOfStock + " sont en rupture complete.")
                .insightType("STOCK_HEALTH")
                .priority(outOfStock > 0 ? "HIGH" : lowStock > 0 ? "MEDIUM" : "LOW")
                .generatedAt(generatedAt)
                .build());
    }

    private int recommendedQuantity(Product product, int currentStock, double dailyDemand) {
        int safetyStock = Math.max(minStock(product), (int) Math.ceil(dailyDemand * 3));
        int leadTimeDemand = (int) Math.ceil(dailyDemand * DEFAULT_LEAD_TIME_DAYS);
        return Math.max(0, leadTimeDemand + safetyStock - currentStock);
    }

    private boolean isAnomalous(StockMovement movement, List<StockMovement> productMovements) {
        if (movement.type() == MovementType.ADJUST && movement.quantity() >= 10) {
            return true;
        }
        double average = productMovements.stream().mapToInt(StockMovement::quantity).average().orElse(0);
        return average > 0 && movement.quantity() >= Math.max(25, average * 3);
    }

    private double dailyDemand(List<StockMovement> movements) {
        int outQuantity = movements.stream()
                .filter(movement -> movement.type() == MovementType.OUT)
                .mapToInt(StockMovement::quantity)
                .sum();
        long activeDays = Math.max(7, movements.stream()
                .map(StockMovement::createdAt)
                .min(Comparator.naturalOrder())
                .map(first -> Math.max(1, java.time.Duration.between(first, LocalDateTime.now()).toDays()))
                .orElse(7L));
        return outQuantity / (double) activeDays;
    }

    private List<AiForecastResponse> toForecastResponses(Long organisationId, List<AiForecastEntity> forecasts) {
        Map<Long, Product> products = productMap(organisationId);
        return forecasts.stream()
                .map(entity -> {
                    Product product = products.get(entity.getProductId());
                    return new AiForecastResponse(
                            entity.getId(),
                            entity.getProductId(),
                            product == null ? "Produit supprime" : product.name(),
                            product == null ? "-" : product.sku(),
                            entity.getHorizonDays(),
                            entity.getPredictedQuantity(),
                            entity.getConfidenceScore(),
                            entity.getModelName(),
                            entity.getGeneratedAt()
                    );
                })
                .toList();
    }

    private List<AiStockoutRiskResponse> toRiskResponses(Long organisationId, List<AiStockoutRiskEntity> risks) {
        Map<Long, Product> products = productMap(organisationId);
        return risks.stream()
                .map(entity -> {
                    Product product = products.get(entity.getProductId());
                    return new AiStockoutRiskResponse(
                            entity.getId(),
                            entity.getProductId(),
                            product == null ? "Produit supprime" : product.name(),
                            product == null ? "-" : product.sku(),
                            entity.getEstimatedStockoutDate(),
                            entity.getRiskScore(),
                            entity.getRiskLevel(),
                            entity.getReason(),
                            entity.getGeneratedAt()
                    );
                })
                .toList();
    }

    private List<AiReorderRecommendationResponse> toReorderResponses(Long organisationId, List<AiReorderRecommendationEntity> recommendations) {
        Map<Long, Product> products = productMap(organisationId);
        return recommendations.stream()
                .map(entity -> {
                    Product product = products.get(entity.getProductId());
                    ProductSupplierService.SupplierScore supplierScore = productSupplierService.findBestScoreForProduct(entity.getProductId(), organisationId)
                            .orElse(null);
                    ProductSupplier productSupplier = supplierScore == null ? null : supplierScore.productSupplier();
                    Supplier supplier = supplierScore == null ? null : supplierScore.supplier();
                    return new AiReorderRecommendationResponse(
                            entity.getId(),
                            entity.getProductId(),
                            product == null ? "Produit supprime" : product.name(),
                            product == null ? "-" : product.sku(),
                            entity.getRecommendedQuantity(),
                            entity.getLeadTimeDays(),
                            entity.getSafetyStock(),
                            entity.getReason(),
                            entity.getStatus(),
                            entity.getPurchaseOrderId(),
                            productSupplier == null ? null : productSupplier.supplierId(),
                            supplier == null ? null : supplier.name(),
                            supplier == null ? null : supplier.leadTimeDays(),
                            productSupplier == null ? null : productSupplier.unitCost(),
                            productSupplier == null ? null : productSupplier.minimumOrderQuantity(),
                            supplierScore == null ? null : supplierScore.score(),
                            supplierScore == null ? null : supplierScore.onTimeRate(),
                            supplierScore == null ? null : supplierScore.conformityRate(),
                            supplierScore == null ? null : supplierScore.explanation(),
                            entity.getGeneratedAt()
                    );
                })
                .toList();
    }

    private List<AiAnomalyResponse> toAnomalyResponses(Long organisationId, List<AiAnomalyEntity> anomalies) {
        Map<Long, Product> products = productMap(organisationId);
        return anomalies.stream()
                .map(entity -> {
                    Product product = entity.getProductId() == null ? null : products.get(entity.getProductId());
                    return new AiAnomalyResponse(
                            entity.getId(),
                            entity.getProductId(),
                            product == null ? null : product.name(),
                            entity.getStockMovementId(),
                            entity.getAnomalyType(),
                            entity.getSeverity(),
                            entity.getScore(),
                            entity.getExplanation(),
                            entity.getDetectedAt()
                    );
                })
                .toList();
    }

    private List<AiInsightResponse> toInsightResponses(List<AiInsightEntity> insights) {
        return insights.stream()
                .map(entity -> new AiInsightResponse(
                        entity.getId(),
                        entity.getTitle(),
                        entity.getContent(),
                        entity.getInsightType(),
                        entity.getPriority(),
                        entity.getGeneratedAt()
                ))
                .toList();
    }

    private Map<Long, Product> productMap(Long organisationId) {
        return productRepository.findAll(organisationId).stream()
                .collect(Collectors.toMap(Product::id, Function.identity()));
    }

    private int quantity(Product product, Map<Long, Stock> stockByProductId) {
        return stockByProductId.getOrDefault(product.id(), emptyStock(product.organisationId(), product.id())).quantity();
    }

    private Stock emptyStock(Long organisationId, Long productId) {
        return Stock.builder().organisationId(organisationId).productId(productId).quantity(0).build();
    }

    private int minStock(Product product) {
        return product.minStock() == null ? 0 : product.minStock();
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private record GenerationSummary(
            int forecastsCount,
            int risksCount,
            int recommendationsCount,
            int anomaliesCount,
            int insightsCount,
            String modelSource
    ) {
    }
}
