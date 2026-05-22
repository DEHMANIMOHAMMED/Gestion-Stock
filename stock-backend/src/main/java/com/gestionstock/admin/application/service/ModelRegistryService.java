package com.gestionstock.admin.application.service;

import com.gestionstock.admin.application.dto.ModelRegistryModelResponse;
import com.gestionstock.admin.application.dto.ModelRegistryProductResponse;
import com.gestionstock.admin.application.dto.ModelRegistryResponse;
import com.gestionstock.ai.infrastructure.entity.AiForecastEntity;
import com.gestionstock.ai.infrastructure.repository.AiForecastRepository;
import com.gestionstock.iam.infrastructure.entity.Role;
import com.gestionstock.iam.infrastructure.entity.User;
import com.gestionstock.product.domain.model.Product;
import com.gestionstock.product.domain.repository.ProductRepository;
import com.gestionstock.security.AuthenticatedUserProvider;
import com.gestionstock.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ModelRegistryService {

    private static final BigDecimal RECALIBRATION_THRESHOLD = BigDecimal.valueOf(45);

    private final AiForecastRepository forecastRepository;
    private final ProductRepository productRepository;
    private final AuthenticatedUserProvider userProvider;

    public ModelRegistryResponse registry() {
        User user = userProvider.requireUser();
        if (user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only admins can access model registry");
        }

        Long organisationId = TenantContext.requireOrganisationId();
        List<AiForecastEntity> forecasts = forecastRepository.findByOrganisationIdOrderByGeneratedAtDesc(organisationId);
        Map<Long, Product> products = productRepository.findAll(organisationId).stream()
                .collect(Collectors.toMap(Product::id, Function.identity()));
        Map<String, List<AiForecastEntity>> byModel = forecasts.stream()
                .collect(Collectors.groupingBy(this::selectedModel));

        List<ModelRegistryModelResponse> models = byModel.entrySet().stream()
                .map(entry -> toModelResponse(entry.getKey(), entry.getValue(), forecasts.size(), products))
                .sorted(Comparator.comparing(ModelRegistryModelResponse::usageCount).reversed())
                .toList();

        return new ModelRegistryResponse(
                LocalDateTime.now(),
                forecasts.size(),
                models.size(),
                averageError(forecasts),
                (int) forecasts.stream().filter(this::needsRecalibration).count(),
                models.isEmpty() ? "Aucun modele" : models.get(0).modelName(),
                models
        );
    }

    private ModelRegistryModelResponse toModelResponse(
            String modelName,
            List<AiForecastEntity> forecasts,
            int totalForecasts,
            Map<Long, Product> products
    ) {
        List<ModelRegistryProductResponse> productRows = forecasts.stream()
                .map(forecast -> toProductResponse(forecast, products.get(forecast.getProductId())))
                .toList();
        List<ModelRegistryProductResponse> recalibration = forecasts.stream()
                .filter(this::needsRecalibration)
                .sorted(Comparator.comparing(this::bestError).reversed())
                .limit(8)
                .map(forecast -> toProductResponse(forecast, products.get(forecast.getProductId())))
                .toList();

        return new ModelRegistryModelResponse(
                modelName,
                forecasts.size(),
                percentage(forecasts.size(), totalForecasts),
                averageError(forecasts),
                recalibration.size(),
                modelStatus(averageError(forecasts), recalibration.size()),
                productRows.stream()
                        .sorted(Comparator.comparing(ModelRegistryProductResponse::predictedQuantity).reversed())
                        .limit(8)
                        .toList(),
                recalibration
        );
    }

    private ModelRegistryProductResponse toProductResponse(AiForecastEntity forecast, Product product) {
        return new ModelRegistryProductResponse(
                forecast.getProductId(),
                product == null ? "Produit supprime" : product.name(),
                product == null ? "-" : product.sku(),
                forecast.getHorizonDays(),
                scale(forecast.getPredictedQuantity()),
                scale(forecast.getMovingAverageError()),
                scale(forecast.getSeasonalError()),
                forecast.getFastapiError() == null ? null : scale(forecast.getFastapiError()),
                forecast.getModelSelectionReason() == null ? "Selection par defaut." : forecast.getModelSelectionReason()
        );
    }

    private String selectedModel(AiForecastEntity forecast) {
        return forecast.getSelectedModel() == null || forecast.getSelectedModel().isBlank()
                ? forecast.getModelName()
                : forecast.getSelectedModel();
    }

    private boolean needsRecalibration(AiForecastEntity forecast) {
        return bestError(forecast).compareTo(RECALIBRATION_THRESHOLD) >= 0;
    }

    private BigDecimal bestError(AiForecastEntity forecast) {
        List<BigDecimal> values = List.of(
                scale(forecast.getMovingAverageError()),
                scale(forecast.getSeasonalError()),
                forecast.getFastapiError() == null ? BigDecimal.valueOf(999) : scale(forecast.getFastapiError())
        );
        return values.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
    }

    private BigDecimal averageError(List<AiForecastEntity> forecasts) {
        if (forecasts.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = forecasts.stream()
                .map(this::bestError)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(forecasts.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentage(long count, long total) {
        if (total == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(count)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private String modelStatus(BigDecimal averageError, int recalibrationCount) {
        if (averageError.compareTo(BigDecimal.valueOf(60)) >= 0 || recalibrationCount >= 5) {
            return "RECALIBRATE";
        }
        if (averageError.compareTo(BigDecimal.valueOf(35)) >= 0 || recalibrationCount > 0) {
            return "WATCH";
        }
        return "HEALTHY";
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }
}
