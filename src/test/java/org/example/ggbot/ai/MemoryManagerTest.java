package org.example.ggbot.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.example.ggbot.agent.AgentChannel;
import org.example.ggbot.agent.AgentContext;
import org.junit.jupiter.api.Test;

class MemoryManagerTest {

    private final MemoryManager memoryManager = new MemoryManager();

    @Test
    void shouldBuildPromptWithCurrentQuestionOnlyWhenHistoryIsEmpty() {
        String fullPrompt = memoryManager.buildPrompt("她是谁", context(List.of()));

        assertThat(fullPrompt).isEqualTo(
                """
                === 当前问题 ===
                USER: 她是谁""");
    }

    @Test
    void shouldKeepAllHistoryWhenLessThanFiveRounds() {
        String fullPrompt = memoryManager.buildPrompt(
                "她是谁",
                context(List.of(
                        "USER: 你知道杨艺不？",
                        "ASSISTANT: 不确定你指的是哪个杨艺。"
                )));

        assertThat(fullPrompt).isEqualTo(
                """
                === 最近对话 ===
                USER: 你知道杨艺不？
                ASSISTANT: 不确定你指的是哪个杨艺。
                
                === 当前问题 ===
                USER: 她是谁""");
    }

    @Test
    void shouldKeepOnlyRecentFiveRounds() {
        List<String> history = List.of(
                "USER: u1", "ASSISTANT: a1",
                "USER: u2", "ASSISTANT: a2",
                "USER: u3", "ASSISTANT: a3",
                "USER: u4", "ASSISTANT: a4",
                "USER: u5", "ASSISTANT: a5",
                "USER: u6", "ASSISTANT: a6"
        );

        List<String> recentMessages = memoryManager.getRecentMessages(history);

        assertThat(recentMessages).containsExactly(
                "USER: u2", "ASSISTANT: a2",
                "USER: u3", "ASSISTANT: a3",
                "USER: u4", "ASSISTANT: a4",
                "USER: u5", "ASSISTANT: a5",
                "USER: u6", "ASSISTANT: a6"
        );
    }

    @Test
    void shouldAvoidDuplicateConsecutiveMessagesWhenWritingMemory() {
        AgentContext context = context(List.of("USER: 你好"));

        memoryManager.addUserMessage(context, "你好");
        memoryManager.addAssistantMessage(context, "收到");
        memoryManager.addAssistantMessage(context, "收到");

        assertThat(context.getConversationHistory()).containsExactly(
                "USER: 你好",
                "ASSISTANT: 收到"
        );
    }

    private AgentContext context(List<String> history) {
        return new AgentContext("task-1", "conversation-1", "user-1", AgentChannel.WEB, history, Map.of());
    }
}
