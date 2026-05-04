package org.example.ggbot.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.example.ggbot.agent.graph.AgentGraphProperties;
import org.example.ggbot.agent.runner.LangGraphAgentRunner;
import org.junit.jupiter.api.Test;

class AgentRunnerTest {

    @Test
    void shouldDelegateToLangGraphRunnerWithConfiguredIterations() {
        LangGraphAgentRunner langGraphAgentRunner = mock(LangGraphAgentRunner.class);
        AgentGraphProperties properties = new AgentGraphProperties();
        properties.setMaxIterations(7);
        AgentRunner runner = new AgentRunner(langGraphAgentRunner, properties);

        AgentState initialState = initialState("帮我总结一下");
        AgentState finalState = initialState("这是最终结果");
        when(langGraphAgentRunner.run(initialState, 7)).thenReturn(finalState);

        AgentState result = runner.run(initialState);

        assertThat(result).isSameAs(finalState);
        verify(langGraphAgentRunner).run(initialState, 7);
    }

    private AgentState initialState(String userInput) {
        return AgentState.initialize(
                "task-1",
                new AgentRequest(
                        "conversation-1",
                        "user-1",
                        userInput,
                        AgentChannel.WEB,
                        null,
                        "conversation-1",
                        Map.of()
                ),
                List.of()
        );
    }
}
