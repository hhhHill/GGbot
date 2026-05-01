package org.example.ggbot.service.conversation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.example.ggbot.common.IdGenerator;
import org.example.ggbot.enums.ConversationStatus;
import org.example.ggbot.enums.MessageRole;
import org.example.ggbot.persistence.entity.ConversationEntity;
import org.example.ggbot.persistence.entity.MessageEntity;
import org.example.ggbot.persistence.entity.SubjectEntity;
import org.example.ggbot.persistence.repository.ConversationRepository;
import org.example.ggbot.persistence.repository.MessageRepository;
import org.example.ggbot.persistence.repository.SubjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private IdGenerator idGenerator;

    @InjectMocks
    private ConversationService conversationService;

    @Test
    void shouldCreateConversationUnderMatchingOrgAndSubject() {
        when(subjectRepository.findByIdAndOrgId(5001L, 1001L)).thenReturn(Optional.of(
                SubjectEntity.builder().id(5001L).orgId(1001L).build()));
        when(idGenerator.nextLongId()).thenReturn(7001L);
        when(conversationRepository.save(any(ConversationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConversationEntity created = conversationService.createConversation(1001L, 5001L, "web", "Hello", 3001L);

        assertThat(created.getId()).isEqualTo(7001L);
        assertThat(created.getOrgId()).isEqualTo(1001L);
        assertThat(created.getSubjectId()).isEqualTo(5001L);
        assertThat(created.getStatus()).isEqualTo(ConversationStatus.ACTIVE);
    }

    @Test
    void shouldListAccessibleConversationsForOwnSubjectAndActiveGroups() {
        when(conversationRepository.findAccessibleConversations(3001L, "3001", 1001L))
                .thenReturn(List.of(ConversationEntity.builder().id(7001L).orgId(1001L).build()));

        List<ConversationEntity> conversations = conversationService.listAccessibleConversations(3001L, 1001L);

        assertThat(conversations).extracting(ConversationEntity::getId).containsExactly(7001L);
    }

    @Test
    void shouldPersistMessageAndTouchConversationTimestamp() {
        ConversationEntity conversation = ConversationEntity.builder()
                .id(7001L)
                .orgId(1001L)
                .subjectId(5001L)
                .status(ConversationStatus.ACTIVE)
                .lastMessageAt(LocalDateTime.of(2026, 5, 1, 12, 0))
                .build();
        when(conversationRepository.findByIdAndOrgId(7001L, 1001L)).thenReturn(Optional.of(conversation));
        when(idGenerator.nextLongId()).thenReturn(8001L);
        when(messageRepository.save(any(MessageEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        conversationService.addMessage(1001L, 7001L, 3001L, MessageRole.USER, "hello", "text", "msg-1");

        ArgumentCaptor<MessageEntity> messageCaptor = ArgumentCaptor.forClass(MessageEntity.class);
        verify(messageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getId()).isEqualTo(8001L);
        assertThat(messageCaptor.getValue().getConversationId()).isEqualTo(7001L);
        assertThat(messageCaptor.getValue().getOrgId()).isEqualTo(1001L);
        assertThat(messageCaptor.getValue().getRole()).isEqualTo(MessageRole.USER);

        verify(conversationRepository).save(any(ConversationEntity.class));
        assertThat(conversation.getLastMessageAt()).isNotNull();
    }
}
