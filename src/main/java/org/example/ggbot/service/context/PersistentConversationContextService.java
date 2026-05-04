package org.example.ggbot.service.context;

import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.enums.MemoryScope;
import org.example.ggbot.persistence.entity.MessageEntity;
import org.example.ggbot.persistence.repository.MemoryRepository;
import org.example.ggbot.persistence.repository.MessageRepository;
import org.example.ggbot.service.dto.ConversationContext;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PersistentConversationContextService {

    private final MessageRepository messageRepository;
    private final MemoryRepository memoryRepository;

    public ConversationContext buildContext(Long orgId, Long subjectId, Long conversationId) {
        List<String> history = messageRepository.findTop20ByOrgIdAndConversationIdOrderByCreatedAtDesc(orgId, conversationId)
                .stream()
                .sorted(Comparator.comparing(MessageEntity::getCreatedAt))
                .map(message -> message.getRole().name() + ": " + message.getContent())
                .toList();
        List<String> globalMemory = memoryRepository.findByOrgIdAndSubjectIdAndScopeOrderByUpdatedAtDesc(
                        orgId, subjectId, MemoryScope.GLOBAL)
                .stream()
                .map(memory -> memory.getContent())
                .toList();
        return new ConversationContext(orgId, subjectId, conversationId, history, globalMemory);
    }
}
