package org.example.ggbot.tool.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.example.ggbot.agent.AgentChannel;
import org.example.ggbot.agent.AgentContext;
import org.example.ggbot.ai.ReliableChatService;
import org.example.ggbot.tool.ToolName;
import org.example.ggbot.tool.ToolResult;
import org.junit.jupiter.api.Test;

class SummarizeToolTest {

    @Test
    void shouldDelegateToReliableChatServiceWithSystemPromptWhenAvailable() {
        ReliableChatService chatService = mock(ReliableChatService.class);
        when(chatService.isAvailable()).thenReturn(true);
        when(chatService.chat(
                """
                你是 GGbot 的 Web MVP 对话助手。
                你的回答应当直接、简洁、可执行。
                如果用户只是普通聊天或提问，请直接回答，不要假装生成文档或 PPT。
                """,
                "你好，请介绍一下你自己"))
                .thenReturn("这是模型回复");
        SummarizeTool tool = new SummarizeTool(chatService);

        ToolResult result = tool.execute("你好，请介绍一下你自己", context(), Map.of());

        assertThat(result.getToolName()).isEqualTo(ToolName.SUMMARIZE);
        assertThat(result.getSummary()).isEqualTo("这是模型回复");
        assertThat(result.getArtifact()).isEqualTo("这是模型回复");
        verify(chatService).isAvailable();
        verify(chatService).chat(
                """
                你是 GGbot 的 Web MVP 对话助手。
                你的回答应当直接、简洁、可执行。
                如果用户只是普通聊天或提问，请直接回答，不要假装生成文档或 PPT。
                """,
                "你好，请介绍一下你自己");
        verifyNoMoreInteractions(chatService);
    }

    @Test
    void shouldFallbackToTemplateWithoutCallingReliableChatServiceWhenUnavailable() {
        ReliableChatService chatService = mock(ReliableChatService.class);
        when(chatService.isAvailable()).thenReturn(false);
        SummarizeTool tool = new SummarizeTool(chatService);

        ToolResult result = tool.execute("帮我总结一下", context(), Map.of());

        assertThat(result.getToolName()).isEqualTo(ToolName.SUMMARIZE);
        assertThat(result.getSummary()).contains("已收到你的需求：帮我总结一下");
        verify(chatService).isAvailable();
        verifyNoMoreInteractions(chatService);
    }

    private AgentContext context() {
        return new AgentContext(
                "task-1",
                "conversation-1",
                "user-1",
                AgentChannel.WEB,
                List.of("USER: 你好"),
                Map.of()
        );
    }
}
