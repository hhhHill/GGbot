package org.example.ggbot.agent.reflection;

import org.example.ggbot.agent.AgentState;
import org.example.ggbot.agent.execution.ExecutionResult;

public interface Reflector {

    ReflectionAnalysis analyze(AgentState state, ExecutionResult result);
}
