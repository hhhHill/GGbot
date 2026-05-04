package org.example.ggbot.agent.execution;

import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.agent.AgentContext;
import org.example.ggbot.agent.AgentState;
import org.example.ggbot.planner.Plan;
import org.example.ggbot.planner.PlanStep;
import org.example.ggbot.tool.SpringAiToolExecutor;
import org.example.ggbot.tool.ToolResult;
import org.springframework.stereotype.Component;

/**
 * 默认执行器实现
 * 负责调用工具执行计划中的待处理步骤，每次执行一个步骤
 */
@Component
@Data
@RequiredArgsConstructor
public class DefaultExecutor implements Executor {

    /** Spring AI工具执行器，负责调用具体的工具实现 */
    private final SpringAiToolExecutor toolExecutor;

    @Override
    public ExecutionResult execute(AgentState state, Plan plan) {
        List<PlanStep> pendingSteps = plan.getPendingSteps();
        if (pendingSteps.isEmpty()) {
            return new ExecutionResult(List.of(), List.of(), true, false, "没有待执行步骤");
        }

        PlanStep step = pendingSteps.get(0);
        try {
            ToolResult toolResult = toolExecutor.execute(
                    step.getToolName(),
                    step.getInstruction(),
                    new AgentContext(
                            state.getTaskId(),
                            state.getRequest().getConversationId(),
                            state.getRequest().getUserId(),
                            state.getRequest().getChannel(),
                            state.getMemory(),
                            state.getContext()
                    ),
                    Map.of(
                            "iteration", state.getIteration(),
                            "stepId", step.getStepId()
                    )
            );

            String observation = renderToolResult(toolResult);
            step.markSuccess(toolResult);
            StepExecutionRecord record = new StepExecutionRecord(
                    state.getIteration(),
                    step.getStepId(),
                    step.getToolName(),
                    step.getInstruction(),
                    step.getStatus(),
                    "执行工具步骤",
                    observation,
                    toolResult,
                    null
            );
            return new ExecutionResult(List.of(step), List.of(record), true, false, observation);
        } catch (RuntimeException ex) {
            step.markFailed(ex.getMessage());
            StepExecutionRecord record = new StepExecutionRecord(
                    state.getIteration(),
                    step.getStepId(),
                    step.getToolName(),
                    step.getInstruction(),
                    step.getStatus(),
                    "执行工具步骤",
                    "工具执行失败",
                    null,
                    ex.getMessage()
            );
            return new ExecutionResult(List.of(step), List.of(record), false, true, ex.getMessage());
        }
    }

    private String renderToolResult(ToolResult toolResult) {
        Object artifact = toolResult.getArtifact();
        if (artifact == null) {
            return toolResult.getSummary();
        }
        if (artifact instanceof String artifactText && artifactText.equals(toolResult.getSummary())) {
            return toolResult.getSummary();
        }
        return toolResult.getSummary() + "\n" + artifact;
    }
}
