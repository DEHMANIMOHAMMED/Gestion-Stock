package com.gestionstock.admin.presentation.controller;

import com.gestionstock.admin.application.dto.ModelRegistryResponse;
import com.gestionstock.admin.application.service.ModelRegistryService;
import com.gestionstock.security.PlanAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/model-registry")
@RequiredArgsConstructor
public class ModelRegistryController {

    private final ModelRegistryService service;
    private final PlanAccessService planAccessService;

    @GetMapping
    public ResponseEntity<ModelRegistryResponse> registry() {
        planAccessService.requireProPlan();
        return ResponseEntity.ok(service.registry());
    }
}
