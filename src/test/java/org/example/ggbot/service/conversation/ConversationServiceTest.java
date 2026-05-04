package org.example.ggbot.service.conversation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(OutputCaptureExtension.class)
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
    void shouldCreateConversationUnderMatchingOrgAndSubject(CapturedOutput output) {
        when(subjectRepository.findByIdAndOrgId(5001L, 1001L)).thenReturn(Optional.of(
                SubjectEntity.builder().id(5001L).orgId(1001L).build()));
        when(idGenerator.nextLongId()).thenReturn(7001L);
        when(conversationRepository.save(any(ConversationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConversationEntity created = conversationService.createConversation(1001L, 5001L, "web", "Hello", 3001L);

        assertThat(created.getId()).isEqualTo(7001L);
        assertThat(created.getOrgId()).isEqualTo(1001L);
        assertThat(created.getSubjectId()).isEqualTo(5001L);
        assertThat(created.getStatus()).isEqualTo(ConversationStatus.ACTIVE);
        assertThat(output).contains("会话已创建");
        assertThat(output).contains("conversationId=7001");
    }

    @Test
    void shouldListAccessibleConversationsForOwnSubjectAndActiveGroups() {
        when(conversationRepository.findAccessibleConversations(3001L, "3001", 1001L))
                .thenReturn(List.of(ConversationEntity.builder().id(7001L).orgId(1001L).build()));

        List<ConversationEntity> conversations = conversationService.listAccessibleConversations(3001L, 1001L);

        assertThat(conversations).extracting(ConversationEntity::getId).containsExactly(7001L);
    }

    @Test
    void shouldPersistMessageAndTouchConversationTimestamp(CapturedOutput output) {
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
        assertThat(output).contains("消息已入库");
        assertThat(output).contains("messageId=8001");
        assertThat(output).contains("conversationId=7001");
    }

    @Test
    void shouldRenameConversationAndUpdateTimestamp(CapturedOutput output) {
        ConversationEntity conversation = ConversationEntity.builder()
                .id(7001L)
                .orgId(1001L)
                .title("旧标题")
                .updatedAt(LocalDateTime.of(2026, 5, 1, 12, 0))
                .build();
        when(conversationRepository.findByIdAndOrgId(7001L, 1001L)).thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(ConversationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConversationEntity renamed = conversationService.renameConversation(1001L, 7001L, "新标题");

        assertThat(renamed.getTitle()).isEqualTo("新标题");
        assertThat(renamed.getUpdatedAt()).isNotNull();
        verify(conversationRepository).save(conversation);
        assertThat(output).contains("会话已重命名");
        assertThat(output).contains("conversationId=7001");
    }

    @Test
    void shouldDeleteConversationAndItsMessages(CapturedOutput output) {
        ConversationEntity conversation = ConversationEntity.builder()
                .id(7001L)
                .orgId(1001L)
                .title("待删除会话")
                .build();
        when(conversationRepository.findByIdAndOrgId(7001L, 1001L)).thenReturn(Optional.of(conversation));

        conversationService.deleteConversation(1001L, 7001L);

        verify(messageRepository).deleteByOrgIdAndConversationId(1001L, 7001L);
        verify(conversationRepository).delete(conversation);
        assertThat(output).contains("会话已删除");
        assertThat(output).contains("conversationId=7001");
    }
}
