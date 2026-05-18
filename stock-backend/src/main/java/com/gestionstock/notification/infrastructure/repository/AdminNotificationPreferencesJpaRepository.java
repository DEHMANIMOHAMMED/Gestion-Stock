package com.gestionstock.notification.infrastructure.repository;

import com.gestionstock.notification.infrastructure.entity.AdminNotificationPreferencesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminNotificationPreferencesJpaRepository extends JpaRepository<AdminNotificationPreferencesEntity, Long> {
    Optional<AdminNotificationPreferencesEntity> findByOrganisationId(Long organisationId);
}
