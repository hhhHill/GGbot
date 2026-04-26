package org.example.ggbot.adapter.web.dto;

import java.util.List;

public record WebJobStatusResponse(
        String jobId,
        String status,
        String progressMessage,
        int retryCount,
        boolean canRetry,
        String taskId,
        String intentType,
        List<String> artifactSummaries,
        String replyText,
        String fallbackReply
) {
}
