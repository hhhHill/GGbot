package org.example.ggbot.planner;

import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.agent.AgentState;
import org.springframework.stereotype.Service;

/**
 * `planner` 模块的总入口。
 *
 * <p>它自身不直接编写具体关键词规则，而是负责：
 *
 * <ol>
 *   <li>调用所有 `PlanningRule` 收集命中结果</li>
 *   <li>把这些结果合并成一个最终 `PlanningSignal`</li>
 *   <li>根据规划信号计算 `IntentType`</li>
 *   <li>调用 `PlanStepFactory` 生成最终 `Plan`</li>
 * </ol>
 *
 * <p>这样后续无论新增规则还是接入 LLM 规划器，都不需要重写上层调用方式。
 */
@Service
@Data
@RequiredArgsConstructor
public class PlannerService implements Planner {

    private final List<PlanningRule> planningRules;
    private final PlanStepFactory planStepFactory;

    /**
     * 根据当前 `AgentState` 生成计划。
     */
    @Override
    public Plan plan(AgentState state) {
        if (state.getCurrentPlan() != null && state.getCurrentPlan().hasPendingSteps()) {
            return state.getCurrentPlan();
        }

        String input = state.getUserInput() == null ? "" : state.getUserInput();
        PlanningSignal signal = planningRules.stream()
                .map(rule -> rule.evaluate(state))
                .reduce(PlanningSignal.empty(), (left, right) -> {
                    left.merge(right);
                    return left;
                });

        IntentType intentType = resolveIntentType(signal);
        List<PlanStep> steps = planStepFactory.createSteps(signal, input);
        Plan plan = new Plan();
        plan.setIntentType(intentType);
        plan.setNeedDoc(signal.isNeedDoc());
        plan.setNeedPpt(signal.isNeedPpt());
        plan.appendSteps(steps);
        return plan;
    }

    /**
     * 根据合并后的规划信号计算最终意图。
     */
    private IntentType resolveIntentType(PlanningSignal signal) {
        if (signal.isNeedDoc() && signal.isNeedPpt()) {
            return IntentType.CREATE_DOC_AND_PPT;
        }
        if (signal.isNeedDoc()) {
            return IntentType.CREATE_DOC;
        }
        if (signal.isNeedPpt()) {
            return IntentType.CREATE_PPT;
        }
        return IntentType.CHAT;
    }
}
