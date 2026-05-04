package org.example.ggbot.agent.replan;

import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.agent.AgentState;
import org.example.ggbot.agent.reflection.ReflectionAnalysis;
import org.example.ggbot.planner.Plan;
import org.springframework.stereotype.Component;

/**
 * 默认重规划器实现
 * 基于策略模式，根据反思结果类型选择对应的重规划策略
 */
@Component
@Data
@RequiredArgsConstructor
public class DefaultRePlanner implements RePlanner {

    /** 所有重规划策略实现列表 */
    private final List<ReplanStrategy> strategies;

    @Override
    public Plan replan(AgentState state, ReflectionAnalysis reflection) {
        return strategies.stream()
                .filter(strategy -> strategy.supports() == reflection.getReflectionType())
                .findFirst()
                .map(strategy -> strategy.apply(state, reflection))
                .orElseGet(() -> state.getCurrentPlan() == null ? new Plan() : state.getCurrentPlan());
    }
}
