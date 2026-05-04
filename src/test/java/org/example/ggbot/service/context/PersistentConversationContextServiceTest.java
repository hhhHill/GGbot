package org.example.ggbot.service.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import org.example.ggbot.enums.MemoryScope;
import org.example.ggbot.enums.MessageRole;
import org.example.ggbot.persistence.entity.MemoryEntity;
import org.example.ggbot.persistence.entity.MessageEntity;
import org.example.ggbot.persistence.repository.MemoryRepository;
import org.example.ggbot.persistence.repository.MessageRepository;
import org.example.ggbot.service.dto.ConversationContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PersistentConversationContextServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MemoryRepository memoryRepository;

    @InjectMocks
    private PersistentConversationContextService persistentConversationContextService;

    @Test
    void shouldBuildAgentContextFromConversationMessagesAndGlobalSubjectMemory() {
        when(messageRepository.findTop20ByOrgIdAndConversationIdOrderByCreatedAtDesc(1001L, 7001L))
                .thenReturn(List.of(
                        MessageEntity.builder()
                                .role(MessageRole.ASSISTANT)
                                .content("hi")
                                .createdAt(LocalDateTime.of(2026, 5, 1, 12, 2))
                                .build(),
                        MessageEntity.builder()
                                .role(MessageRole.USER)
                                .content("hello")
                                .createdAt(LocalDateTime.of(2026, 5, 1, 12, 1))
                                .build()
                ));
        when(memoryRepository.findByOrgIdAndSubjectIdAndScopeOrderByUpdatedAtDesc(
                1001L, 5001L, MemoryScope.GLOBAL))
                .thenReturn(List.of(
                        MemoryEntity.builder().content("user likes concise answers").build()
                ));

        ConversationContext context = persistentConversationContextService.buildContext(1001L, 5001L, 7001L);

        assertThat(context.history()).containsExactly("USER: hello", "ASSISTANT: hi");
        assertThat(context.globalMemory()).containsExactly("user likes concise answers");
    }
}
