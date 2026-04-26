package org.example.ggbot.agent.execution;

import org.example.ggbot.agent.AgentState;
import org.example.ggbot.planner.Plan;

public interface Executor {

    ExecutionResult execute(AgentState state, Plan plan);
}
