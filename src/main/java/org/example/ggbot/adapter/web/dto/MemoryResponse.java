package org.example.ggbot.adapter.web.dto;

import java.time.LocalDateTime;

public record MemoryResponse(
        Long memoryId,
        Long orgId,
        Long subjectId,
        String memoryType,
        String scope,
        String content,
        Long sourceConversationId,
        Long createdByUserId,
        LocalDateTime updatedAt
) {
}
