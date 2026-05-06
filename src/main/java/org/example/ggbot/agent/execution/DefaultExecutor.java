package org.example.ggbot.agent.execution;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import org.example.ggbot.agent.AgentContext;
import org.example.ggbot.agent.AgentState;
import org.example.ggbot.planner.Plan;
import org.example.ggbot.planner.PlanStep;
import org.example.ggbot.planner.StepStatus;
import org.example.ggbot.planner.StepType;
import org.example.ggbot.tool.SpringAiToolExecutor;
import org.example.ggbot.tool.ToolResult;
import org.example.ggbot.tool.support.ArtifactContentExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 默认执行器实现
 * 负责调用工具执行计划中的待处理步骤，每次执行一个步骤
 */
@Component
@Data
public class DefaultExecutor implements Executor {

    /** Spring AI工具执行器，负责调用具体的工具实现 */
    private final SpringAiToolExecutor toolExecutor;
    private final ArtifactContentExtractor artifactContentExtractor;

    @Autowired
    public DefaultExecutor(SpringAiToolExecutor toolExecutor, ArtifactContentExtractor artifactContentExtractor) {
        this.toolExecutor = toolExecutor;
        this.artifactContentExtractor = artifactContentExtractor;
    }

    public DefaultExecutor(SpringAiToolExecutor toolExecutor) {
        this(toolExecutor, new ArtifactContentExtractor());
    }

    @Override
    public ExecutionResult execute(AgentState state, Plan plan) {
        List<PlanStep> pendingSteps = plan.getPendingSteps();
        if (pendingSteps.isEmpty()) {
            return new ExecutionResult(List.of(), List.of(), true, false, "没有待执行步骤");
        }

        PlanStep step = pendingSteps.stream()
                .filter(candidate -> dependenciesSatisfied(plan, state, candidate))
                .findFirst()
                .orElse(null);
        if (step == null) {
            return dependencyFailure(state, pendingSteps);
        }

        if (step.getType() == StepType.CLARIFY) {
            step.markSuccess(step.getInstruction());
            StepExecutionRecord record = new StepExecutionRecord(
                    state.getIteration(),
                    step.getStepId(),
                    step.getToolName(),
                    step.getInstruction(),
                    step.getStatus(),
                    "输出澄清问题",
                    step.getInstruction(),
                    step.getInstruction(),
                    null
            );
            return new ExecutionResult(List.of(step), List.of(record), true, false, step.getInstruction());
        }

        try {
            String instruction = materializeInstruction(step, state);
            ToolResult toolResult = toolExecutor.execute(
                    step.getToolName(),
                    instruction,
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
                            "stepId", step.getStepId(),
                            "dependsOn", step.getDependsOn(),
                            "inputRefs", step.getInputRefs(),
                            "stepResults", collectStepResults(state, step)
                    )
            );

            String observation = renderToolResult(toolResult);
            step.markSuccess(toolResult);
            StepExecutionRecord record = new StepExecutionRecord(
                    state.getIteration(),
                    step.getStepId(),
                    step.getToolName(),
                    instruction,
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

    private boolean dependenciesSatisfied(Plan plan, AgentState state, PlanStep step) {
        if (step.getDependsOn().isEmpty()) {
            return true;
        }
        Map<String, PlanStep> stepMap = plan.getSteps().stream()
                .collect(Collectors.toMap(PlanStep::getStepId, candidate -> candidate, (left, right) -> left));
        return step.getDependsOn().stream().allMatch(dependencyId -> {
            PlanStep dependency = stepMap.get(dependencyId);
            return dependency != null
                    && dependency.getStatus() == StepStatus.SUCCESS
                    && state.getIntermediateResults().containsKey(dependencyId);
        });
    }

    private ExecutionResult dependencyFailure(AgentState state, List<PlanStep> pendingSteps) {
        String message = "当前待执行步骤的前置依赖尚未完成: " + pendingSteps.stream()
                .map(step -> step.getStepId() + " -> " + step.getDependsOn())
                .collect(Collectors.joining(", "));
        StepExecutionRecord record = new StepExecutionRecord(
                state.getIteration(),
                pendingSteps.get(0).getStepId(),
                pendingSteps.get(0).getToolName(),
                pendingSteps.get(0).getInstruction(),
                StepStatus.FAILED,
                "检查步骤依赖",
                message,
                null,
                message
        );
        return new ExecutionResult(List.of(), List.of(record), false, true, message);
    }

    private String materializeInstruction(PlanStep step, AgentState state) {
        if (step.getInputRefs().isEmpty()) {
            return step.getInstruction();
        }
        String dependencyContext = step.getInputRefs().stream()
                .map(ref -> {
                    Object result = state.getIntermediateResults().get(ref);
                    if (result instanceof ToolResult toolResult) {
                        return "%s: %s".formatted(ref, artifactContentExtractor.extract(toolResult));
                    }
                    return "%s: %s".formatted(ref, String.valueOf(result));
                })
                .collect(Collectors.joining("\n"));
        return step.getInstruction() + "\n\n前置结果：\n" + dependencyContext;
    }

    private Map<String, Object> collectStepResults(AgentState state, PlanStep step) {
        return step.getInputRefs().stream()
                .filter(state.getIntermediateResults()::containsKey)
                .collect(Collectors.toMap(ref -> ref, state.getIntermediateResults()::get));
    }
}
