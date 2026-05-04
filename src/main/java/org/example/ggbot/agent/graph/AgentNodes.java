package org.example.ggbot.agent.graph;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.agent.AgentState;
import org.example.ggbot.agent.execution.ExecutionResult;
import org.example.ggbot.agent.execution.Executor;
import org.example.ggbot.agent.reflection.ReflectionAnalysis;
import org.example.ggbot.agent.reflection.Reflector;
import org.example.ggbot.agent.replan.RePlanner;
import org.example.ggbot.planner.Plan;
import org.example.ggbot.planner.Planner;
import org.springframework.stereotype.Component;

/**
 * LangGraph工作流节点的具体实现
 * 每个方法对应工作流中的一个节点处理逻辑
 */
@Component
@RequiredArgsConstructor
public class AgentNodes {

    /** 规划器：负责生成执行计划 */
    private final Planner planner;
    /** 执行器：负责执行计划中的步骤 */
    private final Executor executor;
    /** 反思器：负责评估执行结果，判断下一步走向 */
    private final Reflector reflector;
    /** 重规划器：负责在计划需要调整时生成新的计划 */
    private final RePlanner rePlanner;

    public Map<String, Object> plan(GGBotAgentGraphState state) {
        AgentState delegate = state.delegate();
        delegate.setIteration(delegate.getIteration() + 1);
        delegate.setCurrentPlan(planner.plan(delegate));
        return Map.of();
    }

    public Map<String, Object> execute(GGBotAgentGraphState state) {
        AgentState delegate = state.delegate();
        Plan currentPlan = delegate.getCurrentPlan();
        if (currentPlan == null) {
            throw new IllegalStateException("Current plan is required before execute.");
        }

        ExecutionResult executionResult = executor.execute(delegate, currentPlan);
        return state.rememberExecutionResult(executionResult);
    }

    public Map<String, Object> reflect(GGBotAgentGraphState state) {
        AgentState delegate = state.delegate();
        ExecutionResult executionResult = state.lastExecutionResult();
        if (executionResult == null) {
            throw new IllegalStateException("Execution result is required before reflect.");
        }

        ReflectionAnalysis reflection = reflector.analyze(delegate, executionResult);
        delegate.update(executionResult, reflection);
        return state.rememberReflection(reflection);
    }

    public Map<String, Object> replan(GGBotAgentGraphState state) {
        AgentState delegate = state.delegate();
        ReflectionAnalysis reflection = state.lastReflection();
        if (reflection == null) {
            throw new IllegalStateException("Reflection is required before replan.");
        }

        delegate.setCurrentPlan(rePlanner.replan(delegate, reflection));
        return Map.of();
    }
}
