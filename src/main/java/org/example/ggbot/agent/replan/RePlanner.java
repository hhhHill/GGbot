package org.example.ggbot.agent.replan;

import org.example.ggbot.agent.AgentState;
import org.example.ggbot.agent.reflection.ReflectionAnalysis;
import org.example.ggbot.planner.Plan;

public interface RePlanner {

    Plan replan(AgentState state, ReflectionAnalysis reflection);
}
