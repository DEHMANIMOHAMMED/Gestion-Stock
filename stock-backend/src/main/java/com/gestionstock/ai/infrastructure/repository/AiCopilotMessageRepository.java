package com.gestionstock.ai.infrastructure.repository;

import com.gestionstock.ai.infrastructure.entity.AiCopilotMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiCopilotMessageRepository extends JpaRepository<AiCopilotMessageEntity, Long> {
    List<AiCopilotMessageEntity> findByOrganisationIdAndConversationIdOrderByCreatedAtAsc(Long organisationId, Long conversationId);
}
