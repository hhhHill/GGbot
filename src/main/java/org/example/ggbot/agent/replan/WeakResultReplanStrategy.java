package org.example.ggbot.agent.replan;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.agent.AgentState;
import org.example.ggbot.agent.execution.StepExecutionRecord;
import org.example.ggbot.agent.reflection.ReflectionAnalysis;
import org.example.ggbot.agent.reflection.ReflectionType;
import org.example.ggbot.planner.Plan;
import org.example.ggbot.planner.PlanStep;
import org.example.ggbot.tool.ToolName;
import org.springframework.stereotype.Component;

/**
 * 弱结果重规划策略
 * 当执行结果为空或质量过弱时，添加补强步骤优化结果
 */
@Component
@Data
@RequiredArgsConstructor
public class WeakResultReplanStrategy implements ReplanStrategy {

    @Override
    public ReflectionType supports() {
        return ReflectionType.EMPTY_OR_WEAK_RESULT;
    }

    @Override
    public Plan apply(AgentState state, ReflectionAnalysis reflection) {
        Plan plan = state.getCurrentPlan() == null ? new Plan() : state.getCurrentPlan();
        StepExecutionRecord latest = state.getExecutionHistory().isEmpty()
                ? null
                : state.getExecutionHistory().get(state.getExecutionHistory().size() - 1);

        ToolName lastTool = latest != null
                ? latest.getToolName()
                : plan.getSteps().stream()
                .filter(step -> step.getStatus() != org.example.ggbot.planner.StepStatus.PENDING)
                .reduce((first, second) -> second)
                .map(PlanStep::getToolName)
                .orElse(null);

        ToolName nextTool = lastTool == ToolName.GENERATE_PPT
                ? ToolName.MODIFY_PPT
                : ToolName.SUMMARIZE;

        String instruction = nextTool == ToolName.MODIFY_PPT
                ? "当前 PPT 结果较弱，请补充结构、增强内容完整性并输出修订版"
                : "当前结果较弱，请基于已有上下文给出更完整的总结答复";

        plan.addStep(new PlanStep(
                "improve-" + (plan.getSteps().size() + 1),
                nextTool,
                "结果过弱后的补强步骤",
                instruction
        ));
        return plan;
    }
}
