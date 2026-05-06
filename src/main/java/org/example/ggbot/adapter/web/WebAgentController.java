package org.example.ggbot.adapter.web;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import jakarta.servlet.http.Cookie;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ggbot.ai.StreamingContextKeys;
import org.example.ggbot.adapter.web.dto.AgentTaskAcceptedResponse;
import org.example.ggbot.adapter.web.dto.AgentTaskResponse;
import org.example.ggbot.adapter.web.dto.WebChatRequest;
import org.example.ggbot.agent.AgentChannel;
import org.example.ggbot.agent.AgentRequest;
import org.example.ggbot.agenttask.AgentTaskExecutor;
import org.example.ggbot.agenttask.AgentTaskRecord;
import org.example.ggbot.agenttask.AgentTaskService;
import org.example.ggbot.common.ApiResponse;
import org.example.ggbot.enums.MessageRole;
import org.example.ggbot.persistence.entity.ConversationEntity;
import org.example.ggbot.persistence.entity.SubjectEntity;
import org.example.ggbot.service.auth.WebUserContext;
import org.example.ggbot.service.auth.WebUserContextResolver;
import org.example.ggbot.service.conversation.ConversationService;
import org.example.ggbot.service.organization.OrganizationService;
import org.example.ggbot.service.subject.SubjectService;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Web端Agent交互控制器
 * 处理所有来自Web前端的Agent相关HTTP请求
 * 提供普通异步、流式两种调用模式，支持任务查询、重试能力
 */
@RestController
@RequestMapping("/api/agent")
@Data
@RequiredArgsConstructor
@Slf4j
public class WebAgentController {

    private final AgentTaskService taskService;
    private final AgentTaskExecutor taskExecutor;
    private final WebUserContextResolver webUserContextResolver;
    private final OrganizationService organizationService;
    private final SubjectService subjectService;
    private final ConversationService conversationService;

    /**
     * 普通异步聊天接口
     * @param request 用户聊天请求，包含会话ID、用户ID、消息内容
     * @return 任务接收响应，包含任务ID，前端可通过任务ID轮询结果
     * 逻辑：保存用户消息到会话记忆 -> 创建异步任务 -> 提交到线程池执行 -> 立即返回任务ID
     * 适用场景：不需要实时进度展示的场景，前端轮询获取最终结果
     */
    @PostMapping("/chat")
    public ApiResponse<AgentTaskAcceptedResponse> chat(
            @CookieValue(value = "web_user_key", required = false) Cookie webUserKeyCookie,
            @CookieValue(value = "web_auth_token", required = false) Cookie authCookie,
            @RequestBody WebChatRequest request
    ) {
        PreparedWebChat prepared = prepareWebChat(authCookie, webUserKeyCookie, request);
        AgentRequest agentRequest = toAgentRequest(prepared);
        AgentTaskRecord task = taskService.createTask(agentRequest, "web", null);
        taskExecutor.submit(task.getTaskId());
        return ApiResponse.success(new AgentTaskAcceptedResponse(task.getTaskId(), task.getSessionId(), task.getStatus().name()));
    }

    /**
     * SSE流式聊天接口
     * @param request 用户聊天请求，包含会话ID、用户ID、消息内容
     * @return SseEmitter流对象，前端通过EventSource监听实时推送
     * 逻辑：保存用户消息到会话记忆 -> 创建异步任务 -> 初始化SSE连接 -> 提交流式任务执行 -> 立即返回流对象
     * 特性：实时推送执行进度/结果/错误，支持中途取消，无超时限制
     * 适用场景：需要实时展示执行进度的场景，用户可以看到每一步执行状态
     */
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(
            @CookieValue(value = "web_user_key", required = false) Cookie webUserKeyCookie,
            @CookieValue(value = "web_auth_token", required = false) Cookie authCookie,
            @ModelAttribute WebChatRequest request
    ) {
        SseEmitter emitter = new SseEmitter(0L);
        Consumer<String> chunkConsumer = chunk -> {
            try {
                emitter.send(SseEmitter.event().name("chunk").data(Map.of("content", chunk)));
            } catch (Exception exception) {
                throw new StreamEmissionException(exception);
            }
        };
        PreparedWebChat prepared = prepareWebChat(authCookie, webUserKeyCookie, request);
        AgentRequest agentRequest = toAgentRequest(prepared, chunkConsumer);
        AgentTaskRecord task = taskService.createTask(agentRequest, "web", null);
        emitter.onCompletion(() -> log.debug("Agent stream {} completed", task.getTaskId()));
        emitter.onTimeout(() -> log.warn("Agent stream {} timed out", task.getTaskId()));
        emitter.onError(throwable -> log.warn("Agent stream {} failed", task.getTaskId(), throwable));
        taskExecutor.submitStream(task.getTaskId(), emitter);
        return emitter;
    }

    /**
     * 查询任务执行状态接口
     * @param taskId 任务ID
     * @return 任务当前状态、结果、错误信息等
     * 用于普通异步聊天场景下前端轮询获取结果，流式场景下也可以作为降级查询接口
     */
    @GetMapping("/jobs/{taskId}")
    public ApiResponse<AgentTaskResponse> jobStatus(@PathVariable String taskId) {
        return ApiResponse.success(AgentTaskResponse.from(taskService.findByTaskId(taskId)));
    }

    /**
     * 任务重试接口
     * @param taskId 失败的任务ID
     * @return 新的任务接收响应
     * 逻辑：重置任务状态为待执行，清空之前的错误信息 -> 重新提交到线程池执行 -> 返回新的任务状态
     * 适用场景：任务执行失败后，用户点击重试按钮触发
     */
    @PostMapping("/jobs/{taskId}/retry")
    public ApiResponse<AgentTaskAcceptedResponse> retry(@PathVariable String taskId) {
        // 重置任务状态，重试次数+1
        AgentTaskRecord retried = taskService.retry(taskId);
        // 重新提交任务到线程池执行
        taskExecutor.submit(taskId);
        return ApiResponse.success(new AgentTaskAcceptedResponse(retried.getTaskId(), retried.getSessionId(), retried.getStatus().name()));
    }

    /**
     * 将Web层请求对象转换为内部Agent执行请求对象
     * @param request Web层聊天请求
     * @return 内部AgentRequest对象
     * 封装Web端请求的通用参数，适配内部Agent执行上下文，屏蔽外部请求和内部逻辑的差异
     */
    private AgentRequest toAgentRequest(PreparedWebChat prepared) {
        return toAgentRequest(prepared, null);
    }

    private AgentRequest toAgentRequest(PreparedWebChat prepared, Consumer<String> chunkConsumer) {
        Map<String, Object> metadata = new HashMap<>();
        if (chunkConsumer != null) {
            metadata.put(StreamingContextKeys.STREAM_CHUNK_CONSUMER, chunkConsumer);
        }
        metadata.put("orgId", prepared.orgId());
        return new AgentRequest(
                String.valueOf(prepared.conversationId()),
                String.valueOf(prepared.userId()),
                prepared.message(),
                AgentChannel.WEB,
                null,
                String.valueOf(prepared.conversationId()),
                metadata
        );
    }

    private PreparedWebChat prepareWebChat(Cookie authCookie, Cookie webUserKeyCookie, WebChatRequest request) {
        WebUserContext context = webUserContextResolver.resolve(authCookie, webUserKeyCookie, request.getWebUserKey(), false);
        Long userId = context.resolvedUser().user().getId();
        Long currentOrgId = resolveOrgId(context, request.getOrgId());
        SubjectEntity subject = subjectService.getOrCreateUserSubject(userId, currentOrgId);
        ConversationEntity conversation = resolveConversation(currentOrgId, subject.getId(), userId, request.getConversationId(), request.getMessage());
        conversationService.addMessage(
                currentOrgId,
                conversation.getId(),
                userId,
                MessageRole.USER,
                request.getMessage(),
                "text",
                null
        );
        return new PreparedWebChat(currentOrgId, userId, conversation.getId(), request.getMessage());
    }

    private Long resolveOrgId(WebUserContext context, Long requestedOrgId) {
        if (requestedOrgId == null) {
            return context.resolvedUser().personalOrg().getId();
        }
        organizationService.checkUserActiveInOrg(context.resolvedUser().user().getId(), requestedOrgId);
        return requestedOrgId;
    }

    private ConversationEntity resolveConversation(
            Long orgId,
            Long subjectId,
            Long userId,
            String conversationId,
            String title
    ) {
        if (conversationId != null && !conversationId.isBlank()) {
            return ConversationEntity.builder()
                    .id(Long.valueOf(conversationId))
                    .orgId(orgId)
                    .subjectId(subjectId)
                    .build();
        }
        return conversationService.createConversation(orgId, subjectId, "web", title, userId);
    }

    private record PreparedWebChat(Long orgId, Long userId, Long conversationId, String message) {
    }

    private static final class StreamEmissionException extends RuntimeException {

        private StreamEmissionException(Throwable cause) {
            super(cause);
        }
    }
}
