package com.gestionstock.owner.application.service;

import com.gestionstock.iam.infrastructure.entity.Organisation;
import com.gestionstock.iam.infrastructure.entity.User;
import com.gestionstock.iam.infrastructure.repository.OrganisationRepository;
import com.gestionstock.owner.application.dto.SupportMessageRequest;
import com.gestionstock.owner.application.dto.SupportMessageResponse;
import com.gestionstock.owner.application.dto.SupportReplyRequest;
import com.gestionstock.owner.application.dto.SupportReplyResponse;
import com.gestionstock.owner.infrastructure.entity.SupportMessageEntity;
import com.gestionstock.owner.infrastructure.entity.SupportReplyEntity;
import com.gestionstock.owner.infrastructure.repository.SupportMessageRepository;
import com.gestionstock.owner.infrastructure.repository.SupportReplyRepository;
import com.gestionstock.security.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupportMessageService {

    private final SupportMessageRepository supportMessageRepository;
    private final SupportReplyRepository supportReplyRepository;
    private final OrganisationRepository organisationRepository;
    private final PermissionService permissionService;

    @Transactional
    public SupportMessageResponse create(SupportMessageRequest request) {
        User user = permissionService.requireAuthenticatedTenantUser();
        SupportMessageEntity message = new SupportMessageEntity();
        message.setOrganisationId(user.getOrganisation().getId());
        message.setUserId(user.getId());
        message.setSenderEmail(user.getEmail());
        message.setSubject(request.subject().trim());
        message.setMessage(request.message().trim());
        copyAttachment(request.attachmentName(), request.attachmentContentType(), request.attachmentData(), message);
        message.setStatus("OPEN");
        return toResponse(supportMessageRepository.save(message), Map.of(user.getOrganisation().getId(), user.getOrganisation()));
    }

    @Transactional(readOnly = true)
    public List<SupportMessageResponse> listForCurrentOrganisation() {
        User user = permissionService.requireAuthenticatedTenantUser();
        Map<Long, Organisation> organisations = Map.of(user.getOrganisation().getId(), user.getOrganisation());
        Map<Long, List<SupportReplyEntity>> replies = repliesByMessageId(
                supportMessageRepository.findByOrganisationIdOrderByCreatedAtDesc(user.getOrganisation().getId())
                        .stream()
                        .map(SupportMessageEntity::getId)
                        .toList()
        );
        return supportMessageRepository.findByOrganisationIdOrderByCreatedAtDesc(user.getOrganisation().getId())
                .stream()
                .map(message -> toResponse(message, organisations, replies.getOrDefault(message.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SupportMessageResponse> listForOwner() {
        permissionService.requireOwner();
        List<SupportMessageEntity> messages = supportMessageRepository.findAllByOrderByCreatedAtDesc();
        Map<Long, Organisation> organisations = organisationRepository.findAll()
                .stream()
                .collect(Collectors.toMap(Organisation::getId, organisation -> organisation));
        Map<Long, List<SupportReplyEntity>> replies = repliesByMessageId(messages.stream().map(SupportMessageEntity::getId).toList());
        return messages.stream()
                .map(message -> toResponse(message, organisations, replies.getOrDefault(message.getId(), List.of())))
                .toList();
    }

    @Transactional
    public SupportMessageResponse replyAsTenant(Long id, SupportReplyRequest request) {
        User user = permissionService.requireAuthenticatedTenantUser();
        SupportMessageEntity message = supportMessageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Support message not found"));
        if (!message.getOrganisationId().equals(user.getOrganisation().getId())) {
            throw new AccessDeniedException("Support message belongs to another organisation");
        }
        saveReply(message, user, request);
        message.setStatus("OPEN");
        message.setResolvedAt(null);
        return toResponse(supportMessageRepository.save(message), Map.of(user.getOrganisation().getId(), user.getOrganisation()));
    }

    @Transactional
    public SupportMessageResponse replyAsOwner(Long id, SupportReplyRequest request) {
        User owner = permissionService.requireOwner();
        SupportMessageEntity message = supportMessageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Support message not found"));
        saveReply(message, owner, request);
        if (message.getReadAt() == null) {
            message.setReadAt(LocalDateTime.now());
        }
        if ("OPEN".equals(message.getStatus())) {
            message.setStatus("READ");
        }
        return toResponse(supportMessageRepository.save(message), organisationMap(message.getOrganisationId()));
    }

    @Transactional
    public SupportMessageResponse markRead(Long id) {
        permissionService.requireOwner();
        SupportMessageEntity message = supportMessageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Support message not found"));
        if (message.getReadAt() == null) {
            message.setReadAt(LocalDateTime.now());
        }
        if ("OPEN".equals(message.getStatus())) {
            message.setStatus("READ");
        }
        return toResponse(supportMessageRepository.save(message), organisationMap(message.getOrganisationId()));
    }

    @Transactional
    public SupportMessageResponse resolve(Long id) {
        permissionService.requireOwner();
        SupportMessageEntity message = supportMessageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Support message not found"));
        LocalDateTime now = LocalDateTime.now();
        if (message.getReadAt() == null) {
            message.setReadAt(now);
        }
        message.setResolvedAt(now);
        message.setStatus("RESOLVED");
        return toResponse(supportMessageRepository.save(message), organisationMap(message.getOrganisationId()));
    }

    private Map<Long, Organisation> organisationMap(Long organisationId) {
        return organisationRepository.findById(organisationId)
                .map(organisation -> Map.of(organisation.getId(), organisation))
                .orElseGet(Map::of);
    }

    private void saveReply(SupportMessageEntity message, User user, SupportReplyRequest request) {
        SupportReplyEntity reply = new SupportReplyEntity();
        reply.setSupportMessageId(message.getId());
        reply.setAuthorUserId(user.getId());
        reply.setAuthorEmail(user.getEmail());
        reply.setAuthorRole(user.getRole().name());
        reply.setMessage(request.message().trim());
        copyAttachment(request.attachmentName(), request.attachmentContentType(), request.attachmentData(), reply);
        supportReplyRepository.save(reply);
    }

    private void copyAttachment(String name, String contentType, String data, SupportMessageEntity message) {
        if (data == null || data.isBlank()) {
            return;
        }
        validateImageAttachment(contentType, data);
        message.setAttachmentName(blankToNull(name));
        message.setAttachmentContentType(blankToNull(contentType));
        message.setAttachmentData(data);
    }

    private void copyAttachment(String name, String contentType, String data, SupportReplyEntity reply) {
        if (data == null || data.isBlank()) {
            return;
        }
        validateImageAttachment(contentType, data);
        reply.setAttachmentName(blankToNull(name));
        reply.setAttachmentContentType(blankToNull(contentType));
        reply.setAttachmentData(data);
    }

    private void validateImageAttachment(String contentType, String data) {
        String normalizedType = contentType == null ? "" : contentType.toLowerCase();
        if (!normalizedType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image attachments are allowed");
        }
        if (data.length() > 3_000_000) {
            throw new IllegalArgumentException("Attachment is too large");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Map<Long, List<SupportReplyEntity>> repliesByMessageId(List<Long> messageIds) {
        if (messageIds.isEmpty()) {
            return Map.of();
        }
        return supportReplyRepository.findBySupportMessageIdInOrderByCreatedAtAsc(messageIds)
                .stream()
                .collect(Collectors.groupingBy(SupportReplyEntity::getSupportMessageId));
    }

    private SupportMessageResponse toResponse(SupportMessageEntity message, Map<Long, Organisation> organisations) {
        return toResponse(message, organisations, supportReplyRepository.findBySupportMessageIdOrderByCreatedAtAsc(message.getId()));
    }

    private SupportMessageResponse toResponse(SupportMessageEntity message, Map<Long, Organisation> organisations, List<SupportReplyEntity> replies) {
        Organisation organisation = organisations.get(message.getOrganisationId());
        return new SupportMessageResponse(
                message.getId(),
                message.getOrganisationId(),
                organisation == null ? "Organisation inconnue" : organisation.getName(),
                message.getUserId(),
                message.getSenderEmail(),
                message.getSubject(),
                message.getMessage(),
                message.getAttachmentName(),
                message.getAttachmentContentType(),
                message.getAttachmentData(),
                message.getStatus(),
                message.getCreatedAt(),
                message.getReadAt(),
                message.getResolvedAt(),
                replies.stream().map(this::toReplyResponse).toList()
        );
    }

    private SupportReplyResponse toReplyResponse(SupportReplyEntity reply) {
        return new SupportReplyResponse(
                reply.getId(),
                reply.getAuthorUserId(),
                reply.getAuthorEmail(),
                reply.getAuthorRole(),
                reply.getMessage(),
                reply.getAttachmentName(),
                reply.getAttachmentContentType(),
                reply.getAttachmentData(),
                reply.getCreatedAt()
        );
    }
}
