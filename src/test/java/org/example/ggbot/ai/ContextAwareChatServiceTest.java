package org.example.ggbot.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.example.ggbot.agent.AgentChannel;
import org.example.ggbot.agent.AgentContext;
import org.example.ggbot.prompt.ClasspathPromptRepository;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

class ContextAwareChatServiceTest {

    @Test
    void shouldBuildPromptAndWriteBackMemoryAfterChat() {
        ReliableChatService delegate = mock(ReliableChatService.class);
        MemoryManager memoryManager = new MemoryManager(new ClasspathPromptRepository());
        ContextAwareChatService service = new ContextAwareChatService(delegate, memoryManager);
        AgentContext context = context(List.of(
                "USER: 你知道杨艺不？",
                "ASSISTANT: 不确定你指的是哪个杨艺。"
        ));
        when(delegate.chat(anyString(), anyString())).thenReturn("她是教育界冉冉升起的新星");

        String reply = service.chat("system", "她是谁", context);

        assertThat(reply).isEqualTo("她是教育界冉冉升起的新星");
        verify(delegate).chat(
                "system",
                """
                === 最近对话 ===
                USER: 你知道杨艺不？
                ASSISTANT: 不确定你指的是哪个杨艺。
                
                === 当前问题 ===
                USER: 她是谁"""
        );
        assertThat(context.getConversationHistory()).containsExactly(
                "USER: 你知道杨艺不？",
                "ASSISTANT: 不确定你指的是哪个杨艺。",
                "USER: 她是谁",
                "ASSISTANT: 她是教育界冉冉升起的新星"
        );
    }

    @Test
    void shouldBuildPromptAndWriteBackMemoryAfterStreamCompletes() {
        ReliableChatService delegate = mock(ReliableChatService.class);
        MemoryManager memoryManager = new MemoryManager(new ClasspathPromptRepository());
        ContextAwareChatService service = new ContextAwareChatService(delegate, memoryManager);
        AgentContext context = context(List.of("USER: 你好", "ASSISTANT: 你好"));
        when(delegate.stream(anyString(), anyString())).thenReturn(Flux.just("她", "很好"));

        String reply = service.stream("system", "她怎么样", context).collectList().map(chunks -> String.join("", chunks)).block();

        assertThat(reply).isEqualTo("她很好");
        assertThat(context.getConversationHistory()).containsExactly(
                "USER: 你好",
                "ASSISTANT: 你好",
                "USER: 她怎么样",
                "ASSISTANT: 她很好"
        );
    }

    private AgentContext context(List<String> history) {
        return new AgentContext("task-1", "conversation-1", "user-1", AgentChannel.WEB, history, Map.of());
    }
}
