package com.gestionstock.ai.infrastructure.repository;

import com.gestionstock.ai.infrastructure.entity.AiCopilotConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiCopilotConversationRepository extends JpaRepository<AiCopilotConversationEntity, Long> {
    List<AiCopilotConversationEntity> findTop20ByOrganisationIdAndUserIdOrderByUpdatedAtDesc(Long organisationId, Long userId);

    Optional<AiCopilotConversationEntity> findByIdAndOrganisationIdAndUserId(Long id, Long organisationId, Long userId);
}
