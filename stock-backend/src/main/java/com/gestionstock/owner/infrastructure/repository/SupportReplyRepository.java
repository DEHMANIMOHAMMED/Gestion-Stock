package com.gestionstock.owner.infrastructure.repository;

import com.gestionstock.owner.infrastructure.entity.SupportReplyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SupportReplyRepository extends JpaRepository<SupportReplyEntity, Long> {

    List<SupportReplyEntity> findBySupportMessageIdOrderByCreatedAtAsc(Long supportMessageId);

    List<SupportReplyEntity> findBySupportMessageIdInOrderByCreatedAtAsc(Collection<Long> supportMessageIds);
}
