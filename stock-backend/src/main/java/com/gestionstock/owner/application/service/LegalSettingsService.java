package com.gestionstock.owner.application.service;

import com.gestionstock.iam.infrastructure.entity.Role;
import com.gestionstock.iam.infrastructure.entity.User;
import com.gestionstock.owner.application.dto.LegalSettingsRequest;
import com.gestionstock.owner.application.dto.LegalSettingsResponse;
import com.gestionstock.owner.infrastructure.entity.LegalSettingsEntity;
import com.gestionstock.owner.infrastructure.repository.LegalSettingsRepository;
import com.gestionstock.security.AuthenticatedUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LegalSettingsService {

    private static final String DEFAULT_COMPANY_NAME = "StockPilot AI";

    private final LegalSettingsRepository legalSettingsRepository;
    private final AuthenticatedUserProvider userProvider;

    @Transactional(readOnly = true)
    public LegalSettingsResponse getSettings() {
        requireOwner();
        return legalSettingsRepository.findAll()
                .stream()
                .findFirst()
                .map(this::toResponse)
                .orElseGet(this::defaultResponse);
    }

    @Transactional
    public LegalSettingsResponse updateSettings(LegalSettingsRequest request) {
        User owner = requireOwner();
        LegalSettingsEntity settings = legalSettingsRepository.findAll()
                .stream()
                .findFirst()
                .orElseGet(() -> LegalSettingsEntity.builder().build());

        settings.setCompanyName(request.companyName().trim());
        settings.setLegalNotice(blankToNull(request.legalNotice()));
        settings.setPrivacyPolicy(blankToNull(request.privacyPolicy()));
        settings.setTerms(blankToNull(request.terms()));
        settings.setLegalDocumentUrl(blankToNull(request.legalDocumentUrl()));
        settings.setPrivacyDocumentUrl(blankToNull(request.privacyDocumentUrl()));
        settings.setTermsDocumentUrl(blankToNull(request.termsDocumentUrl()));
        settings.setUpdatedAt(LocalDateTime.now());
        settings.setUpdatedBy(owner);

        return toResponse(legalSettingsRepository.save(settings));
    }

    LegalSettingsResponse currentSettingsForOwnerDashboard() {
        return legalSettingsRepository.findAll()
                .stream()
                .findFirst()
                .map(this::toResponse)
                .orElseGet(this::defaultResponse);
    }

    private User requireOwner() {
        User user = userProvider.requireUser();
        if (user.getRole() != Role.OWNER) {
            throw new AccessDeniedException("Only owner can access SaaS legal settings");
        }
        return user;
    }

    private LegalSettingsResponse toResponse(LegalSettingsEntity entity) {
        return new LegalSettingsResponse(
                entity.getId(),
                entity.getCompanyName(),
                entity.getLegalNotice(),
                entity.getPrivacyPolicy(),
                entity.getTerms(),
                entity.getLegalDocumentUrl(),
                entity.getPrivacyDocumentUrl(),
                entity.getTermsDocumentUrl(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy() == null ? null : entity.getUpdatedBy().getEmail()
        );
    }

    private LegalSettingsResponse defaultResponse() {
        return new LegalSettingsResponse(
                null,
                DEFAULT_COMPANY_NAME,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
