package org.example.ggbot.agent.reflection;

import org.example.ggbot.agent.AgentState;
import org.example.ggbot.agent.execution.ExecutionResult;

/**
 * 反思器接口，定义执行结果分析的契约
 * 负责评估执行结果的质量，决定下一步动作
 */
public interface Reflector {

    /**
     * 分析执行结果，生成反思结论
     * @param state Agent状态
     * @param result 执行结果
     * @return 反思分析结论
     */
    ReflectionAnalysis analyze(AgentState state, ExecutionResult result);
}
