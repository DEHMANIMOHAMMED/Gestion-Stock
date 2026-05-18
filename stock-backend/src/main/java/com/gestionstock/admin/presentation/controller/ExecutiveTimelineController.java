package com.gestionstock.admin.presentation.controller;

import com.gestionstock.admin.application.dto.ExecutiveTimelineResponse;
import com.gestionstock.admin.application.service.ExecutiveTimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/executive-timeline")
@RequiredArgsConstructor
public class ExecutiveTimelineController {

    private final ExecutiveTimelineService service;

    @GetMapping
    public ResponseEntity<ExecutiveTimelineResponse> report(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(service.report(date));
    }

    @GetMapping(value = "/export/csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate reportDate = date == null ? LocalDate.now() : date;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=daily-report-" + reportDate + ".csv")
                .body(service.exportCsv(reportDate));
    }

    @GetMapping(value = "/export/pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate reportDate = date == null ? LocalDate.now() : date;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=daily-report-" + reportDate + ".pdf")
                .body(service.exportPdf(reportDate));
    }
}
