package com.gestionstock.ai.application.service;

import com.gestionstock.iam.infrastructure.repository.OrganisationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class AiScheduledRunService {

    private final OrganisationRepository organisationRepository;
    private final AiRunService runService;
    private final AiRunWorker runWorker;

    @Scheduled(fixedDelayString = "${ai.jobs.fixed-delay-ms:900000}", initialDelayString = "${ai.jobs.initial-delay-ms:60000}")
    public void scheduleRunsForAllTenants() {
        organisationRepository.findAll().forEach(organisation -> {
            var queuedRun = runService.createQueuedRun(organisation.getId(), null, "SCHEDULED");
            if (queuedRun.created()) {
                runWorker.runAsync(queuedRun.run().getId(), organisation.getId());
            }
        });
    }
}
