package org.example.ggbot.service.dto;

import java.util.List;

public record ConversationContext(
        Long orgId,
        Long subjectId,
        Long conversationId,
        List<String> history,
        List<String> globalMemory
) {
}
