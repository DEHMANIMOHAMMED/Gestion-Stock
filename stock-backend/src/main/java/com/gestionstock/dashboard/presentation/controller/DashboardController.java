package com.gestionstock.dashboard.presentation.controller;

import com.gestionstock.dashboard.application.dto.DashboardSummaryResponse;
import com.gestionstock.dashboard.application.dto.LowStockAlertResponse;
import com.gestionstock.dashboard.application.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<LowStockAlertResponse>> getLowStockAlerts() {
        return ResponseEntity.ok(dashboardService.getLowStockAlerts());
    }
}
