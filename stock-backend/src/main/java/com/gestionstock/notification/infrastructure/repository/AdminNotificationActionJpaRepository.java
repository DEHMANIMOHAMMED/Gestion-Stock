package com.gestionstock.notification.infrastructure.repository;

import com.gestionstock.notification.infrastructure.entity.AdminNotificationActionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminNotificationActionJpaRepository extends JpaRepository<AdminNotificationActionEntity, Long> {

    List<AdminNotificationActionEntity> findByNotificationIdAndOrganisationIdOrderByCreatedAtDesc(Long notificationId, Long organisationId);
}
