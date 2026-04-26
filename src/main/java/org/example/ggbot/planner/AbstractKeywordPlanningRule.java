package org.example.ggbot.planner;

import java.util.Arrays;
import java.util.List;
import org.example.ggbot.agent.AgentState;

/**
 * 基于关键词匹配的规则基类。
 *
 * <p>子类只需要提供三件事：
 *
 * <ul>
 *   <li>自己关心的关键词集合</li>
 *   <li>命中后如何修改 `PlanningSignal`</li>
 *   <li>规则自身的名称</li>
 * </ul>
 *
 * <p>这样可以避免每个规则类重复编写相同的匹配样板代码。
 */
abstract class AbstractKeywordPlanningRule implements PlanningRule {

    @Override
    public PlanningSignal evaluate(AgentState state) {
        String input = state.getUserInput() == null ? "" : state.getUserInput();
        List<String> matchedKeywords = Arrays.stream(keywords())
                .filter(input::contains)
                .distinct()
                .toList();

        if (matchedKeywords.isEmpty()) {
            return PlanningSignal.empty();
        }

        PlanningSignal signal = PlanningSignal.empty();
        applySignal(signal);
        signal.getMatchedKeywords().addAll(matchedKeywords);
        signal.getMatchedRules().add(ruleName());
        return signal;
    }

    protected abstract String[] keywords();

    protected abstract void applySignal(PlanningSignal signal);

    protected abstract String ruleName();
}
