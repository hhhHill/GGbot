package org.example.ggbot.adapter.feishu;

import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.adapter.feishu.dto.FeishuWebhookRequest;
import org.example.ggbot.agent.AgentRequest;
import org.example.ggbot.agent.AgentResult;
import org.example.ggbot.agent.AgentService;
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
    private final AgentService agentService;
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
        AgentResult agentResult = agentService.handle(agentRequest);
        feishuMessageClient.sendText(agentRequest.getReplyTargetId(), agentResult.getReplyText());
        return ApiResponse.success(agentResult);
    }
}
