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

    private final FeishuEventParser feishuEventParser;
    private final AgentTaskService taskService;
    private final AgentTaskExecutor taskExecutor;

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
        AgentTaskCreationResult creationResult = taskService.createOrGetByExternalEventId(agentRequest, "feishu", externalEventId);
        if (creationResult.created()) {
            taskExecutor.submit(creationResult.task().getTaskId());
        }
        return ApiResponse.success(new AgentTaskAcceptedResponse(
                creationResult.task().getTaskId(),
                creationResult.task().getSessionId(),
                creationResult.task().getStatus().name()
        ));
    }
}
