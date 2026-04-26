package org.example.ggbot.planner;

import org.springframework.stereotype.Component;

/**
 * 文档相关规划规则。
 *
 * <p>当请求中包含文档、方案、PRD 等关键词时，
 * 该规则会把 `needDoc` 信号置为 `true`。
 */
@Component
public class DocPlanningRule extends AbstractKeywordPlanningRule {

    @Override
    protected String[] keywords() {
        return new String[]{"文档", "方案", "PRD", "prd"};
    }

    @Override
    protected void applySignal(PlanningSignal signal) {
        signal.setNeedDoc(true);
    }

    @Override
    protected String ruleName() {
        return "doc-rule";
    }
}
