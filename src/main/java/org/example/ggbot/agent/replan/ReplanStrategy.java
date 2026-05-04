package org.example.ggbot.agent.replan;

import org.example.ggbot.agent.AgentState;
import org.example.ggbot.agent.reflection.ReflectionAnalysis;
import org.example.ggbot.agent.reflection.ReflectionType;
import org.example.ggbot.planner.Plan;

/**
 * 重规划策略接口
 * 每种反思类型对应一个策略实现，处理特定场景下的计划调整逻辑
 */
public interface ReplanStrategy {

    /**
     * 返回该策略支持的反思类型
     * @return 支持的ReflectionType
     */
    ReflectionType supports();

    /**
     * 应用重规划策略，调整现有计划
     * @param state Agent状态
     * @param reflection 反思分析结果
     * @return 调整后的计划
     */
    Plan apply(AgentState state, ReflectionAnalysis reflection);
}
