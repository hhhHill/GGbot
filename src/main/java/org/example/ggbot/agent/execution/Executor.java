package org.example.ggbot.agent.execution;

import org.example.ggbot.agent.AgentState;
import org.example.ggbot.planner.Plan;

/**
 * 执行器接口，定义计划执行的契约
 */
public interface Executor {

    /**
     * 执行计划中的待处理步骤
     * @param state Agent状态
     * @param plan 待执行的计划
     * @return 执行结果
     */
    ExecutionResult execute(AgentState state, Plan plan);
}
