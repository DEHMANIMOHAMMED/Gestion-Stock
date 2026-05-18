package com.gestionstock.ai.application.service;

import com.gestionstock.ai.infrastructure.entity.AiRunEntity;
import com.gestionstock.ai.infrastructure.repository.AiRunRepository;
import com.gestionstock.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiRunServiceTest {

    private final AiRunRepository runRepository = mock(AiRunRepository.class);
    private final AiRunWorker runWorker = mock(AiRunWorker.class);
    private final AiRunService service = new AiRunService(runRepository, runWorker);

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void requestManualRunDoesNotStartAnotherWorkerWhenRunIsAlreadyActive() {
        TenantContext.setOrganisationId(42L);
        AiRunEntity activeRun = AiRunEntity.builder()
                .id(7L)
                .organisationId(42L)
                .status("RUNNING")
                .triggerType("MANUAL")
                .startedAt(LocalDateTime.now())
                .forecastsCount(0)
                .risksCount(0)
                .recommendationsCount(0)
                .anomaliesCount(0)
                .insightsCount(0)
                .modelSource("LOCAL_FALLBACK")
                .build();

        when(runRepository.existsByOrganisationIdAndStatusIn(42L, List.of("QUEUED", "RUNNING"))).thenReturn(true);
        when(runRepository.findTopByOrganisationIdOrderByStartedAtDesc(42L)).thenReturn(Optional.of(activeRun));

        var response = service.requestManualRun();

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.status()).isEqualTo("RUNNING");
        verify(runWorker, never()).runAsync(any(), any());
    }
}
