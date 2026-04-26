package org.example.ggbot.planner;

import org.example.ggbot.agent.AgentState;

/**
 * 规划规则接口。
 *
 * <p>每条规则只负责判断“当前请求是否命中自己关心的模式”，
 * 并在命中后返回一部分 `PlanningSignal`，而不是直接决定最终 `IntentType`。
 */
public interface PlanningRule {

    PlanningSignal evaluate(AgentState state);
}
