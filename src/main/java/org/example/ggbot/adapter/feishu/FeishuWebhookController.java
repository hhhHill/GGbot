package org.example.ggbot.adapter.feishu;

import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.adapter.feishu.dto.FeishuWebhookRequest;
import org.example.ggbot.agent.AgentRequest;
import org.example.ggbot.adapter.web.dto.AgentTaskAcceptedResponse;
import org.example.ggbot.agenttask.AgentTaskCreationResult;
import org.example.ggbot.agenttask.AgentTaskExecutor;
import org.example.ggbot.agenttask.AgentTaskService;
import org.example.ggbot.common.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/feishu")
@Data
@RequiredArgsConstructor
public class FeishuWebhookController {

    private static final String FEISHU_SOURCE = "feishu";
    private static final String BUSY_MESSAGE = "上一条消息仍在处理中，请等待当前任务完成后再发送下一条。";

    private final FeishuEventParser feishuEventParser;
    private final AgentTaskService taskService;
    private final AgentTaskExecutor taskExecutor;
    private final FeishuMessageClient feishuMessageClient;

    @PostMapping("/webhook")
    public Object webhook(@RequestBody FeishuWebhookRequest request) {
        if (request.getChallenge() != null && !request.getChallenge().isBlank()) {
            return Map.of("challenge", request.getChallenge());
        }

        String eventType = feishuEventParser.eventType(request);
        if (!"im.message.receive_v1".equals(eventType)) {
            return ApiResponse.success("ignored", "Unsupported event type: " + eventType);
        }

        AgentRequest agentRequest = feishuEventParser.toAgentRequest(request);
        String externalEventId = feishuEventParser.externalEventId(request);
        if (agentRequest.getConversationId() != null && !agentRequest.getConversationId().isBlank()) {
            return taskService.findActiveTask(FEISHU_SOURCE, agentRequest.getConversationId())
                    .map(activeTask -> activeTask.getExternalEventId() != null
                            && activeTask.getExternalEventId().equals(externalEventId)
                            ? acceptedResponse(activeTask)
                            : busyResponse(agentRequest, activeTask))
                    .orElseGet(() -> createTask(agentRequest, externalEventId));
        }
        return createTask(agentRequest, externalEventId);
    }

    private ApiResponse<AgentTaskAcceptedResponse> createTask(AgentRequest agentRequest, String externalEventId) {
        AgentTaskCreationResult creationResult = taskService.createOrGetByExternalEventId(agentRequest, FEISHU_SOURCE, externalEventId);
        if (creationResult.created()) {
            taskExecutor.submit(creationResult.task().getTaskId());
        }
        return acceptedResponse(creationResult.task());
    }

    private ApiResponse<AgentTaskAcceptedResponse> busyResponse(AgentRequest request, org.example.ggbot.agenttask.AgentTaskRecord activeTask) {
        feishuMessageClient.sendText(request.getReplyTargetId(), BUSY_MESSAGE);
        return ApiResponse.success(
                new AgentTaskAcceptedResponse(
                        activeTask.getTaskId(),
                        activeTask.getSessionId(),
                        activeTask.getStatus().name()
                ),
                BUSY_MESSAGE
        );
    }

    private ApiResponse<AgentTaskAcceptedResponse> acceptedResponse(org.example.ggbot.agenttask.AgentTaskRecord task) {
        return ApiResponse.success(new AgentTaskAcceptedResponse(
                task.getTaskId(),
                task.getSessionId(),
                task.getStatus().name()
        ));
    }
}
