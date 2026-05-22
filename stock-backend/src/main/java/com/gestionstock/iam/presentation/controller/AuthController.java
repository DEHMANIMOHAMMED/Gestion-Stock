package com.gestionstock.iam.presentation.controller;

import com.gestionstock.iam.application.service.AuthService;
import com.gestionstock.iam.presentation.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me() {
        return ResponseEntity.ok(authService.me());
    }

    @GetMapping("/organisation-profile")
    public ResponseEntity<OrganisationProfileResponse> organisationProfile() {
        return ResponseEntity.ok(authService.organisationProfile());
    }

    @PutMapping("/organisation-profile")
    public ResponseEntity<OrganisationProfileResponse> updateOrganisationProfile(
            @Valid @RequestBody OrganisationProfileRequest request
    ) {
        return ResponseEntity.ok(authService.updateOrganisationProfile(request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(authService.loginWithGoogle(request.idToken(), request.planCode()));
    }


}
