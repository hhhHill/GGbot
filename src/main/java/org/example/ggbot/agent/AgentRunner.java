package org.example.ggbot.agent;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.agent.execution.ExecutionResult;
import org.example.ggbot.agent.execution.Executor;
import org.example.ggbot.agent.reflection.ReflectionAnalysis;
import org.example.ggbot.agent.reflection.Reflector;
import org.example.ggbot.agent.replan.RePlanner;
import org.example.ggbot.planner.Plan;
import org.example.ggbot.planner.Planner;
import org.springframework.stereotype.Component;

@Component
@Data
@RequiredArgsConstructor
public class AgentRunner {

    private final Planner planner;
    private final Executor executor;
    private final Reflector reflector;
    private final RePlanner rePlanner;
    private final int maxIterations = 10;

    public AgentState run(AgentState initialState) {
        AgentState state = initialState;
        while (!state.isDone() && state.getIteration() < maxIterations) {
            state.setIteration(state.getIteration() + 1);
            Plan plan = planner.plan(state);
            state.setCurrentPlan(plan);

            ExecutionResult executionResult = executor.execute(state, plan);
            ReflectionAnalysis reflection = reflector.analyze(state, executionResult);
            state.update(executionResult, reflection);

            if (state.isDone()) {
                break;
            }

            if (reflection.isNeedReplan()) {
                state.setCurrentPlan(rePlanner.replan(state, reflection));
            }
        }

        if (!state.isDone()) {
            state.setDone(true);
            if (state.getFinalReply() == null || state.getFinalReply().isBlank()) {
                state.setFinalReply("Agent reached the maximum iteration limit.");
            }
        }
        return state;
    }
}
