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
 * 计划耗尽重规划策略
 * 当计划步骤全部执行完但任务仍未完成时，添加补充步骤
 */
@Component
@Data
@RequiredArgsConstructor
public class PlanExhaustedReplanStrategy implements ReplanStrategy {

    @Override
    public ReflectionType supports() {
        return ReflectionType.PLAN_EXHAUSTED_BUT_NOT_DONE;
    }

    @Override
    public Plan apply(AgentState state, ReflectionAnalysis reflection) {
        Plan plan = state.getCurrentPlan() == null ? new Plan() : state.getCurrentPlan();
        plan.addStep(new PlanStep(
                "extend-" + (plan.getSteps().size() + 1),
                ToolName.SUMMARIZE,
                "计划耗尽后的增量补充步骤",
                "当前计划已执行完，但任务仍未完成，请基于已有上下文补充总结并给出下一步建议"
        ));
        return plan;
    }
}
