package com.gestionstock.sales.presentation.controller;

import com.gestionstock.sales.application.dto.SaleRequest;
import com.gestionstock.sales.application.dto.SaleResponse;
import com.gestionstock.sales.application.dto.SalesSummaryResponse;
import com.gestionstock.sales.application.dto.DemandAnalyticsResponse;
import com.gestionstock.sales.domain.service.SalesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sales")
@RequiredArgsConstructor
public class SalesController {

    private final SalesService salesService;

    @PostMapping
    public ResponseEntity<SaleResponse> create(@Valid @RequestBody SaleRequest request) {
        return ResponseEntity.ok(salesService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<SaleResponse>> findRecent() {
        return ResponseEntity.ok(salesService.findRecent());
    }

    @GetMapping("/summary")
    public ResponseEntity<SalesSummaryResponse> summary() {
        return ResponseEntity.ok(salesService.summary());
    }

    @GetMapping("/analytics")
    public ResponseEntity<DemandAnalyticsResponse> analytics() {
        return ResponseEntity.ok(salesService.analytics());
    }
}
