package org.example.ggbot.agent.replan;

import org.example.ggbot.agent.AgentState;
import org.example.ggbot.agent.reflection.ReflectionAnalysis;
import org.example.ggbot.planner.Plan;

/**
 * 重规划器接口，定义计划调整的契约
 * 当反思结果判定需要调整计划时，调用重规划器生成新的计划
 */
public interface RePlanner {

    /**
     * 根据反思结果重新规划
     * @param state Agent状态
     * @param reflection 反思分析结果
     * @return 调整后的新计划
     */
    Plan replan(AgentState state, ReflectionAnalysis reflection);
}
