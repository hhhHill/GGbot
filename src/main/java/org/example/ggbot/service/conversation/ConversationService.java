package org.example.ggbot.service.conversation;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ggbot.common.IdGenerator;
import org.example.ggbot.enums.ConversationStatus;
import org.example.ggbot.enums.MessageRole;
import org.example.ggbot.exception.BadRequestException;
import org.example.ggbot.exception.NotFoundException;
import org.example.ggbot.persistence.entity.ConversationEntity;
import org.example.ggbot.persistence.entity.MessageEntity;
import org.example.ggbot.persistence.entity.SubjectEntity;
import org.example.ggbot.persistence.repository.ConversationRepository;
import org.example.ggbot.persistence.repository.MessageRepository;
import org.example.ggbot.persistence.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final SubjectRepository subjectRepository;
    private final IdGenerator idGenerator;

    @Transactional
    public ConversationEntity createConversation(Long orgId, Long subjectId, String source, String title, Long createdByUserId) {
        SubjectEntity subject = subjectRepository.findByIdAndOrgId(subjectId, orgId)
                .orElseThrow(() -> new NotFoundException("Subject not found"));
        if (!orgId.equals(subject.getOrgId())) {
            throw new BadRequestException("Subject does not belong to org");
        }
        ConversationEntity conversation = ConversationEntity.builder()
                .id(idGenerator.nextLongId())
                .orgId(orgId)
                .subjectId(subjectId)
                .title(title)
                .source(source)
                .status(ConversationStatus.ACTIVE)
                .createdByUserId(createdByUserId)
                .lastMessageAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        ConversationEntity saved = conversationRepository.save(conversation);
        log.info(
                "会话已创建: conversationId={}, orgId={}, subjectId={}, source={}, createdByUserId={}",
                saved.getId(),
                saved.getOrgId(),
                saved.getSubjectId(),
                saved.getSource(),
                saved.getCreatedByUserId()
        );
        return saved;
    }

    @Transactional
    public ConversationEntity getOrCreateActiveConversation(Long orgId, Long subjectId, String source, Long createdByUserId) {
        return conversationRepository.findByOrgIdAndSubjectIdOrderByLastMessageAtDesc(orgId, subjectId).stream()
                .filter(conversation -> conversation.getStatus() == ConversationStatus.ACTIVE)
                .findFirst()
                .orElseGet(() -> createConversation(orgId, subjectId, source, null, createdByUserId));
    }

    @Transactional
    public MessageEntity addMessage(
            Long orgId,
            Long conversationId,
            Long senderUserId,
            MessageRole role,
            String content,
            String messageType,
            String providerMessageId
    ) {
        ConversationEntity conversation = conversationRepository.findByIdAndOrgId(conversationId, orgId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
        MessageEntity message = MessageEntity.builder()
                .id(idGenerator.nextLongId())
                .orgId(orgId)
                .conversationId(conversationId)
                .senderUserId(senderUserId)
                .role(role)
                .content(content)
                .messageType(messageType)
                .providerMessageId(providerMessageId)
                .createdAt(LocalDateTime.now())
                .build();
        MessageEntity saved = messageRepository.save(message);
        conversation.setLastMessageAt(saved.getCreatedAt());
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
        log.info(
                "消息已入库: messageId={}, conversationId={}, orgId={}, role={}, senderUserId={}, messageType={}",
                saved.getId(),
                saved.getConversationId(),
                saved.getOrgId(),
                saved.getRole(),
                saved.getSenderUserId(),
                saved.getMessageType()
        );
        return saved;
    }

    @Transactional
    public ConversationEntity renameConversation(Long orgId, Long conversationId, String title) {
        ConversationEntity conversation = conversationRepository.findByIdAndOrgId(conversationId, orgId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
        conversation.setTitle(title == null ? null : title.trim());
        conversation.setUpdatedAt(LocalDateTime.now());
        ConversationEntity saved = conversationRepository.save(conversation);
        log.info(
                "会话已重命名: conversationId={}, orgId={}, title={}",
                saved.getId(),
                saved.getOrgId(),
                saved.getTitle()
        );
        return saved;
    }

    @Transactional
    public void deleteConversation(Long orgId, Long conversationId) {
        ConversationEntity conversation = conversationRepository.findByIdAndOrgId(conversationId, orgId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
        messageRepository.deleteByOrgIdAndConversationId(orgId, conversationId);
        conversationRepository.delete(conversation);
        log.info(
                "会话已删除: conversationId={}, orgId={}, title={}",
                conversation.getId(),
                conversation.getOrgId(),
                conversation.getTitle()
        );
    }

    public List<ConversationEntity> listAccessibleConversations(Long userId, Long orgId) {
        return conversationRepository.findAccessibleConversations(userId, String.valueOf(userId), orgId);
    }

    public List<MessageEntity> listMessages(Long orgId, Long conversationId) {
        return messageRepository.findByOrgIdAndConversationIdOrderByCreatedAtAsc(orgId, conversationId);
    }
}
