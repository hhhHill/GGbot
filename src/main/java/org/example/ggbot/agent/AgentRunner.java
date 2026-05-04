package org.example.ggbot.agent;

import org.example.ggbot.agent.graph.AgentGraphProperties;
import org.example.ggbot.agent.runner.LangGraphAgentRunner;
import org.springframework.stereotype.Component;

@Component
public class AgentRunner {

    private final LangGraphAgentRunner langGraphAgentRunner;
    private final AgentGraphProperties properties;

    public AgentRunner(LangGraphAgentRunner langGraphAgentRunner, AgentGraphProperties properties) {
        this.langGraphAgentRunner = langGraphAgentRunner;
        this.properties = properties;
    }

    public AgentState run(AgentState initialState) {
        return langGraphAgentRunner.run(initialState, properties.getMaxIterations());
    }
}
