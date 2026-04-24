package org.example.ggbot.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.memory.ConversationMemoryService;
import org.example.ggbot.planner.Plan;
import org.example.ggbot.planner.PlanStep;
import org.example.ggbot.planner.PlannerService;
import org.example.ggbot.task.TaskRecord;
import org.example.ggbot.task.TaskService;
import org.example.ggbot.task.TaskStatus;
import org.example.ggbot.tool.ToolRegistry;
import org.example.ggbot.tool.ToolRequest;
import org.example.ggbot.tool.ToolResult;
import org.springframework.stereotype.Service;

@Service
@Data
@RequiredArgsConstructor
public class AgentService {

    private final PlannerService plannerService;
    private final ConversationMemoryService conversationMemoryService;
    private final TaskService taskService;
    private final ToolRegistry toolRegistry;

    public AgentResult handle(AgentRequest request) {
        TaskRecord taskRecord = taskService.createTask(request.getConversationId(), request.getUserInput());
        taskService.updateStatus(taskRecord.getTaskId(), TaskStatus.RUNNING, "Agent is processing request");

        try {
            conversationMemoryService.appendUserMessage(request.getConversationId(), request.getUserInput());
            AgentContext context = new AgentContext(
                    taskRecord.getTaskId(),
                    request.getConversationId(),
                    request.getUserId(),
                    request.getChannel(),
                    conversationMemoryService.getConversationHistory(request.getConversationId()),
                    request.getMetadata()
            );

            Plan plan = plannerService.plan(request);
            List<String> artifactSummaries = new ArrayList<>();
            List<String> replySections = new ArrayList<>();

            for (PlanStep step : plan.getSteps()) {
                ToolResult toolResult = toolRegistry.getTool(step.getToolName()).execute(
                        new ToolRequest(taskRecord.getTaskId(), step.getInstruction(), context, new HashMap<>())
                );
                artifactSummaries.add(toolResult.getSummary());
                replySections.add(renderToolResult(toolResult));
            }

            String replyText = String.join("\n\n", replySections);
            conversationMemoryService.appendAgentMessage(request.getConversationId(), replyText);
            taskService.updateStatus(taskRecord.getTaskId(), TaskStatus.COMPLETED, replyText);
            return new AgentResult(taskRecord.getTaskId(), plan.getIntentType(), replyText, artifactSummaries);
        } catch (RuntimeException ex) {
            taskService.updateStatus(taskRecord.getTaskId(), TaskStatus.FAILED, ex.getMessage());
            throw ex;
        }
    }

    private String renderToolResult(ToolResult toolResult) {
        Object artifact = toolResult.getArtifact();
        if (artifact == null) {
            return toolResult.getSummary();
        }
        return toolResult.getSummary() + "\n" + artifact;
    }
}
