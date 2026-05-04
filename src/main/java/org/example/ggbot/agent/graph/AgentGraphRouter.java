package org.example.ggbot.agent.graph;

import org.example.ggbot.agent.AgentState;
import org.example.ggbot.agent.reflection.ReflectionAnalysis;
import org.springframework.stereotype.Component;

/**
 * Agent工作流路由器
 * 负责在反思节点后决定下一步的执行路径
 */
@Component
public class AgentGraphRouter {

    /**
     * 反思节点后的路由决策
     * @param state 图状态对象
     * @param maxIterations 最大迭代次数
     * @return 路由决策结果
     * 决策逻辑：
     * 1. 如果任务已完成 → 结束
     * 2. 如果达到最大迭代次数 → 结束，返回兜底回复
     * 3. 如果需要重规划 → 进入重规划节点
     * 4. 否则 → 进入下一轮计划执行
     */
    public AgentRoutingDecision routeAfterReflect(GGBotAgentGraphState state, int maxIterations) {
        AgentState delegate = state.delegate();
        if (delegate.isDone()) {
            return AgentRoutingDecision.END;
        }
        if (delegate.getIteration() >= maxIterations) {
            applyMaxIterationFallback(delegate);
            return AgentRoutingDecision.END;
        }

        ReflectionAnalysis reflection = state.lastReflection();
        if (reflection != null && reflection.isNeedReplan()) {
            return AgentRoutingDecision.REPLAN;
        }
        return AgentRoutingDecision.EXECUTE;
    }

    private void applyMaxIterationFallback(AgentState state) {
        state.setDone(true);
        if (state.getFinalReply() == null || state.getFinalReply().isBlank()) {
            state.setFinalReply("Agent reached the maximum iteration limit.");
        }
    }
}
