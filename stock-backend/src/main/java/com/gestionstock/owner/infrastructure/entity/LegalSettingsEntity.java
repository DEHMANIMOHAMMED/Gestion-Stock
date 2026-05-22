package com.gestionstock.owner.infrastructure.entity;

import com.gestionstock.iam.infrastructure.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "legal_settings")
public class LegalSettingsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", nullable = false, length = 160)
    private String companyName;

    @Column(name = "legal_notice", columnDefinition = "text")
    private String legalNotice;

    @Column(name = "privacy_policy", columnDefinition = "text")
    private String privacyPolicy;

    @Column(columnDefinition = "text")
    private String terms;

    @Column(name = "legal_document_url", length = 500)
    private String legalDocumentUrl;

    @Column(name = "privacy_document_url", length = 500)
    private String privacyDocumentUrl;

    @Column(name = "terms_document_url", length = 500)
    private String termsDocumentUrl;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id")
    private User updatedBy;
}
