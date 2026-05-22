package com.gestionstock.ai.presentation.controller;

import com.gestionstock.ai.application.dto.*;
import com.gestionstock.ai.application.service.AiDecisionService;
import com.gestionstock.ai.application.service.AiExperienceService;
import com.gestionstock.ai.application.service.AiRunService;
import com.gestionstock.security.PlanAccessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiDecisionService aiDecisionService;
    private final AiRunService aiRunService;
    private final AiExperienceService aiExperienceService;
    private final PlanAccessService planAccessService;

    @GetMapping("/dashboard")
    public ResponseEntity<AiDashboardResponse> dashboard() {
        return ResponseEntity.ok(aiDecisionService.getDashboard());
    }

    @PostMapping("/runs")
    public ResponseEntity<AiRunResponse> requestRun() {
        return ResponseEntity.accepted().body(aiRunService.requestManualRun());
    }

    @GetMapping("/runs")
    public ResponseEntity<List<AiRunResponse>> runs() {
        return ResponseEntity.ok(aiRunService.latestRuns());
    }

    @GetMapping("/runs/latest")
    public ResponseEntity<AiRunResponse> latestRun() {
        AiRunResponse latestRun = aiRunService.latestRun();
        return latestRun == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(latestRun);
    }

    @GetMapping("/forecasts")
    public ResponseEntity<List<AiForecastResponse>> forecasts(
            @RequestParam(required = false) Integer horizon
    ) {
        planAccessService.requireProPlan();
        return ResponseEntity.ok(aiDecisionService.getForecasts(horizon));
    }

    @GetMapping("/forecasts/backtest")
    public ResponseEntity<List<AiForecastBacktestResponse>> forecastBacktests(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Integer horizon
    ) {
        planAccessService.requireProPlan();
        return ResponseEntity.ok(aiDecisionService.getForecastBacktests(productId, horizon));
    }

    @GetMapping("/stockout-risks")
    public ResponseEntity<List<AiStockoutRiskResponse>> stockoutRisks() {
        return ResponseEntity.ok(aiDecisionService.getStockoutRisks());
    }

    @GetMapping("/reorder-recommendations")
    public ResponseEntity<List<AiReorderRecommendationResponse>> reorderRecommendations() {
        planAccessService.requireProPlan();
        return ResponseEntity.ok(aiDecisionService.getReorderRecommendations());
    }

    @GetMapping("/anomalies")
    public ResponseEntity<List<AiAnomalyResponse>> anomalies() {
        planAccessService.requireProPlan();
        return ResponseEntity.ok(aiDecisionService.getAnomalies());
    }

    @GetMapping("/insights")
    public ResponseEntity<List<AiInsightResponse>> insights() {
        return ResponseEntity.ok(aiDecisionService.getInsights());
    }

    @GetMapping("/experience-dashboard")
    public ResponseEntity<AiExecutiveDashboardResponse> experienceDashboard() {
        planAccessService.requireProPlan();
        return ResponseEntity.ok(aiExperienceService.executiveDashboard());
    }

    @GetMapping("/stock-health")
    public ResponseEntity<List<AiProductHealthResponse>> stockHealth() {
        planAccessService.requireProPlan();
        return ResponseEntity.ok(aiExperienceService.stockHealth());
    }

    @PostMapping("/what-if")
    public ResponseEntity<AiWhatIfResponse> whatIf(@Valid @RequestBody AiWhatIfRequest request) {
        planAccessService.requireProPlan();
        return ResponseEntity.ok(aiExperienceService.whatIf(request));
    }

    @PostMapping("/copilot")
    public ResponseEntity<AiCopilotResponse> copilot(@Valid @RequestBody AiCopilotRequest request) {
        planAccessService.requireProPlan();
        return ResponseEntity.ok(aiExperienceService.copilot(request));
    }

    @GetMapping("/copilot/history")
    public ResponseEntity<List<AiCopilotConversationResponse>> copilotHistory() {
        planAccessService.requireProPlan();
        return ResponseEntity.ok(aiExperienceService.copilotHistory());
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AiAuditLogResponse>> auditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actorEmail,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        planAccessService.requireProPlan();
        return ResponseEntity.ok(aiExperienceService.auditLogs(action, actorEmail, targetType, source, module, severity, from, to));
    }

    @GetMapping(value = "/audit-logs/export/csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportAuditLogsCsv(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actorEmail,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        planAccessService.requireProPlan();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit-logs.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(aiExperienceService.exportAuditLogsCsv(action, actorEmail, targetType, source, module, severity, from, to));
    }

    @GetMapping(value = "/audit-logs/export/pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> exportAuditLogsPdf(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actorEmail,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        planAccessService.requireProPlan();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit-logs.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(aiExperienceService.exportAuditLogsPdf(action, actorEmail, targetType, source, module, severity, from, to));
    }

    @GetMapping("/reorder-recommendations/{id}/explanation")
    public ResponseEntity<AiRecommendationExplanationResponse> explainRecommendation(@PathVariable Long id) {
        planAccessService.requireProPlan();
        return ResponseEntity.ok(aiExperienceService.explainRecommendation(id));
    }
}
