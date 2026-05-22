package com.gestionstock.iam.presentation.controller;

import com.gestionstock.iam.application.service.OrganisationUserService;
import com.gestionstock.iam.presentation.dto.CreateOrganisationUserRequest;
import com.gestionstock.iam.presentation.dto.OrganisationUserResponse;
import com.gestionstock.iam.presentation.dto.ResetOrganisationUserPasswordRequest;
import com.gestionstock.iam.presentation.dto.UpdateOrganisationUserRoleRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/organisation-users")
@RequiredArgsConstructor
public class OrganisationUserController {

    private final OrganisationUserService organisationUserService;

    @GetMapping
    public ResponseEntity<List<OrganisationUserResponse>> listUsers() {
        return ResponseEntity.ok(organisationUserService.listUsers());
    }

    @PostMapping
    public ResponseEntity<OrganisationUserResponse> createUser(@Valid @RequestBody CreateOrganisationUserRequest request) {
        return ResponseEntity.ok(organisationUserService.createUser(request));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<OrganisationUserResponse> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrganisationUserRoleRequest request
    ) {
        return ResponseEntity.ok(organisationUserService.updateRole(id, request));
    }

    @PatchMapping("/{id}/enabled")
    public ResponseEntity<OrganisationUserResponse> setEnabled(
            @PathVariable Long id,
            @RequestParam boolean enabled
    ) {
        return ResponseEntity.ok(organisationUserService.setEnabled(id, enabled));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<OrganisationUserResponse> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody ResetOrganisationUserPasswordRequest request
    ) {
        return ResponseEntity.ok(organisationUserService.resetPassword(id, request));
    }
}
