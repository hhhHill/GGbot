package org.example.ggbot.agenttask;

import java.time.Instant;
import lombok.Getter;
import org.example.ggbot.agent.AgentRequest;

/**
 * Agent异步任务记录，包含任务的完整生命周期信息
 * 不可变对象，状态变更通过创建新记录实现
 */
@Getter
public final class AgentTaskRecord {

    /** 任务ID，全局唯一 */
    private final String taskId;
    /** 会话ID */
    private final String sessionId;
    /** 用户ID */
    private final String userId;
    /** 任务来源（WEB/FEISHU等） */
    private final String source;
    /** 用户输入内容 */
    private final String input;
    /** 任务状态 */
    private final AgentTaskStatus status;
    /** 执行结果（成功时不为空） */
    private final String result;
    /** 错误信息（失败时不为空） */
    private final String errorMessage;
    /** 已重试次数 */
    private final int retryCount;
    /** 最大重试次数 */
    private final int maxRetry;
    /** 外部事件ID，用于去重 */
    private final String externalEventId;
    /** 回复目标ID */
    private final String replyTargetId;
    /** 渠道侧消息ID */
    private final String channelMessageId;
    /** 任务创建时间 */
    private final Instant createdAt;
    /** 任务更新时间 */
    private final Instant updatedAt;
    /** 任务开始执行时间 */
    private final Instant startedAt;
    /** 任务完成时间 */
    private final Instant finishedAt;
    /** 原始Agent请求对象 */
    private final AgentRequest request;

    public AgentTaskRecord(
            String taskId,
            String sessionId,
            String userId,
            String source,
            String input,
            AgentTaskStatus status,
            String result,
            String errorMessage,
            int retryCount,
            int maxRetry,
            String externalEventId,
            String replyTargetId,
            String channelMessageId,
            Instant createdAt,
            Instant updatedAt,
            Instant startedAt,
            Instant finishedAt,
            AgentRequest request
    ) {
        this.taskId = taskId;
        this.sessionId = sessionId;
        this.userId = userId;
        this.source = source;
        this.input = input;
        this.status = status;
        this.result = result;
        this.errorMessage = errorMessage;
        this.retryCount = retryCount;
        this.maxRetry = maxRetry;
        this.externalEventId = externalEventId;
        this.replyTargetId = replyTargetId;
        this.channelMessageId = channelMessageId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.request = request;
    }

    public AgentTaskRecord markRunning(Instant now) {
        return new AgentTaskRecord(
                taskId, sessionId, userId, source, input, AgentTaskStatus.RUNNING, result, null,
                retryCount, maxRetry, externalEventId, replyTargetId, channelMessageId,
                createdAt, now, startedAt == null ? now : startedAt, null, request
        );
    }

    public AgentTaskRecord markRetrying(int nextRetryCount, String nextErrorMessage, Instant now) {
        return new AgentTaskRecord(
                taskId, sessionId, userId, source, input, AgentTaskStatus.RETRYING, null, nextErrorMessage,
                nextRetryCount, maxRetry, externalEventId, replyTargetId, channelMessageId,
                createdAt, now, startedAt == null ? now : startedAt, null, request
        );
    }

    public AgentTaskRecord markSuccess(String nextResult, Instant now) {
        return new AgentTaskRecord(
                taskId, sessionId, userId, source, input, AgentTaskStatus.SUCCESS, nextResult, null,
                retryCount, maxRetry, externalEventId, replyTargetId, channelMessageId,
                createdAt, now, startedAt == null ? now : startedAt, now, request
        );
    }

    public AgentTaskRecord markFailed(String nextErrorMessage, Instant now) {
        return new AgentTaskRecord(
                taskId, sessionId, userId, source, input, AgentTaskStatus.FAILED, null, nextErrorMessage,
                retryCount, maxRetry, externalEventId, replyTargetId, channelMessageId,
                createdAt, now, startedAt == null ? now : startedAt, now, request
        );
    }

    public AgentTaskRecord resetForManualRetry(Instant now) {
        return new AgentTaskRecord(
                taskId, sessionId, userId, source, input, AgentTaskStatus.PENDING, null, null,
                0, maxRetry, externalEventId, replyTargetId, channelMessageId,
                createdAt, now, null, null, request
        );
    }
}
