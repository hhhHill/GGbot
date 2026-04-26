package org.example.ggbot.job;

import java.time.Instant;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public final class JobRecord {

    private final String jobId;
    private final String conversationId;
    private final String userId;
    private final String originalRequestPayload;
    private final JobStatus status;
    private final String progressMessage;
    private final int retryCount;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant lastProgressAt;
    private final String resultPayload;
    private final String errorMessage;
    private final String fallbackReply;

    private JobRecord(
            String jobId,
            String conversationId,
            String userId,
            String originalRequestPayload,
            JobStatus status,
            String progressMessage,
            int retryCount,
            Instant createdAt,
            Instant updatedAt,
            Instant lastProgressAt,
            String resultPayload,
            String errorMessage,
            String fallbackReply
    ) {
        this.jobId = jobId;
        this.conversationId = conversationId;
        this.userId = userId;
        this.originalRequestPayload = originalRequestPayload;
        this.status = status;
        this.progressMessage = progressMessage;
        this.retryCount = retryCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastProgressAt = lastProgressAt;
        this.resultPayload = resultPayload;
        this.errorMessage = errorMessage;
        this.fallbackReply = fallbackReply;
    }

    public static JobRecord queued(String jobId, String conversationId, String userId, String originalRequestPayload, Instant now) {
        return new JobRecord(
                jobId,
                conversationId,
                userId,
                originalRequestPayload,
                JobStatus.QUEUED,
                "已接收请求",
                0,
                now,
                now,
                now,
                null,
                null,
                null
        );
    }

    public JobRecord markRunning(String nextProgressMessage, Instant now) {
        return new JobRecord(
                jobId,
                conversationId,
                userId,
                originalRequestPayload,
                JobStatus.RUNNING,
                nextProgressMessage,
                retryCount,
                createdAt,
                now,
                now,
                resultPayload,
                errorMessage,
                fallbackReply
        );
    }

    public JobRecord markSucceeded(String nextResultPayload, Instant now) {
        return new JobRecord(
                jobId,
                conversationId,
                userId,
                originalRequestPayload,
                JobStatus.SUCCEEDED,
                progressMessage,
                retryCount,
                createdAt,
                now,
                now,
                nextResultPayload,
                null,
                null
        );
    }

    public JobRecord markFailed(String nextErrorMessage, String nextFallbackReply, Instant now) {
        return new JobRecord(
                jobId,
                conversationId,
                userId,
                originalRequestPayload,
                JobStatus.FAILED,
                progressMessage,
                retryCount,
                createdAt,
                now,
                lastProgressAt,
                null,
                nextErrorMessage,
                nextFallbackReply
        );
    }

    public JobRecord markTimeout(String nextFallbackReply, Instant now) {
        return new JobRecord(
                jobId,
                conversationId,
                userId,
                originalRequestPayload,
                JobStatus.TIMEOUT,
                progressMessage,
                retryCount,
                createdAt,
                now,
                lastProgressAt,
                null,
                errorMessage,
                nextFallbackReply
        );
    }

    public JobRecord retryAs(String nextJobId, Instant now) {
        return new JobRecord(
                nextJobId,
                conversationId,
                userId,
                originalRequestPayload,
                JobStatus.QUEUED,
                "已接收请求",
                retryCount + 1,
                now,
                now,
                now,
                null,
                null,
                null
        );
    }
}
