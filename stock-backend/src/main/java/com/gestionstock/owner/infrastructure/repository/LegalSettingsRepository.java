package com.gestionstock.owner.infrastructure.repository;

import com.gestionstock.owner.infrastructure.entity.LegalSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LegalSettingsRepository extends JpaRepository<LegalSettingsEntity, Long> {
}
