package com.gestionstock.iam.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "organisations")
public class Organisation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(length = 80)
    private String industry;

    @Column(name = "size_range", length = 40)
    private String sizeRange;

    @Column(length = 60)
    private String phone;

    @Column(length = 220)
    private String address;

    @Column(length = 120)
    private String city;

    @Column(length = 120)
    private String country;

    @Column(length = 10)
    private String currency;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "tax_id", length = 80)
    private String taxId;

    @Column(length = 220)
    private String website;

    @Column(name = "stock_alert_email", length = 180)
    private String stockAlertEmail;

    @Column(name = "default_lead_time_days", nullable = false)
    private Integer defaultLeadTimeDays;

    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, length = 20)
    private String status;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }
        if (defaultLeadTimeDays == null) {
            defaultLeadTimeDays = 7;
        }
    }
}
