package org.example.ggbot.agent;

import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.memory.ConversationMemoryService;
import org.example.ggbot.task.TaskRecord;
import org.example.ggbot.task.TaskService;
import org.example.ggbot.task.TaskStatus;
import org.springframework.stereotype.Service;

@Service
@Data
@RequiredArgsConstructor
public class AgentService {

    private final AgentRunner agentRunner;
    private final ConversationMemoryService conversationMemoryService;
    private final TaskService taskService;

    public AgentResult handle(AgentRequest request) {
        TaskRecord taskRecord = taskService.createTask(request.getConversationId(), request.getUserInput());
        taskService.updateStatus(taskRecord.getTaskId(), TaskStatus.RUNNING, "Agent is processing request");

        try {
            conversationMemoryService.appendUserMessage(request.getConversationId(), request.getUserInput());
            AgentState initialState = AgentState.initialize(
                    taskRecord.getTaskId(),
                    request,
                    conversationMemoryService.getConversationHistory(request.getConversationId())
            );

            AgentState finalState = agentRunner.run(initialState);
            String replyText = finalState.getFinalReply();
            conversationMemoryService.appendAgentMessage(request.getConversationId(), replyText);
            taskService.updateStatus(taskRecord.getTaskId(), TaskStatus.COMPLETED, replyText);
            return new AgentResult(
                    taskRecord.getTaskId(),
                    finalState.getCurrentPlan() == null ? null : finalState.getCurrentPlan().getIntentType(),
                    replyText,
                    finalState.getExecutionHistory().stream().map(record -> record.getObservation()).toList()
            );
        } catch (RuntimeException ex) {
            taskService.updateStatus(taskRecord.getTaskId(), TaskStatus.FAILED, ex.getMessage());
            throw ex;
        }
    }
}
