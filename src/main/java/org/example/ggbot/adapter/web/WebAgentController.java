package org.example.ggbot.adapter.web;

import java.util.HashMap;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.adapter.web.dto.WebChatRequest;
import org.example.ggbot.adapter.web.dto.WebChatResponse;
import org.example.ggbot.agent.AgentChannel;
import org.example.ggbot.agent.AgentRequest;
import org.example.ggbot.agent.AgentResult;
import org.example.ggbot.agent.AgentService;
import org.example.ggbot.common.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
@Data
@RequiredArgsConstructor
public class WebAgentController {

    private final AgentService agentService;

    @PostMapping("/chat")
    public ApiResponse<WebChatResponse> chat(@RequestBody WebChatRequest request) {
        AgentResult agentResult = agentService.handle(new AgentRequest(
                request.getConversationId(),
                request.getUserId(),
                request.getMessage(),
                AgentChannel.WEB,
                null,
                request.getConversationId(),
                new HashMap<>()
        ));

        return ApiResponse.success(new WebChatResponse(
                agentResult.getTaskId(),
                agentResult.getReplyText(),
                agentResult.getIntentType().name(),
                agentResult.getArtifactSummaries()
        ));
    }
}
