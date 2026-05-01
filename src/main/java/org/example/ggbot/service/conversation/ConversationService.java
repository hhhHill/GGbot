package org.example.ggbot.service.conversation;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
        return conversationRepository.save(conversation);
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
        return saved;
    }

    public List<ConversationEntity> listAccessibleConversations(Long userId, Long orgId) {
        return conversationRepository.findAccessibleConversations(userId, String.valueOf(userId), orgId);
    }

    public List<MessageEntity> listMessages(Long orgId, Long conversationId) {
        return messageRepository.findByOrgIdAndConversationIdOrderByCreatedAtAsc(orgId, conversationId);
    }
}
