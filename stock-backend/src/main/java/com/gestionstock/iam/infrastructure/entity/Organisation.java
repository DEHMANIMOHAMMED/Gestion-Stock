package com.gestionstock.iam.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted;
}
