package org.example.ggbot.planner;

import org.example.ggbot.agent.AgentState;

public interface Planner {

    Plan plan(AgentState state);
}
