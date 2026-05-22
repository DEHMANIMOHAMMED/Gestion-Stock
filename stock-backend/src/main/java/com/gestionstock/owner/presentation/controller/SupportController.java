package com.gestionstock.owner.presentation.controller;

import com.gestionstock.owner.application.dto.SupportMessageRequest;
import com.gestionstock.owner.application.dto.SupportMessageResponse;
import com.gestionstock.owner.application.dto.SupportReplyRequest;
import com.gestionstock.owner.application.service.SupportMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/support")
public class SupportController {

    private final SupportMessageService supportMessageService;

    @GetMapping("/messages")
    public ResponseEntity<List<SupportMessageResponse>> messages() {
        return ResponseEntity.ok(supportMessageService.listForCurrentOrganisation());
    }

    @PostMapping("/messages")
    public ResponseEntity<SupportMessageResponse> create(@Valid @RequestBody SupportMessageRequest request) {
        return ResponseEntity.ok(supportMessageService.create(request));
    }

    @PostMapping("/messages/{id}/replies")
    public ResponseEntity<SupportMessageResponse> reply(@PathVariable Long id, @Valid @RequestBody SupportReplyRequest request) {
        return ResponseEntity.ok(supportMessageService.replyAsTenant(id, request));
    }
}
