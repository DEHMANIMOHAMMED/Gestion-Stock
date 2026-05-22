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
import com.gestionstock.sales.domain.model.SalesDemand;
import com.gestionstock.sales.domain.repository.SalesRepository;
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
    private final AiForecastSnapshotRepository forecastSnapshotRepository;
    private final AiStockoutRiskRepository stockoutRiskRepository;
    private final AiReorderRecommendationRepository reorderRepository;
    private final AiAnomalyRepository anomalyRepository;
    private final AiInsightRepository insightRepository;
    private final AiRunRepository runRepository;
    private final AiEngineClient aiEngineClient;
    private final ProductSupplierService productSupplierService;
    private final SalesRepository salesRepository;

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
    public List<AiForecastBacktestResponse> getForecastBacktests(Long productId, Integer horizonDays) {
        Long organisationId = TenantContext.requireOrganisationId();
        ensureInitialSnapshot(organisationId);
        int horizon = horizonDays == null ? 30 : horizonDays;
        Map<Long, Product> products = productMap(organisationId);
        Map<Long, AiForecastEntity> latestForecasts = forecastRepository
                .findByOrganisationIdAndHorizonDaysOrderByGeneratedAtDesc(organisationId, horizon)
                .stream()
                .collect(Collectors.toMap(AiForecastEntity::getProductId, Function.identity(), (first, ignored) -> first));
        List<SalesDemand> demand = salesRepository.findDemandSince(organisationId, LocalDateTime.now().minusDays(horizon));

        return latestForecasts.values().stream()
                .filter(forecast -> productId == null || forecast.getProductId().equals(productId))
                .map(forecast -> toBacktestResponse(forecast, products.get(forecast.getProductId()), demand))
                .sorted(Comparator.comparing(AiForecastBacktestResponse::reliabilityScore).reversed())
                .toList();
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
        Map<Long, List<SalesDemand>> salesByProduct = salesRepository.findDemandSince(organisationId, LocalDateTime.now().minusDays(90))
                .stream()
                .collect(Collectors.groupingBy(SalesDemand::productId));
        Map<Long, Double> salesDemandByProduct = salesDemandByProduct(organisationId);
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
            double dailyDemand = salesDemandByProduct.getOrDefault(
                    product.id(),
                    dailyDemand(movementsByProduct.getOrDefault(product.id(), List.of()))
            );

            for (Integer horizon : FORECAST_HORIZONS) {
                saveForecast(toForecast(
                        organisationId,
                        product.id(),
                        horizon,
                        dailyDemand,
                        selectBestModel(product.id(), horizon, salesByProduct.getOrDefault(product.id(), List.of()), null),
                        generatedAt
                ));
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
        Map<Long, List<SalesDemand>> salesByProduct = salesRepository.findDemandSince(organisationId, LocalDateTime.now().minusDays(90))
                .stream()
                .collect(Collectors.groupingBy(SalesDemand::productId));
        decision.forecasts().forEach(forecast -> {
            DemandModelSelection selection = selectBestModel(
                    forecast.productId(),
                    forecast.horizonDays(),
                    salesByProduct.getOrDefault(forecast.productId(), List.of()),
                    forecast.predictedQuantity()
            );
            saveForecast(AiForecastEntity.builder()
                    .organisationId(organisationId)
                    .productId(forecast.productId())
                    .horizonDays(forecast.horizonDays())
                    .predictedQuantity(selection.predictedQuantity())
                    .confidenceScore(forecast.confidenceScore())
                    .modelName(forecast.modelName())
                    .selectedModel(selection.selectedModel())
                    .modelSelectionReason(selection.reason())
                    .movingAverageError(selection.movingAverageError())
                    .seasonalError(selection.seasonalError())
                    .fastapiError(selection.fastApiError())
                    .generatedAt(generatedAt)
                    .build());
        });

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

    private DemandModelSelection selectBestModel(
            Long productId,
            int horizonDays,
            List<SalesDemand> demand,
            BigDecimal fastApiPrediction
    ) {
        int last30 = quantityBetween(demand, LocalDate.now().minusDays(29), LocalDate.now());
        int previous30 = quantityBetween(demand, LocalDate.now().minusDays(59), LocalDate.now().minusDays(30));
        int sameWeekdayVolume = sameWeekdayVolume(demand, horizonDays);

        BigDecimal movingAveragePrediction = BigDecimal.valueOf((last30 / 30.0) * horizonDays).setScale(2, RoundingMode.HALF_UP);
        BigDecimal seasonalPrediction = BigDecimal.valueOf((sameWeekdayVolume / 4.0) * Math.max(1, horizonDays / 7.0)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal actualBaseline = BigDecimal.valueOf(Math.max(1, last30));
        BigDecimal movingAverageError = modelError(actualBaseline, BigDecimal.valueOf(previous30 == 0 ? last30 : previous30));
        BigDecimal seasonalError = modelError(actualBaseline, BigDecimal.valueOf(sameWeekdayVolume == 0 ? last30 : sameWeekdayVolume));
        BigDecimal fastApiError = fastApiPrediction == null ? null : modelError(actualBaseline, fastApiPrediction);

        String selectedModel = "moving-average-v2";
        BigDecimal selectedPrediction = movingAveragePrediction;
        BigDecimal bestError = movingAverageError;
        String reason = "Le moving average gagne avec l'erreur historique la plus faible.";

        if (seasonalError.compareTo(bestError) < 0 && sameWeekdayVolume > 0) {
            selectedModel = "seasonality-weekday-v1";
            selectedPrediction = seasonalPrediction;
            bestError = seasonalError;
            reason = "La saisonnalite hebdomadaire explique mieux les ventes recentes.";
        }
        if (fastApiError != null && fastApiError.compareTo(bestError) < 0) {
            selectedModel = "fastapi-model";
            selectedPrediction = fastApiPrediction.setScale(2, RoundingMode.HALF_UP);
            reason = "Le modele FastAPI presente le meilleur score sur l'historique recent.";
        }
        if (last30 < 5) {
            reason = "Historique ventes faible: selection prudente, a recalibrer avec plus de donnees.";
        }

        return new DemandModelSelection(
                selectedModel,
                selectedPrediction.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP),
                reason,
                movingAverageError,
                seasonalError,
                fastApiError
        );
    }

    private int sameWeekdayVolume(List<SalesDemand> demand, int horizonDays) {
        LocalDate from = LocalDate.now().minusDays(27);
        List<java.time.DayOfWeek> futureDays = java.util.stream.IntStream.rangeClosed(1, Math.max(1, Math.min(horizonDays, 30)))
                .mapToObj(offset -> LocalDate.now().plusDays(offset).getDayOfWeek())
                .distinct()
                .toList();
        return demand.stream()
                .filter(item -> !item.soldAt().toLocalDate().isBefore(from))
                .filter(item -> futureDays.contains(item.soldAt().toLocalDate().getDayOfWeek()))
                .mapToInt(SalesDemand::quantity)
                .sum();
    }

    private BigDecimal modelError(BigDecimal actual, BigDecimal predicted) {
        BigDecimal denominator = actual.compareTo(BigDecimal.ONE) < 0 ? BigDecimal.ONE : actual;
        return actual.subtract(predicted == null ? BigDecimal.ZERO : predicted)
                .abs()
                .multiply(BigDecimal.valueOf(100))
                .divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private AiForecastEntity toForecast(
            Long organisationId,
            Long productId,
            int horizonDays,
            double dailyDemand,
            DemandModelSelection selection,
            LocalDateTime generatedAt
    ) {
        BigDecimal predictedQuantity = selection == null
                ? BigDecimal.valueOf(dailyDemand * horizonDays).setScale(2, RoundingMode.HALF_UP)
                : selection.predictedQuantity();
        BigDecimal confidenceScore = BigDecimal.valueOf(dailyDemand == 0 ? 55 : 72).setScale(2, RoundingMode.HALF_UP);
        return AiForecastEntity.builder()
                .organisationId(organisationId)
                .productId(productId)
                .horizonDays(horizonDays)
                .predictedQuantity(predictedQuantity)
                .confidenceScore(confidenceScore)
                .modelName(selection == null ? "moving-average-mvp" : selection.selectedModel())
                .selectedModel(selection == null ? "moving-average-mvp" : selection.selectedModel())
                .modelSelectionReason(selection == null ? "Selection par defaut faute d'historique." : selection.reason())
                .movingAverageError(selection == null ? null : selection.movingAverageError())
                .seasonalError(selection == null ? null : selection.seasonalError())
                .fastapiError(selection == null ? null : selection.fastApiError())
                .generatedAt(generatedAt)
                .build();
    }

    private AiForecastEntity saveForecast(AiForecastEntity forecast) {
        AiForecastEntity saved = forecastRepository.save(forecast);
        forecastSnapshotRepository.save(AiForecastSnapshotEntity.builder()
                .organisationId(saved.getOrganisationId())
                .productId(saved.getProductId())
                .horizonDays(saved.getHorizonDays())
                .targetDate(saved.getGeneratedAt().toLocalDate().plusDays(saved.getHorizonDays()))
                .predictedQuantity(saved.getPredictedQuantity())
                .confidenceScore(saved.getConfidenceScore())
                .modelName(saved.getModelName())
                .selectedModel(saved.getSelectedModel())
                .modelSelectionReason(saved.getModelSelectionReason())
                .movingAverageError(saved.getMovingAverageError())
                .seasonalError(saved.getSeasonalError())
                .fastapiError(saved.getFastapiError())
                .generatedAt(saved.getGeneratedAt())
                .build());
        return saved;
    }

    private AiForecastBacktestResponse toBacktestResponse(
            AiForecastEntity forecast,
            Product product,
            List<SalesDemand> demand
    ) {
        LocalDate from = LocalDate.now().minusDays(forecast.getHorizonDays() - 1L);
        BigDecimal predictedDaily = forecast.getPredictedQuantity()
                .divide(BigDecimal.valueOf(forecast.getHorizonDays()), 2, RoundingMode.HALF_UP);
        List<AiForecastBacktestPointResponse> points = java.util.stream.IntStream.range(0, forecast.getHorizonDays())
                .mapToObj(offset -> {
                    LocalDate date = from.plusDays(offset);
                    int actual = demand.stream()
                            .filter(item -> item.productId().equals(forecast.getProductId()))
                            .filter(item -> item.soldAt().toLocalDate().isEqual(date))
                            .mapToInt(SalesDemand::quantity)
                            .sum();
                    BigDecimal variance = forecastVariance(BigDecimal.valueOf(actual), predictedDaily);
                    return new AiForecastBacktestPointResponse(date, actual, predictedDaily, variance);
                })
                .toList();
        BigDecimal mae = meanAbsoluteError(points);
        BigDecimal mape = meanAbsolutePercentError(points);
        BigDecimal reliability = BigDecimal.valueOf(Math.max(0, 100 - mape.doubleValue()))
                .setScale(2, RoundingMode.HALF_UP);
        return new AiForecastBacktestResponse(
                forecast.getProductId(),
                product == null ? "Produit supprime" : product.name(),
                product == null ? "-" : product.sku(),
                forecast.getHorizonDays(),
                mae,
                mape,
                reliability,
                forecastQualityLevel(reliability, points),
                forecastBacktestRecommendation(reliability, mape, points),
                points
        );
    }

    private BigDecimal forecastVariance(BigDecimal actual, BigDecimal predicted) {
        if (predicted.compareTo(BigDecimal.ZERO) == 0) {
            return actual.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(100);
        }
        return actual.subtract(predicted)
                .multiply(BigDecimal.valueOf(100))
                .divide(predicted, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal meanAbsoluteError(List<AiForecastBacktestPointResponse> points) {
        if (points.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = points.stream()
                .map(point -> BigDecimal.valueOf(point.actualUnits()).subtract(point.predictedUnits()).abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(points.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal meanAbsolutePercentError(List<AiForecastBacktestPointResponse> points) {
        List<BigDecimal> errors = points.stream()
                .filter(point -> point.actualUnits() > 0 || point.predictedUnits().compareTo(BigDecimal.ZERO) > 0)
                .map(point -> {
                    BigDecimal denominator = BigDecimal.valueOf(Math.max(1, point.actualUnits()));
                    return BigDecimal.valueOf(point.actualUnits())
                            .subtract(point.predictedUnits())
                            .abs()
                            .multiply(BigDecimal.valueOf(100))
                            .divide(denominator, 2, RoundingMode.HALF_UP);
                })
                .toList();
        if (errors.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return errors.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(errors.size()), 2, RoundingMode.HALF_UP);
    }

    private String forecastQualityLevel(BigDecimal reliability, List<AiForecastBacktestPointResponse> points) {
        int actualVolume = points.stream().mapToInt(AiForecastBacktestPointResponse::actualUnits).sum();
        if (actualVolume < 5) {
            return "INSUFFICIENT_DATA";
        }
        if (reliability.compareTo(BigDecimal.valueOf(75)) >= 0) {
            return "HIGH";
        }
        if (reliability.compareTo(BigDecimal.valueOf(55)) >= 0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String forecastBacktestRecommendation(
            BigDecimal reliability,
            BigDecimal mape,
            List<AiForecastBacktestPointResponse> points
    ) {
        int actualVolume = points.stream().mapToInt(AiForecastBacktestPointResponse::actualUnits).sum();
        if (actualVolume < 5) {
            return "Collecter plus de ventes avant de faire confiance au modele.";
        }
        if (reliability.compareTo(BigDecimal.valueOf(55)) < 0) {
            return "Recalibrer le modele et verifier les ruptures cachees ou promotions.";
        }
        if (mape.compareTo(BigDecimal.valueOf(25)) > 0) {
            return "Surveiller ce produit, l'ecart reste significatif.";
        }
        return "Modele exploitable pour les commandes automatiques.";
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

    private Map<Long, Double> salesDemandByProduct(Long organisationId) {
        List<SalesDemand> demand = salesRepository.findDemandSince(organisationId, LocalDateTime.now().minusDays(90));
        return demand.stream()
                .collect(Collectors.groupingBy(
                        SalesDemand::productId,
                        Collectors.collectingAndThen(Collectors.toList(), this::dailyDemandFromSales)
                ));
    }

    private double dailyDemandFromSales(List<SalesDemand> demand) {
        if (demand.isEmpty()) {
            return 0;
        }
        int total = demand.stream().mapToInt(SalesDemand::quantity).sum();
        LocalDate oldest = demand.stream()
                .map(item -> item.soldAt().toLocalDate())
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());
        long days = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(oldest, LocalDate.now()) + 1);
        return total / (double) days;
    }

    private List<AiForecastResponse> toForecastResponses(Long organisationId, List<AiForecastEntity> forecasts) {
        Map<Long, Product> products = productMap(organisationId);
        Map<Long, ForecastQuality> qualityByProduct = forecastQualityByProduct(organisationId);
        return forecasts.stream()
                .map(entity -> {
                    Product product = products.get(entity.getProductId());
                    ForecastQuality quality = qualityByProduct.getOrDefault(entity.getProductId(), ForecastQuality.empty());
                    BigDecimal adjustedConfidence = adjustedConfidence(entity.getConfidenceScore(), quality);
                    return new AiForecastResponse(
                            entity.getId(),
                            entity.getProductId(),
                            product == null ? "Produit supprime" : product.name(),
                            product == null ? "-" : product.sku(),
                            entity.getHorizonDays(),
                            entity.getPredictedQuantity(),
                            adjustedConfidence,
                            confidenceLevel(adjustedConfidence, quality),
                            quality.backtestErrorPercent(),
                            quality.demandTrendPercent(),
                            quality.salesVolume30Days(),
                            demandSignal(quality),
                            entity.getModelName(),
                            entity.getSelectedModel() == null ? entity.getModelName() : entity.getSelectedModel(),
                            entity.getModelSelectionReason() == null ? "Selection par defaut." : entity.getModelSelectionReason(),
                            nullToZero(entity.getMovingAverageError()),
                            nullToZero(entity.getSeasonalError()),
                            entity.getFastapiError(),
                            entity.getGeneratedAt()
                    );
                })
                .toList();
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private Map<Long, ForecastQuality> forecastQualityByProduct(Long organisationId) {
        LocalDateTime now = LocalDateTime.now();
        List<SalesDemand> recent = salesRepository.findDemandSince(organisationId, now.minusDays(60));
        return recent.stream()
                .collect(Collectors.groupingBy(
                        SalesDemand::productId,
                        Collectors.collectingAndThen(Collectors.toList(), this::forecastQuality)
                ));
    }

    private ForecastQuality forecastQuality(List<SalesDemand> demand) {
        int last30 = quantityBetween(demand, LocalDate.now().minusDays(29), LocalDate.now());
        int previous30 = quantityBetween(demand, LocalDate.now().minusDays(59), LocalDate.now().minusDays(30));
        double trend = previous30 == 0
                ? (last30 == 0 ? 0 : 100)
                : ((last30 - previous30) * 100.0) / previous30;
        double expected = previous30 == 0 ? last30 : previous30;
        double error = expected == 0 ? 0 : Math.abs(last30 - expected) * 100.0 / expected;
        return new ForecastQuality(
                BigDecimal.valueOf(error).setScale(2, RoundingMode.HALF_UP),
                BigDecimal.valueOf(trend).setScale(2, RoundingMode.HALF_UP),
                last30
        );
    }

    private int quantityBetween(List<SalesDemand> demand, LocalDate from, LocalDate to) {
        return demand.stream()
                .filter(item -> {
                    LocalDate date = item.soldAt().toLocalDate();
                    return !date.isBefore(from) && !date.isAfter(to);
                })
                .mapToInt(SalesDemand::quantity)
                .sum();
    }

    private BigDecimal adjustedConfidence(BigDecimal baseConfidence, ForecastQuality quality) {
        double base = baseConfidence == null ? 60 : baseConfidence.doubleValue();
        double volumeBonus = Math.min(14, quality.salesVolume30Days() / 4.0);
        double errorPenalty = Math.min(28, quality.backtestErrorPercent().doubleValue() / 2.0);
        double trendPenalty = Math.abs(quality.demandTrendPercent().doubleValue()) > 80 ? 8 : 0;
        double adjusted = Math.max(35, Math.min(95, base + volumeBonus - errorPenalty - trendPenalty));
        return BigDecimal.valueOf(adjusted).setScale(2, RoundingMode.HALF_UP);
    }

    private String confidenceLevel(BigDecimal confidence, ForecastQuality quality) {
        if (quality.salesVolume30Days() < 5) {
            return "INSUFFICIENT_DATA";
        }
        if (confidence.doubleValue() >= 78) {
            return "HIGH";
        }
        if (confidence.doubleValue() >= 60) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String demandSignal(ForecastQuality quality) {
        if (quality.salesVolume30Days() < 5) {
            return "Donnees ventes insuffisantes";
        }
        double trend = quality.demandTrendPercent().doubleValue();
        if (trend >= 25) {
            return "Demande en hausse";
        }
        if (trend <= -25) {
            return "Demande en baisse";
        }
        return "Demande stable";
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

    private record ForecastQuality(
            BigDecimal backtestErrorPercent,
            BigDecimal demandTrendPercent,
            Integer salesVolume30Days
    ) {
        private static ForecastQuality empty() {
            return new ForecastQuality(BigDecimal.ZERO, BigDecimal.ZERO, 0);
        }
    }

    private record DemandModelSelection(
            String selectedModel,
            BigDecimal predictedQuantity,
            String reason,
            BigDecimal movingAverageError,
            BigDecimal seasonalError,
            BigDecimal fastApiError
    ) {
    }
}
