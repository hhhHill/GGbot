package org.example.ggbot.agent.replan;

import org.example.ggbot.agent.AgentState;
import org.example.ggbot.agent.reflection.ReflectionAnalysis;
import org.example.ggbot.agent.reflection.ReflectionType;
import org.example.ggbot.planner.Plan;

public interface ReplanStrategy {

    ReflectionType supports();

    Plan apply(AgentState state, ReflectionAnalysis reflection);
}
