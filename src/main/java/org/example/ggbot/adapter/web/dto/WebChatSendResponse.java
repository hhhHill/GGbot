package org.example.ggbot.adapter.web.dto;

public record WebChatSendResponse(
        Long orgId,
        Long subjectId,
        Long conversationId,
        String reply
) {
}
