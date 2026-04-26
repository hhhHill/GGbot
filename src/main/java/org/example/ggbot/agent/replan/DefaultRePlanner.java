package org.example.ggbot.agent.replan;

import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.agent.AgentState;
import org.example.ggbot.agent.reflection.ReflectionAnalysis;
import org.example.ggbot.planner.Plan;
import org.springframework.stereotype.Component;

@Component
@Data
@RequiredArgsConstructor
public class DefaultRePlanner implements RePlanner {

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
