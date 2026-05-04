package org.example.ggbot.agent.replan;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.agent.AgentState;
import org.example.ggbot.agent.reflection.ReflectionAnalysis;
import org.example.ggbot.agent.reflection.ReflectionType;
import org.example.ggbot.planner.Plan;
import org.example.ggbot.planner.PlanStep;
import org.example.ggbot.tool.ToolName;
import org.springframework.stereotype.Component;

/**
 * 工具执行失败的重规划策略
 * 重试失败的步骤，多次失败则降级为总结答复
 */
@Component
@Data
@RequiredArgsConstructor
public class ToolFailureReplanStrategy implements ReplanStrategy {

    @Override
    public ReflectionType supports() {
        return ReflectionType.TOOL_EXECUTION_FAILURE;
    }

    @Override
    public Plan apply(AgentState state, ReflectionAnalysis reflection) {
        Plan plan = state.getCurrentPlan() == null ? new Plan() : state.getCurrentPlan();
        long failedCount = plan.getSteps().stream()
                .filter(step -> step.getStatus() == org.example.ggbot.planner.StepStatus.FAILED)
                .count();

        if (failedCount <= 1) {
            PlanStep failedStep = plan.getSteps().stream()
                    .filter(step -> step.getStatus() == org.example.ggbot.planner.StepStatus.FAILED)
                    .reduce((first, second) -> second)
                    .orElse(null);
            if (failedStep != null) {
                plan.addStep(new PlanStep(
                        "retry-" + (plan.getSteps().size() + 1),
                        failedStep.getToolName(),
                        "失败后重试原步骤",
                        failedStep.getInstruction()
                ));
            }
            return plan;
        }

        plan.addStep(new PlanStep(
                "fallback-" + (plan.getSteps().size() + 1),
                ToolName.SUMMARIZE,
                "失败后的降级总结",
                "原计划多次执行失败，请总结当前失败原因并给出保底答复"
        ));
        return plan;
    }
}
