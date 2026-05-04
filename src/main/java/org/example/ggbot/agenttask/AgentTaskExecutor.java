package org.example.ggbot.agenttask;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.bsc.async.AsyncGenerator;
import org.bsc.langgraph4j.NodeOutput;
import org.example.ggbot.adapter.feishu.FeishuInboundMessage;
import org.example.ggbot.adapter.feishu.FeishuMessageClient;
import org.example.ggbot.adapter.feishu.FeishuMessageHandler;
import org.example.ggbot.agent.AgentChannel;
import org.example.ggbot.agent.AgentResult;
import org.example.ggbot.agent.AgentService;
import org.example.ggbot.agent.AgentState;
import org.example.ggbot.agent.graph.AgentGraphProperties;
import org.example.ggbot.agent.runner.LangGraphAgentRunner;
import org.example.ggbot.enums.MessageRole;
import org.example.ggbot.memory.ConversationMemoryService;
import org.example.ggbot.service.conversation.ConversationService;
import org.example.ggbot.session.WebSessionService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Agent任务执行器
 * 负责异步执行Agent任务，支持普通异步执行和流式SSE执行两种模式
 */
@Component
@Slf4j
public class AgentTaskExecutor {

    /** 任务服务 */
    private final AgentTaskService taskService;
    /** Agent服务 */
    private final AgentService agentService;
    /** 会话记忆服务 */
    private final ConversationMemoryService conversationMemoryService;
    /** Web会话服务 */
    private final WebSessionService sessionService;
    /** 会话服务 */
    private final ConversationService conversationService;
    /** 飞书消息客户端 */
    private final FeishuMessageClient feishuMessageClient;
    /** 飞书消息处理器 */
    private final FeishuMessageHandler feishuMessageHandler;
    /** LangGraph Agent执行器 */
    private final LangGraphAgentRunner langGraphAgentRunner;
    /** Agent图配置 */
    private final AgentGraphProperties graphProperties;
    /** 任务执行线程池 */
    private final Executor executor;

    public AgentTaskExecutor(
            AgentTaskService taskService,
            AgentService agentService,
            ConversationMemoryService conversationMemoryService,
            WebSessionService sessionService,
            ConversationService conversationService,
            FeishuMessageClient feishuMessageClient,
            FeishuMessageHandler feishuMessageHandler,
            LangGraphAgentRunner langGraphAgentRunner,
            AgentGraphProperties graphProperties,
            @Qualifier("jobWorkerExecutor") Executor executor) {
        this.taskService = taskService;
        this.agentService = agentService;
        this.conversationMemoryService = conversationMemoryService;
        this.sessionService = sessionService;
        this.conversationService = conversationService;
        this.feishuMessageClient = feishuMessageClient;
        this.feishuMessageHandler = feishuMessageHandler;
        this.langGraphAgentRunner = langGraphAgentRunner;
        this.graphProperties = graphProperties;
        this.executor = executor;
    }

    public void submit(String taskId) {
        executor.execute(() -> process(taskId));
    }

    public void submitStream(String taskId, SseEmitter emitter) {
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<Thread> workerThread = new AtomicReference<>();
        AtomicReference<AsyncGenerator.Cancellable<NodeOutput<org.example.ggbot.agent.graph.GGBotAgentGraphState>>> streamRef =
                new AtomicReference<>();

        Runnable cancelAction = () -> {
            if (completed.get()) {
                return;
            }
            AsyncGenerator.Cancellable<NodeOutput<org.example.ggbot.agent.graph.GGBotAgentGraphState>> stream = streamRef.getAndSet(null);
            if (stream != null) {
                stream.cancel(true);
            }
            Thread thread = workerThread.get();
            if (thread != null) {
                thread.interrupt();
            }
        };

        emitter.onCompletion(cancelAction);
        emitter.onTimeout(cancelAction);
        emitter.onError(throwable -> cancelAction.run());

        executor.execute(() -> {
            workerThread.set(Thread.currentThread());
            try {
                processStream(taskId, emitter, streamRef, completed);
            } finally {
                completed.set(true);
            }
        });
    }

    public void process(String taskId) {
        while (true) {
            AgentTaskRecord task = taskService.findByTaskId(taskId);
            try {
                log.info("Agent task {} entering RUNNING", taskId);
                taskService.markRunning(taskId);
                if (shouldUsePersistentFeishuFlow(task)) {
                    String replyText = feishuMessageHandler.handle(toInboundMessage(task));
                    taskService.markSuccess(taskId, replyText);
                    log.info("Agent task {} finished SUCCESS with persistent feishu flow", taskId);
                    return;
                }
                AgentResult result = agentService.handle(task.getRequest());
                String replyText = result.getReplyText();
                taskService.markSuccess(taskId, replyText);
                persistWebAssistantMessage(task, replyText);
                if ("feishu".equalsIgnoreCase(task.getSource())) {
                    feishuMessageClient.sendText(task.getReplyTargetId(), replyText);
                }
                log.info("Agent task {} finished SUCCESS", taskId);
                return;
            } catch (Exception exception) {
                handleFailure(taskId, exception);
                AgentTaskRecord updated = taskService.findByTaskId(taskId);
                if (updated.getStatus() == AgentTaskStatus.FAILED) {
                    if ("feishu".equalsIgnoreCase(updated.getSource())) {
                        feishuMessageClient.sendText(updated.getReplyTargetId(), "任务执行失败，请稍后重试。");
                    }
                    return;
                }
            }
        }
    }

    private void processStream(
            String taskId,
            SseEmitter emitter,
            AtomicReference<AsyncGenerator.Cancellable<NodeOutput<org.example.ggbot.agent.graph.GGBotAgentGraphState>>> streamRef,
            AtomicBoolean completed) {
        while (true) {
            AgentTaskRecord task = taskService.findByTaskId(taskId);
            try {
                log.info("Agent task {} entering RUNNING (stream)", taskId);
                taskService.markRunning(taskId);
                AgentState initialState = createInitialState(task);
                AgentState finalState = executeStream(taskId, task, initialState, emitter, streamRef);
                String replyText = finalState.getFinalReply();
                conversationMemoryService.appendAgentMessage(task.getSessionId(), replyText);
                taskService.markSuccess(taskId, replyText);
                persistWebAssistantMessage(task, replyText);
                if ("feishu".equalsIgnoreCase(task.getSource())) {
                    feishuMessageClient.sendText(task.getReplyTargetId(), replyText);
                }
                try {
                    sendEvent(emitter, "complete", completePayload(task, taskId, finalState, replyText));
                } catch (StreamTerminatedException closed) {
                    log.info("Agent task {} stream closed before complete event", taskId);
                }
                completed.set(true);
                safeComplete(emitter);
                log.info("Agent task {} finished SUCCESS (stream)", taskId);
                return;
            } catch (StreamTerminatedException cancelled) {
                cancelTask(taskId, cancelled.getMessage());
                completed.set(true);
                safeComplete(emitter);
                return;
            } catch (Exception exception) {
                if (Thread.currentThread().isInterrupted()) {
                    cancelTask(taskId, "Stream cancelled");
                    completed.set(true);
                    safeComplete(emitter);
                    return;
                }
                handleFailure(taskId, exception);
                AgentTaskRecord updated = taskService.findByTaskId(taskId);
                if (updated.getStatus() == AgentTaskStatus.CANCELLED) {
                    completed.set(true);
                    safeComplete(emitter);
                    return;
                }
                if (updated.getStatus() == AgentTaskStatus.FAILED) {
                    if ("feishu".equalsIgnoreCase(updated.getSource())) {
                        feishuMessageClient.sendText(updated.getReplyTargetId(), "任务执行失败，请稍后重试。");
                    }
                    try {
                        sendEvent(emitter, "error", errorPayload(updated, taskId, errorMessage(exception)));
                    } catch (StreamTerminatedException cancelled) {
                        log.info("Agent task {} stream closed before error event", taskId);
                    } finally {
                        completed.set(true);
                        safeComplete(emitter);
                    }
                    return;
                }
                try {
                    sendEvent(
                            emitter,
                            "progress",
                            retryPayload(
                                    updated,
                                    taskId,
                                    updated.getRetryCount(),
                                    updated.getErrorMessage(),
                                    backoffMillis(updated.getRetryCount())));
                } catch (StreamTerminatedException cancelled) {
                    cancelTask(taskId, cancelled.getMessage());
                    completed.set(true);
                    safeComplete(emitter);
                    return;
                }
            }
        }
    }

    private AgentState createInitialState(AgentTaskRecord task) {
        String conversationId = task.getSessionId();
        conversationMemoryService.appendUserMessage(conversationId, task.getInput());
        return AgentState.initialize(
                task.getTaskId(),
                task.getRequest(),
                conversationMemoryService.getConversationHistory(conversationId)
        );
    }

    private AgentState executeStream(
            String taskId,
            AgentTaskRecord task,
            AgentState initialState,
            SseEmitter emitter,
            AtomicReference<AsyncGenerator.Cancellable<NodeOutput<org.example.ggbot.agent.graph.GGBotAgentGraphState>>> streamRef) {
        AsyncGenerator.Cancellable<NodeOutput<org.example.ggbot.agent.graph.GGBotAgentGraphState>> stream =
                langGraphAgentRunner.stream(initialState, graphProperties.getMaxIterations());
        streamRef.set(stream);
        AgentState lastState = initialState;
        Iterator<NodeOutput<org.example.ggbot.agent.graph.GGBotAgentGraphState>> iterator = stream.iterator();
        while (iterator.hasNext()) {
            NodeOutput<org.example.ggbot.agent.graph.GGBotAgentGraphState> output = iterator.next();
            if (output.isSTART() || output.isEND()) {
                continue;
            }
            AgentState currentState = output.state().delegate();
            lastState = currentState;
            sendEvent(emitter, "progress", progressPayload(task, taskId, output, currentState));
        }
        streamRef.compareAndSet(stream, null);
        return lastState;
    }

    private void handleFailure(String taskId, Exception exception) {
        AgentTaskRecord current = taskService.findByTaskId(taskId);
        int nextRetryCount = current.getRetryCount() + 1;
        log.warn("Agent task {} failed on attempt {}", taskId, nextRetryCount, exception);
        if (nextRetryCount > current.getMaxRetry()) {
            taskService.markFailed(taskId, errorMessage(exception));
            log.error("Agent task {} exhausted retries", taskId, exception);
            return;
        }
        taskService.markRetrying(taskId, nextRetryCount, errorMessage(exception));
        sleep(backoffMillis(nextRetryCount), taskId);
    }

    private long backoffMillis(int retryCount) {
        return switch (retryCount) {
            case 1 -> 2_000L;
            case 2 -> 4_000L;
            default -> 8_000L;
        };
    }

    private void sleep(long millis, String taskId) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            taskService.markCancelled(taskId, "Task retry interrupted");
        }
    }

    private void cancelTask(String taskId, String reason) {
        taskService.markCancelled(taskId, reason == null || reason.isBlank() ? "Stream cancelled" : reason);
        log.info("Agent task {} cancelled", taskId);
    }

    private String errorMessage(Exception exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }

    private boolean shouldUsePersistentFeishuFlow(AgentTaskRecord task) {
        return "feishu".equalsIgnoreCase(task.getSource())
                && task.getRequest().getMetadata().containsKey("tenantKey")
                && task.getRequest().getMetadata().containsKey("openId");
    }

    private FeishuInboundMessage toInboundMessage(AgentTaskRecord task) {
        Map<String, Object> metadata = task.getRequest().getMetadata();
        return new FeishuInboundMessage(
                stringValue(metadata, "tenantKey"),
                stringValue(metadata, "tenantName"),
                stringValue(metadata, "openId"),
                stringValue(metadata, "chatId"),
                stringValue(metadata, "chatName"),
                stringValue(metadata, "chatType"),
                stringValue(metadata, "messageId"),
                task.getRequest().getUserInput(),
                stringValue(metadata, "senderNickname"),
                stringValue(metadata, "senderAvatar")
        );
    }

    private String stringValue(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value == null ? "" : value.toString();
    }

    private Map<String, Object> progressPayload(
            AgentTaskRecord task,
            String taskId,
            NodeOutput<org.example.ggbot.agent.graph.GGBotAgentGraphState> output,
            AgentState state) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", taskId);
        payload.put("conversationId", conversationIdValue(task));
        payload.put("node", output.node());
        payload.put("status", progressStatus(output.node(), state));
        payload.put("message", state.isDone() ? "任务已完成" : "任务执行中...");
        payload.put("iteration", state.getIteration());
        payload.put("done", state.isDone());
        return payload;
    }

    private Map<String, Object> retryPayload(
            AgentTaskRecord task,
            String taskId,
            int retryCount,
            String errorMessage,
            long backoffMillis) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", taskId);
        payload.put("conversationId", conversationIdValue(task));
        payload.put("status", "RETRYING");
        payload.put("message", errorMessage);
        payload.put("retryCount", retryCount);
        payload.put("backoffMillis", backoffMillis);
        return payload;
    }

    private Map<String, Object> completePayload(AgentTaskRecord task, String taskId, AgentState state, String replyText) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", taskId);
        payload.put("conversationId", conversationIdValue(task));
        payload.put("status", "SUCCESS");
        payload.put("replyText", replyText);
        payload.put("iteration", state.getIteration());
        payload.put("done", state.isDone());
        return payload;
    }

    private Map<String, Object> errorPayload(AgentTaskRecord task, String taskId, String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", taskId);
        payload.put("conversationId", conversationIdValue(task));
        payload.put("status", "FAILED");
        payload.put("message", message);
        return payload;
    }

    private String progressStatus(String node, AgentState state) {
        return switch (node) {
            case "plan" -> "PLANNED";
            case "execute" -> "EXECUTED";
            case "reflect" -> "REFLECTED";
            case "replan" -> "REPLANNED";
            default -> state.isDone() ? "DONE" : node.toUpperCase();
        };
    }

    private String conversationIdValue(AgentTaskRecord task) {
        return task.getRequest().getChannel() == AgentChannel.WEB ? task.getSessionId() : null;
    }

    private void persistWebAssistantMessage(AgentTaskRecord task, String replyText) {
        if (task.getRequest().getChannel() != AgentChannel.WEB) {
            return;
        }
        Object orgId = task.getRequest().getMetadata().get("orgId");
        if (orgId instanceof Number number) {
            conversationService.addMessage(
                    number.longValue(),
                    Long.parseLong(task.getSessionId()),
                    null,
                    MessageRole.ASSISTANT,
                    replyText,
                    "text",
                    null
            );
            return;
        }
        sessionService.appendAssistantMessage(task.getUserId(), task.getSessionId(), replyText);
    }

    private void sendEvent(SseEmitter emitter, String eventName, Map<String, Object> payload) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(payload));
        } catch (IOException | IllegalStateException exception) {
            throw new StreamTerminatedException("SSE connection closed", exception);
        }
    }

    private void safeComplete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (IllegalStateException ignored) {
            // The client already disconnected or the emitter was completed elsewhere.
        }
    }

    private static final class StreamTerminatedException extends RuntimeException {

        private StreamTerminatedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
