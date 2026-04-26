package org.example.ggbot.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.agent.execution.ExecutionResult;
import org.example.ggbot.agent.execution.StepExecutionRecord;
import org.example.ggbot.agent.reflection.ReflectionAnalysis;
import org.example.ggbot.planner.Plan;
import org.example.ggbot.tool.ToolResult;

@Data
@RequiredArgsConstructor
public class AgentState {

    private final String taskId;
    private final AgentRequest request;
    private final String userInput;
    private Plan currentPlan;
    private final List<StepExecutionRecord> executionHistory = new ArrayList<>();
    private final Map<String, Object> intermediateResults = new HashMap<>();
    private final List<String> memory = new ArrayList<>();
    private final Map<String, Object> context = new HashMap<>();
    private int iteration;
    private boolean done;
    private String finalReply = "";

    public static AgentState initialize(String taskId, AgentRequest request, List<String> memory) {
        AgentState state = new AgentState(taskId, request, request.getUserInput());
        state.getMemory().addAll(memory);
        state.getContext().putAll(request.getMetadata());
        return state;
    }

    public AgentState update(ExecutionResult result, ReflectionAnalysis reflection) {
        this.executionHistory.addAll(result.getRecords());
        for (StepExecutionRecord record : result.getRecords()) {
            if (record.getResult() != null) {
                intermediateResults.put(record.getStepId(), record.getResult());
            }
            if (record.getObservation() != null && !record.getObservation().isBlank()) {
                appendReply(record.getObservation());
            } else if (record.getResult() instanceof ToolResult toolResult) {
                appendReply(toolResult.getSummary());
            }
        }
        this.done = reflection.isDone();
        context.put("lastReflection", reflection);
        context.put("lastExecutionSummary", result.getSummary());
        return this;
    }

    private void appendReply(String replyPart) {
        if (replyPart == null || replyPart.isBlank()) {
            return;
        }
        if (finalReply == null || finalReply.isBlank()) {
            finalReply = replyPart;
            return;
        }
        finalReply = finalReply + "\n\n" + replyPart;
    }
}
