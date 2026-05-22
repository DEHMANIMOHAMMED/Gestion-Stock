package com.gestionstock.owner.presentation.controller;

import com.gestionstock.owner.application.dto.LegalSettingsRequest;
import com.gestionstock.owner.application.dto.LegalSettingsResponse;
import com.gestionstock.owner.application.dto.OwnerDashboardResponse;
import com.gestionstock.owner.application.dto.OwnerOrganizationResponse;
import com.gestionstock.owner.application.dto.OwnerSupportPasswordRequest;
import com.gestionstock.owner.application.dto.OwnerSupportSubscriptionResponse;
import com.gestionstock.owner.application.dto.OwnerSupportSubscriptionCancelRequest;
import com.gestionstock.owner.application.dto.OwnerSupportUserResponse;
import com.gestionstock.owner.application.dto.OwnerSupportUserStatusRequest;
import com.gestionstock.owner.application.dto.SupportMessageResponse;
import com.gestionstock.owner.application.dto.SupportReplyRequest;
import com.gestionstock.owner.application.service.LegalSettingsService;
import com.gestionstock.owner.application.service.OwnerSupportAdminService;
import com.gestionstock.owner.application.service.OwnerDashboardService;
import com.gestionstock.owner.application.service.SupportMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/owner")
@RequiredArgsConstructor
public class OwnerController {

    private final OwnerDashboardService ownerDashboardService;
    private final LegalSettingsService legalSettingsService;
    private final SupportMessageService supportMessageService;
    private final OwnerSupportAdminService ownerSupportAdminService;

    @GetMapping("/dashboard")
    public ResponseEntity<OwnerDashboardResponse> dashboard() {
        return ResponseEntity.ok(ownerDashboardService.dashboard());
    }

    @GetMapping("/organizations")
    public ResponseEntity<List<OwnerOrganizationResponse>> organizations() {
        return ResponseEntity.ok(ownerDashboardService.organizations());
    }

    @GetMapping("/legal-settings")
    public ResponseEntity<LegalSettingsResponse> legalSettings() {
        return ResponseEntity.ok(legalSettingsService.getSettings());
    }

    @PutMapping("/legal-settings")
    public ResponseEntity<LegalSettingsResponse> updateLegalSettings(@Valid @RequestBody LegalSettingsRequest request) {
        return ResponseEntity.ok(legalSettingsService.updateSettings(request));
    }

    @GetMapping("/support-messages")
    public ResponseEntity<List<SupportMessageResponse>> supportMessages() {
        return ResponseEntity.ok(supportMessageService.listForOwner());
    }

    @PatchMapping("/support-messages/{id}/read")
    public ResponseEntity<SupportMessageResponse> markSupportMessageRead(@PathVariable Long id) {
        return ResponseEntity.ok(supportMessageService.markRead(id));
    }

    @PatchMapping("/support-messages/{id}/resolve")
    public ResponseEntity<SupportMessageResponse> resolveSupportMessage(@PathVariable Long id) {
        return ResponseEntity.ok(supportMessageService.resolve(id));
    }

    @PostMapping("/support-messages/{id}/replies")
    public ResponseEntity<SupportMessageResponse> replyToSupportMessage(@PathVariable Long id, @Valid @RequestBody SupportReplyRequest request) {
        return ResponseEntity.ok(supportMessageService.replyAsOwner(id, request));
    }

    @GetMapping("/organizations/{organisationId}/users")
    public ResponseEntity<List<OwnerSupportUserResponse>> organisationUsers(@PathVariable Long organisationId) {
        return ResponseEntity.ok(ownerSupportAdminService.users(organisationId));
    }

    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<OwnerSupportUserResponse> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody OwnerSupportUserStatusRequest request
    ) {
        return ResponseEntity.ok(ownerSupportAdminService.updateUserStatus(userId, request.enabled(), request.reason()));
    }

    @PatchMapping("/users/{userId}/password")
    public ResponseEntity<OwnerSupportUserResponse> changeUserPassword(
            @PathVariable Long userId,
            @Valid @RequestBody OwnerSupportPasswordRequest request
    ) {
        return ResponseEntity.ok(ownerSupportAdminService.changePassword(userId, request.newPassword(), request.reason()));
    }

    @PatchMapping("/organizations/{organisationId}/subscription/cancel")
    public ResponseEntity<OwnerSupportSubscriptionResponse> cancelOrganisationSubscription(
            @PathVariable Long organisationId,
            @Valid @RequestBody OwnerSupportSubscriptionCancelRequest request
    ) {
        return ResponseEntity.ok(ownerSupportAdminService.cancelSubscription(organisationId, request.reason()));
    }
}
