package org.example.ggbot.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.example.ggbot.common.IdGenerator;
import org.example.ggbot.memory.ConversationMemoryService;
import org.example.ggbot.memory.SpringAiConversationMemoryService;
import org.example.ggbot.service.dto.ConversationContext;
import org.example.ggbot.task.InMemoryTaskService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;

class AgentServiceTest {

    @Test
    void shouldPersistUserAndAgentMessagesThroughConversationMemoryService() {
        AgentRunner runner = mock(AgentRunner.class);
        ConversationMemoryService memoryService =
                new SpringAiConversationMemoryService(MessageWindowChatMemory.builder().maxMessages(20).build());
        AgentService agentService = new AgentService(runner, memoryService, new InMemoryTaskService(new IdGenerator()));
        when(runner.run(any(AgentState.class))).thenAnswer(invocation -> {
            AgentState state = invocation.getArgument(0);
            state.setDone(true);
            state.setFinalReply("这是最终回复");
            return state;
        });

        AgentResult result = agentService.handle(new AgentRequest(
                "conversation-1",
                "user-1",
                "帮我总结一下",
                AgentChannel.WEB,
                null,
                "conversation-1",
                Map.of()
        ));

        assertThat(result.getReplyText()).isEqualTo("这是最终回复");
        assertThat(result.getIntentType()).isNull();
        assertThat(memoryService.getConversationHistory("conversation-1")).containsExactly(
                "USER: 帮我总结一下",
                "AGENT: 这是最终回复"
        );
        assertThat(memoryService.getMessages("conversation-1")).hasSize(2);
    }

    @Test
    void shouldReplyUsingPersistentConversationContextWithoutMutatingChatMemory() {
        AgentRunner runner = mock(AgentRunner.class);
        ConversationMemoryService memoryService =
                new SpringAiConversationMemoryService(MessageWindowChatMemory.builder().maxMessages(20).build());
        AgentService agentService = new AgentService(runner, memoryService, new InMemoryTaskService(new IdGenerator()));
        when(runner.run(any(AgentState.class))).thenAnswer(invocation -> {
            AgentState state = invocation.getArgument(0);
            assertThat(state.getMemory()).containsExactly(
                    "USER: hello",
                    "ASSISTANT: hi",
                    "MEMORY: user likes concise answers"
            );
            state.setDone(true);
            state.setFinalReply("持久化上下文回复");
            return state;
        });

        String reply = agentService.replyWithPersistentContext(
                new ConversationContext(
                        1001L,
                        5001L,
                        7001L,
                        List.of("USER: hello", "ASSISTANT: hi"),
                        List.of("user likes concise answers")
                ),
                "继续这个话题"
        );

        assertThat(reply).isEqualTo("持久化上下文回复");
        assertThat(memoryService.getConversationHistory("7001")).isEmpty();
    }
}
