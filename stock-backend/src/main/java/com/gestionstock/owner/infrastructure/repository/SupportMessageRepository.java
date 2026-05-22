package com.gestionstock.owner.infrastructure.repository;

import com.gestionstock.owner.infrastructure.entity.SupportMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportMessageRepository extends JpaRepository<SupportMessageEntity, Long> {

    List<SupportMessageEntity> findByOrganisationIdOrderByCreatedAtDesc(Long organisationId);

    List<SupportMessageEntity> findAllByOrderByCreatedAtDesc();
}
