package org.example.ggbot.adapter.web.dto;

public record AgentTaskAcceptedResponse(
        String taskId,
        String sessionId,
        String status
) {
}
