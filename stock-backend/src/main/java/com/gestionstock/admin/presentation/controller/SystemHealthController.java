package com.gestionstock.admin.presentation.controller;

import com.gestionstock.admin.application.dto.SystemHealthResponse;
import com.gestionstock.admin.application.service.SystemHealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/system-health")
@RequiredArgsConstructor
public class SystemHealthController {

    private final SystemHealthService service;

    @GetMapping
    public ResponseEntity<SystemHealthResponse> health() {
        return ResponseEntity.ok(service.health());
    }
}
