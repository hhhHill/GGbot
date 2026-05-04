package org.example.ggbot.adapter.web.dto;

import java.time.LocalDateTime;

public record ConversationSummaryResponse(
        Long conversationId,
        Long orgId,
        Long subjectId,
        String title,
        String source,
        String status,
        LocalDateTime lastMessageAt
) {
}
