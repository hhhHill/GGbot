package org.example.ggbot.agenttask;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.agent.AgentRequest;
import org.example.ggbot.common.IdGenerator;
import org.springframework.stereotype.Service;

/**
 * 内存版Agent任务服务实现
 * 基于ConcurrentHashMap存储任务，重启后数据丢失，适合开发和测试环境
 */
@Service
@RequiredArgsConstructor
public class InMemoryAgentTaskService implements AgentTaskService {

    /** ID生成器 */
    private final IdGenerator idGenerator;
    /** 任务存储，key为taskId */
    private final ConcurrentMap<String, AgentTaskRecord> tasks = new ConcurrentHashMap<>();
    /** 外部事件ID索引，用于去重，key为externalEventId，value为taskId */
    private final ConcurrentMap<String, String> externalEventIndex = new ConcurrentHashMap<>();

    @Override
    public synchronized AgentTaskRecord createTask(AgentRequest request, String source, String externalEventId) {
        return create(request, source, externalEventId);
    }

    @Override
    public synchronized AgentTaskCreationResult createOrGetByExternalEventId(AgentRequest request, String source, String externalEventId) {
        if (externalEventId == null || externalEventId.isBlank()) {
            return new AgentTaskCreationResult(create(request, source, null), true);
        }
        String existingTaskId = externalEventIndex.get(externalEventId);
        if (existingTaskId != null) {
            return new AgentTaskCreationResult(findByTaskId(existingTaskId), false);
        }
        AgentTaskRecord created = create(request, source, externalEventId);
        externalEventIndex.putIfAbsent(externalEventId, created.getTaskId());
        String indexedTaskId = externalEventIndex.get(externalEventId);
        return created.getTaskId().equals(indexedTaskId)
                ? new AgentTaskCreationResult(created, true)
                : new AgentTaskCreationResult(findByTaskId(indexedTaskId), false);
    }

    @Override
    public AgentTaskRecord findByTaskId(String taskId) {
        AgentTaskRecord task = tasks.get(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Agent task not found: " + taskId);
        }
        return task;
    }

    @Override
    public AgentTaskRecord markRunning(String taskId) {
        return replace(taskId, record -> record.markRunning(Instant.now()));
    }

    @Override
    public AgentTaskRecord markSuccess(String taskId, String result) {
        return replace(taskId, record -> record.markSuccess(result, Instant.now()));
    }

    @Override
    public AgentTaskRecord markFailed(String taskId, String errorMessage) {
        return replace(taskId, record -> record.markFailed(errorMessage, Instant.now()));
    }

    @Override
    public AgentTaskRecord markCancelled(String taskId, String errorMessage) {
        return replace(taskId, record -> new AgentTaskRecord(
                record.getTaskId(),
                record.getSessionId(),
                record.getUserId(),
                record.getSource(),
                record.getInput(),
                AgentTaskStatus.CANCELLED,
                null,
                errorMessage,
                record.getRetryCount(),
                record.getMaxRetry(),
                record.getExternalEventId(),
                record.getReplyTargetId(),
                record.getChannelMessageId(),
                record.getCreatedAt(),
                Instant.now(),
                record.getStartedAt() == null ? Instant.now() : record.getStartedAt(),
                Instant.now(),
                record.getRequest()
        ));
    }

    @Override
    public AgentTaskRecord markRetrying(String taskId, int retryCount, String errorMessage) {
        return replace(taskId, record -> record.markRetrying(retryCount, errorMessage, Instant.now()));
    }

    @Override
    public AgentTaskRecord retry(String taskId) {
        AgentTaskRecord current = findByTaskId(taskId);
        if (current.getStatus() != AgentTaskStatus.FAILED) {
            throw new IllegalStateException("Only failed tasks can retry: " + taskId);
        }
        return replace(taskId, record -> record.resetForManualRetry(Instant.now()));
    }

    private AgentTaskRecord create(AgentRequest request, String source, String externalEventId) {
        Instant now = Instant.now();
        AgentTaskRecord task = new AgentTaskRecord(
                idGenerator.nextId("agent-task"),
                request.getConversationId(),
                request.getUserId(),
                source,
                request.getUserInput(),
                AgentTaskStatus.PENDING,
                null,
                null,
                0,
                3,
                externalEventId,
                request.getReplyTargetId(),
                request.getChannelMessageId(),
                now,
                now,
                null,
                null,
                request
        );
        tasks.put(task.getTaskId(), task);
        return task;
    }

    private AgentTaskRecord replace(String taskId, java.util.function.UnaryOperator<AgentTaskRecord> updater) {
        AgentTaskRecord updated = tasks.computeIfPresent(taskId, (ignored, current) -> updater.apply(current));
        if (updated == null) {
            throw new IllegalArgumentException("Agent task not found: " + taskId);
        }
        return updated;
    }
}
