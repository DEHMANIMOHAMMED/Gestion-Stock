package com.gestionstock.notification.infrastructure.repository;

import com.gestionstock.notification.infrastructure.entity.AdminNotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminNotificationJpaRepository extends JpaRepository<AdminNotificationEntity, Long> {
    List<AdminNotificationEntity> findTop20ByOrganisationIdOrderByCreatedAtDesc(Long organisationId);

    List<AdminNotificationEntity> findByOrganisationIdOrderByCreatedAtDesc(Long organisationId);

    List<AdminNotificationEntity> findTop20ByOrganisationIdAndReadAtIsNullOrderByCreatedAtDesc(Long organisationId);

    long countByOrganisationIdAndReadAtIsNull(Long organisationId);

    Optional<AdminNotificationEntity> findByOrganisationIdAndDeduplicationKey(Long organisationId, String deduplicationKey);

    Optional<AdminNotificationEntity> findByIdAndOrganisationId(Long id, Long organisationId);
}
