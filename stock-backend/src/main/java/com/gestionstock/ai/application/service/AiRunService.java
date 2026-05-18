package com.gestionstock.ai.application.service;

import com.gestionstock.ai.application.dto.AiRunResponse;
import com.gestionstock.ai.infrastructure.entity.AiRunEntity;
import com.gestionstock.ai.infrastructure.repository.AiRunRepository;
import com.gestionstock.iam.infrastructure.entity.User;
import com.gestionstock.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiRunService {

    private static final List<String> ACTIVE_STATUSES = List.of("QUEUED", "RUNNING");

    private final AiRunRepository runRepository;
    private final AiRunWorker runWorker;

    @Transactional
    public AiRunResponse requestManualRun() {
        Long organisationId = TenantContext.requireOrganisationId();
        QueuedRun queuedRun = createQueuedRun(organisationId, currentUserId(), "MANUAL");
        if (queuedRun.created()) {
            runWorker.runAsync(queuedRun.run().getId(), organisationId);
        }
        return toResponse(queuedRun.run());
    }

    @Transactional
    public QueuedRun createQueuedRun(Long organisationId, Long userId, String triggerType) {
        if (runRepository.existsByOrganisationIdAndStatusIn(organisationId, ACTIVE_STATUSES)) {
            AiRunEntity activeRun = runRepository.findTopByOrganisationIdOrderByStartedAtDesc(organisationId)
                    .filter(run -> ACTIVE_STATUSES.contains(run.getStatus()))
                    .orElseGet(() -> saveQueuedRun(organisationId, userId, triggerType));
            return new QueuedRun(activeRun, false);
        }
        return new QueuedRun(saveQueuedRun(organisationId, userId, triggerType), true);
    }

    @Transactional(readOnly = true)
    public List<AiRunResponse> latestRuns() {
        Long organisationId = TenantContext.requireOrganisationId();
        return runRepository.findTop10ByOrganisationIdOrderByStartedAtDesc(organisationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AiRunResponse latestRun() {
        Long organisationId = TenantContext.requireOrganisationId();
        return runRepository.findTopByOrganisationIdOrderByStartedAtDesc(organisationId)
                .map(this::toResponse)
                .orElse(null);
    }

    public AiRunResponse toResponse(AiRunEntity run) {
        return new AiRunResponse(
                run.getId(),
                run.getStatus(),
                run.getTriggerType(),
                run.getStartedAt(),
                run.getCompletedAt(),
                run.getErrorMessage(),
                run.getForecastsCount(),
                run.getRisksCount(),
                run.getRecommendationsCount(),
                run.getAnomaliesCount(),
                run.getInsightsCount(),
                run.getModelSource()
        );
    }

    private AiRunEntity saveQueuedRun(Long organisationId, Long userId, String triggerType) {
        return runRepository.save(AiRunEntity.builder()
                .organisationId(organisationId)
                .requestedByUserId(userId)
                .status("QUEUED")
                .triggerType(triggerType)
                .startedAt(LocalDateTime.now())
                .forecastsCount(0)
                .risksCount(0)
                .recommendationsCount(0)
                .anomaliesCount(0)
                .insightsCount(0)
                .modelSource("LOCAL_FALLBACK")
                .build());
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return null;
        }
        return user.getId();
    }

    public record QueuedRun(AiRunEntity run, boolean created) {
    }
}
