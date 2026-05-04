package org.example.ggbot.adapter.web.dto;

import java.time.Instant;
import org.example.ggbot.agenttask.AgentTaskRecord;

public record AgentTaskResponse(
        String taskId,
        String sessionId,
        String userId,
        String source,
        String input,
        String status,
        String result,
        String errorMessage,
        int retryCount,
        int maxRetry,
        Instant createdAt,
        Instant updatedAt,
        Instant startedAt,
        Instant finishedAt
) {

    public static AgentTaskResponse from(AgentTaskRecord task) {
        return new AgentTaskResponse(
                task.getTaskId(),
                task.getSessionId(),
                task.getUserId(),
                task.getSource(),
                task.getInput(),
                task.getStatus().name(),
                task.getResult(),
                task.getErrorMessage(),
                task.getRetryCount(),
                task.getMaxRetry(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getStartedAt(),
                task.getFinishedAt()
        );
    }
}
