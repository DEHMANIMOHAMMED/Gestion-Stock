package com.gestionstock.ai.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiRunWorker {

    private final AiDecisionService aiDecisionService;

    @Async("aiTaskExecutor")
    public void runAsync(Long runId, Long organisationId) {
        aiDecisionService.executeRun(runId, organisationId);
    }
}
