package org.example.ggbot.planner;

import org.springframework.stereotype.Component;

/**
 * PPT 相关规划规则。
 *
 * <p>当请求中包含 PPT、演示、汇报 等关键词时，
 * 该规则会把 `needPpt` 信号置为 `true`。
 */
@Component
public class PptPlanningRule extends AbstractKeywordPlanningRule {

    @Override
    protected String[] keywords() {
        return new String[]{"PPT", "ppt", "演示", "汇报"};
    }

    @Override
    protected void applySignal(PlanningSignal signal) {
        signal.setNeedPpt(true);
    }

    @Override
    protected String ruleName() {
        return "ppt-rule";
    }
}
