package com.gestionstock.sales.domain.service;

import com.gestionstock.ai.infrastructure.entity.AiForecastEntity;
import com.gestionstock.ai.infrastructure.repository.AiForecastRepository;
import com.gestionstock.iam.infrastructure.entity.Organisation;
import com.gestionstock.iam.infrastructure.repository.OrganisationRepository;
import com.gestionstock.notification.domain.service.AdminNotificationService;
import com.gestionstock.product.domain.model.Product;
import com.gestionstock.product.domain.repository.ProductRepository;
import com.gestionstock.sales.application.dto.*;
import com.gestionstock.sales.domain.model.Sale;
import com.gestionstock.sales.domain.model.SaleLine;
import com.gestionstock.sales.domain.model.SaleStatus;
import com.gestionstock.sales.domain.model.SalesChannel;
import com.gestionstock.sales.domain.repository.SalesRepository;
import com.gestionstock.security.TenantContext;
import com.gestionstock.stock.domain.model.Stock;
import com.gestionstock.stock.domain.repository.StockRepository;
import com.gestionstock.stock.domain.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalesService {

    private final SalesRepository salesRepository;
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final StockService stockService;
    private final AiForecastRepository forecastRepository;
    private final OrganisationRepository organisationRepository;
    private final AdminNotificationService notificationService;

    @Transactional
    public SaleResponse create(SaleRequest request) {
        Long organisationId = TenantContext.requireOrganisationId();
        SalesChannel channel = SalesChannel.from(request.channel());
        Map<Long, Product> productsById = productRepository.findAll(organisationId).stream()
                .collect(Collectors.toMap(Product::id, Function.identity()));

        List<SaleLine> lines = request.lines().stream()
                .map(line -> toLine(organisationId, productsById, line))
                .toList();
        BigDecimal total = lines.stream()
                .map(SaleLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        LocalDateTime now = LocalDateTime.now();
        Sale sale = Sale.builder()
                .organisationId(organisationId)
                .reference(reference(now))
                .customerName(trimToNull(request.customerName()))
                .channel(channel)
                .status(SaleStatus.COMPLETED)
                .totalAmount(total)
                .soldAt(now)
                .createdAt(now)
                .lines(lines)
                .build();

        lines.forEach(line -> stockService.registerMovement(line.productId(), line.quantity(), "OUT"));
        Sale saved = salesRepository.save(sale);
        createForecastDivergenceAlerts(organisationId, lines, productsById);
        return toResponse(saved, productsById);
    }

    public List<SaleResponse> findRecent() {
        Long organisationId = TenantContext.requireOrganisationId();
        Map<Long, Product> productsById = productRepository.findAll(organisationId).stream()
                .collect(Collectors.toMap(Product::id, Function.identity()));
        return salesRepository.findRecent(organisationId, 100).stream()
                .map(sale -> toResponse(sale, productsById))
                .toList();
    }

    public SalesSummaryResponse summary() {
        Long organisationId = TenantContext.requireOrganisationId();
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<Sale> sales = salesRepository.findBetween(organisationId, since, LocalDateTime.now());
        Map<Long, Product> productsById = productRepository.findAll(organisationId).stream()
                .collect(Collectors.toMap(Product::id, Function.identity()));

        BigDecimal revenue = sales.stream()
                .filter(sale -> sale.status() == SaleStatus.COMPLETED)
                .map(Sale::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        int units = sales.stream()
                .flatMap(sale -> sale.lines().stream())
                .mapToInt(SaleLine::quantity)
                .sum();
        BigDecimal averageBasket = sales.isEmpty()
                ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(sales.size()), 2, RoundingMode.HALF_UP);
        Map<Long, ProductDemandAccumulator> byProduct = new LinkedHashMap<>();
        sales.stream()
                .flatMap(sale -> sale.lines().stream())
                .forEach(line -> byProduct.computeIfAbsent(line.productId(), ProductDemandAccumulator::new).add(line));

        List<ProductDemandResponse> topProducts = byProduct.values().stream()
                .sorted(Comparator.comparing(ProductDemandAccumulator::revenue).reversed())
                .limit(8)
                .map(item -> item.toResponse(productsById.get(item.productId())))
                .toList();
        return new SalesSummaryResponse(revenue, units, sales.size(), averageBasket, topProducts);
    }

    public DemandAnalyticsResponse analytics() {
        Long organisationId = TenantContext.requireOrganisationId();
        LocalDateTime now = LocalDateTime.now();
        List<Sale> sales = salesRepository.findBetween(organisationId, now.minusDays(60), now);
        Map<Long, Product> productsById = productRepository.findAll(organisationId).stream()
                .collect(Collectors.toMap(Product::id, Function.identity()));
        Map<Long, Stock> stockByProductId = stockRepository.findAll(organisationId).stream()
                .collect(Collectors.toMap(Stock::productId, Function.identity()));

        List<Sale> last30Sales = sales.stream()
                .filter(sale -> !sale.soldAt().isBefore(now.minusDays(30)))
                .toList();
        List<Sale> previous30Sales = sales.stream()
                .filter(sale -> sale.soldAt().isBefore(now.minusDays(30)))
                .toList();
        Map<Long, ProductDemandAccumulator> currentByProduct = productDemand(last30Sales);
        Map<Long, ProductDemandAccumulator> previousByProduct = productDemand(previous30Sales);
        List<ProductDemandTrendResponse> trends = currentByProduct.values().stream()
                .map(item -> toTrend(item, previousByProduct.get(item.productId()), productsById.get(item.productId()), stockByProductId))
                .toList();

        List<DemandForecastComparisonResponse> forecastComparisons = forecastComparisons(
                organisationId,
                productsById,
                currentByProduct
        );

        return new DemandAnalyticsResponse(
                summaryFromSales(last30Sales, productsById),
                dailySeries(last30Sales),
                channels(last30Sales),
                trends.stream()
                        .sorted(Comparator.comparing(ProductDemandTrendResponse::trendPercent).reversed())
                        .limit(6)
                        .toList(),
                trends.stream()
                        .sorted(Comparator.comparing(ProductDemandTrendResponse::trendPercent))
                        .limit(6)
                        .toList(),
                trends.stream()
                        .filter(item -> item.currentPeriodUnits() >= 5 && item.currentStock() <= Math.max(item.minStock() * 2, item.currentPeriodUnits()))
                        .sorted(Comparator.comparing(ProductDemandTrendResponse::currentPeriodUnits).reversed())
                        .limit(8)
                        .toList(),
                seasonality(organisationId, last30Sales),
                forecastComparisons,
                forecastAlerts(forecastComparisons)
        );
    }

    private List<DemandForecastComparisonResponse> forecastComparisons(
            Long organisationId,
            Map<Long, Product> productsById,
            Map<Long, ProductDemandAccumulator> currentByProduct
    ) {
        Map<Long, AiForecastEntity> latestForecasts = forecastRepository
                .findByOrganisationIdAndHorizonDaysOrderByGeneratedAtDesc(organisationId, 30)
                .stream()
                .collect(Collectors.toMap(AiForecastEntity::getProductId, Function.identity(), (first, ignored) -> first));

        return latestForecasts.values().stream()
                .map(forecast -> {
                    Product product = productsById.get(forecast.getProductId());
                    int actual = currentByProduct.getOrDefault(
                            forecast.getProductId(),
                            new ProductDemandAccumulator(forecast.getProductId())
                    ).quantity();
                    BigDecimal variance = variancePercent(BigDecimal.valueOf(actual), forecast.getPredictedQuantity());
                    return new DemandForecastComparisonResponse(
                            forecast.getProductId(),
                            product == null ? "Produit supprime" : product.name(),
                            product == null ? "-" : product.sku(),
                            actual,
                            forecast.getPredictedQuantity().setScale(2, RoundingMode.HALF_UP),
                            variance,
                            forecast.getConfidenceScore().setScale(2, RoundingMode.HALF_UP),
                            forecastStatus(variance, actual, forecast.getPredictedQuantity()),
                            forecastRecommendation(variance, actual, forecast.getPredictedQuantity())
                    );
                })
                .sorted(Comparator.comparing((DemandForecastComparisonResponse item) -> item.variancePercent().abs()).reversed())
                .limit(10)
                .toList();
    }

    private List<DemandForecastAlertResponse> forecastAlerts(List<DemandForecastComparisonResponse> comparisons) {
        return comparisons.stream()
                .filter(item -> item.variancePercent().abs().compareTo(BigDecimal.valueOf(35)) >= 0 && item.actualUnits30Days() >= 5)
                .map(item -> new DemandForecastAlertResponse(
                        item.productId(),
                        item.productName(),
                        item.sku(),
                        item.variancePercent().compareTo(BigDecimal.ZERO) > 0 ? "HIGH" : "MEDIUM",
                        item.variancePercent(),
                        "Ecart reel vs prevision de " + item.variancePercent() + "% sur 30 jours.",
                        item.variancePercent().compareTo(BigDecimal.ZERO) > 0
                                ? "Recalculer le stock de securite et verifier la commande fournisseur."
                                : "Verifier ralentissement demande avant prochain reapprovisionnement."
                ))
                .toList();
    }

    private void createForecastDivergenceAlerts(Long organisationId, List<SaleLine> lines, Map<Long, Product> productsById) {
        Map<Long, ProductDemandAccumulator> actualByProduct = productDemand(
                salesRepository.findBetween(organisationId, LocalDateTime.now().minusDays(30), LocalDateTime.now())
        );
        Map<Long, AiForecastEntity> latestForecasts = forecastRepository
                .findByOrganisationIdAndHorizonDaysOrderByGeneratedAtDesc(organisationId, 30)
                .stream()
                .collect(Collectors.toMap(AiForecastEntity::getProductId, Function.identity(), (first, ignored) -> first));
        lines.stream()
                .map(SaleLine::productId)
                .distinct()
                .forEach(productId -> {
                    AiForecastEntity forecast = latestForecasts.get(productId);
                    if (forecast == null) {
                        return;
                    }
                    int actual = actualByProduct.getOrDefault(productId, new ProductDemandAccumulator(productId)).quantity();
                    BigDecimal variance = variancePercent(BigDecimal.valueOf(actual), forecast.getPredictedQuantity());
                    if (actual < 5 || variance.abs().compareTo(BigDecimal.valueOf(35)) < 0) {
                        return;
                    }
                    Product product = productsById.get(productId);
                    String severity = variance.compareTo(BigDecimal.ZERO) > 0 ? "HIGH" : "MEDIUM";
                    notificationService.createOnce(
                            organisationId,
                            "DEMAND_FORECAST_DIVERGENCE",
                            severity,
                            "Ecart demande detecte: " + (product == null ? "produit " + productId : product.name()),
                            "Les ventes reelles sur 30 jours divergent de " + variance + "% de la prevision IA.",
                            null,
                            null,
                            "demand-forecast-divergence-" + productId + "-" + LocalDate.now()
                    );
                });
    }

    private DemandSeasonalityResponse seasonality(Long organisationId, List<Sale> sales) {
        Map<DayOfWeek, List<Sale>> byWeekday = sales.stream()
                .collect(Collectors.groupingBy(sale -> sale.soldAt().getDayOfWeek()));
        List<SalesPeriodPointResponse> weekdaySeries = List.of(
                weekdayPoint(DayOfWeek.MONDAY, byWeekday),
                weekdayPoint(DayOfWeek.TUESDAY, byWeekday),
                weekdayPoint(DayOfWeek.WEDNESDAY, byWeekday),
                weekdayPoint(DayOfWeek.THURSDAY, byWeekday),
                weekdayPoint(DayOfWeek.FRIDAY, byWeekday),
                weekdayPoint(DayOfWeek.SATURDAY, byWeekday),
                weekdayPoint(DayOfWeek.SUNDAY, byWeekday)
        );
        int max = weekdaySeries.stream().mapToInt(SalesPeriodPointResponse::unitsSold).max().orElse(0);
        double average = weekdaySeries.stream().mapToInt(SalesPeriodPointResponse::unitsSold).average().orElse(0);
        BigDecimal index = average == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(max / average).setScale(2, RoundingMode.HALF_UP);
        String industry = organisationRepository.findById(organisationId)
                .map(Organisation::getIndustry)
                .filter(value -> !value.isBlank())
                .orElse("General");
        return new DemandSeasonalityResponse(
                industry,
                sectorPattern(industry),
                index,
                weekdaySeries,
                seasonalityInsight(index, industry)
        );
    }

    private SalesPeriodPointResponse weekdayPoint(DayOfWeek day, Map<DayOfWeek, List<Sale>> byWeekday) {
        List<Sale> daySales = byWeekday.getOrDefault(day, List.of());
        BigDecimal revenue = daySales.stream()
                .map(Sale::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        int units = daySales.stream().flatMap(sale -> sale.lines().stream()).mapToInt(SaleLine::quantity).sum();
        return new SalesPeriodPointResponse(LocalDate.now().with(day), revenue, units, daySales.size());
    }

    private String sectorPattern(String industry) {
        String normalized = industry == null ? "" : industry.toLowerCase();
        if (normalized.contains("pharma")) {
            return "Demande reguliere avec pics sanitaires possibles";
        }
        if (normalized.contains("garage") || normalized.contains("auto")) {
            return "Demande concentree sur jours ouvrables";
        }
        if (normalized.contains("boutique") || normalized.contains("retail")) {
            return "Pics attendus fin de semaine et promotions";
        }
        return "Saisonnalite faible a confirmer par historique";
    }

    private String seasonalityInsight(BigDecimal index, String industry) {
        if (index.compareTo(BigDecimal.valueOf(1.4)) >= 0) {
            return "La demande est concentree sur certains jours; ajuste les seuils avant ces pics.";
        }
        return "La demande reste assez reguliere pour le secteur " + industry + ".";
    }

    private BigDecimal variancePercent(BigDecimal actual, BigDecimal forecast) {
        if (forecast == null || forecast.compareTo(BigDecimal.ZERO) == 0) {
            return actual.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(100);
        }
        return actual.subtract(forecast)
                .multiply(BigDecimal.valueOf(100))
                .divide(forecast, 2, RoundingMode.HALF_UP);
    }

    private String forecastStatus(BigDecimal variance, int actual, BigDecimal forecast) {
        if (actual == 0 && forecast.compareTo(BigDecimal.ZERO) > 0) {
            return "Demande absente";
        }
        if (variance.abs().compareTo(BigDecimal.valueOf(35)) >= 0) {
            return variance.compareTo(BigDecimal.ZERO) > 0 ? "Sous-estime" : "Surestime";
        }
        if (variance.abs().compareTo(BigDecimal.valueOf(15)) >= 0) {
            return "A surveiller";
        }
        return "Aligne";
    }

    private String forecastRecommendation(BigDecimal variance, int actual, BigDecimal forecast) {
        if (actual == 0 && forecast.compareTo(BigDecimal.ZERO) > 0) {
            return "Verifier si le produit est inactif ou en rupture cachee.";
        }
        if (variance.compareTo(BigDecimal.valueOf(35)) >= 0) {
            return "Augmenter la couverture et recalculer les commandes.";
        }
        if (variance.compareTo(BigDecimal.valueOf(-35)) <= 0) {
            return "Reduire le reapprovisionnement avant surstock.";
        }
        return "Conserver le modele, ecart acceptable.";
    }

    private SaleLine toLine(Long organisationId, Map<Long, Product> productsById, SaleLineRequest request) {
        if (!productsById.containsKey(request.productId())) {
            throw new IllegalArgumentException("Product not found");
        }
        BigDecimal unitPrice = request.unitPrice().setScale(2, RoundingMode.HALF_UP);
        return SaleLine.builder()
                .organisationId(organisationId)
                .productId(request.productId())
                .quantity(request.quantity())
                .unitPrice(unitPrice)
                .lineTotal(unitPrice.multiply(BigDecimal.valueOf(request.quantity())).setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    private SalesSummaryResponse summaryFromSales(List<Sale> sales, Map<Long, Product> productsById) {
        BigDecimal revenue = sales.stream()
                .filter(sale -> sale.status() == SaleStatus.COMPLETED)
                .map(Sale::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        int units = sales.stream()
                .flatMap(sale -> sale.lines().stream())
                .mapToInt(SaleLine::quantity)
                .sum();
        BigDecimal averageBasket = sales.isEmpty()
                ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(sales.size()), 2, RoundingMode.HALF_UP);
        List<ProductDemandResponse> topProducts = productDemand(sales).values().stream()
                .sorted(Comparator.comparing(ProductDemandAccumulator::revenue).reversed())
                .limit(8)
                .map(item -> item.toResponse(productsById.get(item.productId())))
                .toList();
        return new SalesSummaryResponse(revenue, units, sales.size(), averageBasket, topProducts);
    }

    private List<SalesPeriodPointResponse> dailySeries(List<Sale> sales) {
        LocalDate start = LocalDate.now().minusDays(29);
        Map<LocalDate, List<Sale>> salesByDate = sales.stream()
                .collect(Collectors.groupingBy(sale -> sale.soldAt().toLocalDate()));
        List<SalesPeriodPointResponse> points = new ArrayList<>();
        for (int offset = 0; offset < 30; offset++) {
            LocalDate date = start.plusDays(offset);
            List<Sale> daySales = salesByDate.getOrDefault(date, List.of());
            BigDecimal revenue = daySales.stream()
                    .map(Sale::totalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
            int units = daySales.stream().flatMap(sale -> sale.lines().stream()).mapToInt(SaleLine::quantity).sum();
            points.add(new SalesPeriodPointResponse(date, revenue, units, daySales.size()));
        }
        return points;
    }

    private List<SalesChannelResponse> channels(List<Sale> sales) {
        BigDecimal totalRevenue = sales.stream()
                .map(Sale::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sales.stream()
                .collect(Collectors.groupingBy(Sale::channel))
                .entrySet()
                .stream()
                .map(entry -> {
                    List<Sale> channelSales = entry.getValue();
                    BigDecimal revenue = channelSales.stream().map(Sale::totalAmount).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
                    int units = channelSales.stream().flatMap(sale -> sale.lines().stream()).mapToInt(SaleLine::quantity).sum();
                    BigDecimal share = totalRevenue.compareTo(BigDecimal.ZERO) == 0
                            ? BigDecimal.ZERO
                            : revenue.multiply(BigDecimal.valueOf(100)).divide(totalRevenue, 2, RoundingMode.HALF_UP);
                    return new SalesChannelResponse(entry.getKey().name(), revenue, units, channelSales.size(), share);
                })
                .sorted(Comparator.comparing(SalesChannelResponse::revenue).reversed())
                .toList();
    }

    private Map<Long, ProductDemandAccumulator> productDemand(List<Sale> sales) {
        Map<Long, ProductDemandAccumulator> byProduct = new LinkedHashMap<>();
        sales.stream()
                .flatMap(sale -> sale.lines().stream())
                .forEach(line -> byProduct.computeIfAbsent(line.productId(), ProductDemandAccumulator::new).add(line));
        return byProduct;
    }

    private ProductDemandTrendResponse toTrend(
            ProductDemandAccumulator current,
            ProductDemandAccumulator previous,
            Product product,
            Map<Long, Stock> stockByProductId
    ) {
        int previousQuantity = previous == null ? 0 : previous.quantity();
        BigDecimal trend = trend(current.quantity(), previousQuantity);
        int stock = stockByProductId.getOrDefault(current.productId(), Stock.builder().quantity(0).build()).quantity();
        int minStock = product == null || product.minStock() == null ? 0 : product.minStock();
        return new ProductDemandTrendResponse(
                current.productId(),
                product == null ? "Produit supprime" : product.name(),
                product == null ? "-" : product.sku(),
                current.quantity(),
                previousQuantity,
                trend,
                current.revenue().setScale(2, RoundingMode.HALF_UP),
                stock,
                minStock,
                demandSignal(trend, stock, minStock, current.quantity())
        );
    }

    private BigDecimal trend(int current, int previous) {
        if (previous == 0) {
            return BigDecimal.valueOf(current == 0 ? 0 : 100).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf((current - previous) * 100.0 / previous).setScale(2, RoundingMode.HALF_UP);
    }

    private String demandSignal(BigDecimal trend, int stock, int minStock, int units) {
        if (stock <= minStock && units > 0) {
            return "Demande forte + stock faible";
        }
        if (trend.doubleValue() >= 25) {
            return "Hausse rapide";
        }
        if (trend.doubleValue() <= -25) {
            return "Baisse nette";
        }
        return "Stable";
    }

    private SaleResponse toResponse(Sale sale, Map<Long, Product> productsById) {
        return new SaleResponse(
                sale.id(),
                sale.reference(),
                sale.customerName(),
                sale.channel().name(),
                sale.status().name(),
                sale.totalAmount(),
                sale.soldAt(),
                sale.lines().stream()
                        .map(line -> toLineResponse(line, productsById.get(line.productId())))
                        .toList()
        );
    }

    private SaleLineResponse toLineResponse(SaleLine line, Product product) {
        return new SaleLineResponse(
                line.id(),
                line.productId(),
                product == null ? "Produit supprime" : product.name(),
                product == null ? "-" : product.sku(),
                line.quantity(),
                line.unitPrice(),
                line.lineTotal()
        );
    }

    private String reference(LocalDateTime now) {
        return "SALE-" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static final class ProductDemandAccumulator {
        private final Long productId;
        private int quantity;
        private BigDecimal revenue = BigDecimal.ZERO;

        private ProductDemandAccumulator(Long productId) {
            this.productId = productId;
        }

        private void add(SaleLine line) {
            quantity += line.quantity();
            revenue = revenue.add(line.lineTotal());
        }

        private Long productId() {
            return productId;
        }

        private BigDecimal revenue() {
            return revenue;
        }

        private int quantity() {
            return quantity;
        }

        private ProductDemandResponse toResponse(Product product) {
            return new ProductDemandResponse(
                    productId,
                    product == null ? "Produit supprime" : product.name(),
                    product == null ? "-" : product.sku(),
                    quantity,
                    revenue.setScale(2, RoundingMode.HALF_UP)
            );
        }
    }
}
