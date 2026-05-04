package org.example.ggbot.adapter.web.dto;

import java.time.LocalDateTime;

public record MessageResponse(
        Long messageId,
        Long conversationId,
        Long senderUserId,
        String role,
        String content,
        String messageType,
        LocalDateTime createdAt
) {
}
