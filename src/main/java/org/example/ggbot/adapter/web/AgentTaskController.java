package org.example.ggbot.adapter.web;

import org.example.ggbot.adapter.web.dto.AgentTaskAcceptedResponse;
import org.example.ggbot.adapter.web.dto.AgentTaskResponse;
import org.example.ggbot.agenttask.AgentTaskExecutor;
import org.example.ggbot.agenttask.AgentTaskRecord;
import org.example.ggbot.agenttask.AgentTaskService;
import org.example.ggbot.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class AgentTaskController {

    private final AgentTaskService taskService;
    private final AgentTaskExecutor executor;

    public AgentTaskController(AgentTaskService taskService, AgentTaskExecutor executor) {
        this.taskService = taskService;
        this.executor = executor;
    }

    @GetMapping("/{taskId}")
    public ApiResponse<AgentTaskResponse> get(@PathVariable String taskId) {
        return ApiResponse.success(AgentTaskResponse.from(taskService.findByTaskId(taskId)));
    }

    @PostMapping("/{taskId}/retry")
    public ApiResponse<AgentTaskAcceptedResponse> retry(@PathVariable String taskId) {
        AgentTaskRecord task = taskService.retry(taskId);
        executor.submit(taskId);
        return ApiResponse.success(new AgentTaskAcceptedResponse(task.getTaskId(), task.getSessionId(), task.getStatus().name()));
    }
}
