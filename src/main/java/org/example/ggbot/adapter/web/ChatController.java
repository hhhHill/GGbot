package org.example.ggbot.adapter.web;

import java.util.HashMap;
import org.example.ggbot.adapter.web.dto.AgentTaskAcceptedResponse;
import org.example.ggbot.adapter.web.dto.ChatSendRequest;
import org.example.ggbot.agent.AgentChannel;
import org.example.ggbot.agent.AgentRequest;
import org.example.ggbot.agenttask.AgentTaskExecutor;
import org.example.ggbot.agenttask.AgentTaskRecord;
import org.example.ggbot.agenttask.AgentTaskService;
import org.example.ggbot.common.ApiResponse;
import org.example.ggbot.session.WebSessionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final AgentTaskService taskService;
    private final AgentTaskExecutor executor;
    private final WebSessionService sessionService;

    public ChatController(AgentTaskService taskService, AgentTaskExecutor executor, WebSessionService sessionService) {
        this.taskService = taskService;
        this.executor = executor;
        this.sessionService = sessionService;
    }

    @PostMapping("/send")
    public ApiResponse<AgentTaskAcceptedResponse> send(@RequestBody ChatSendRequest request) {
        AgentRequest agentRequest = new AgentRequest(
                request.getSessionId(),
                request.getUserId(),
                request.getMessage(),
                AgentChannel.WEB,
                null,
                request.getSessionId(),
                new HashMap<>()
        );
        sessionService.appendUserMessage(request.getUserId(), request.getSessionId(), request.getMessage());
        AgentTaskRecord task = taskService.createTask(agentRequest, "web", null);
        executor.submit(task.getTaskId());
        return ApiResponse.success(new AgentTaskAcceptedResponse(task.getTaskId(), task.getSessionId(), task.getStatus().name()));
    }
}
