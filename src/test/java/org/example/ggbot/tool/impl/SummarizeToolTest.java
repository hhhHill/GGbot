package org.example.ggbot.tool.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.example.ggbot.agent.AgentChannel;
import org.example.ggbot.agent.AgentContext;
import org.example.ggbot.ai.SpringAiChatService;
import org.example.ggbot.tool.ToolName;
import org.example.ggbot.tool.ToolResult;
import org.junit.jupiter.api.Test;

class SummarizeToolTest {

    @Test
    void shouldUseLlmReplyWhenChatClientIsAvailable() {
        SpringAiChatService chatService = mock(SpringAiChatService.class);
        when(chatService.isAvailable()).thenReturn(true);
        when(chatService.chat(anyString(), anyString())).thenReturn("这是模型回复");
        SummarizeTool tool = new SummarizeTool(chatService);

        ToolResult result = tool.execute("你好，请介绍一下你自己", context(), Map.of());

        assertThat(result.getToolName()).isEqualTo(ToolName.SUMMARIZE);
        assertThat(result.getSummary()).isEqualTo("这是模型回复");
        assertThat(result.getArtifact()).isEqualTo("这是模型回复");
    }

    @Test
    void shouldFallbackToTemplateWhenChatClientIsUnavailable() {
        SpringAiChatService chatService = mock(SpringAiChatService.class);
        when(chatService.isAvailable()).thenReturn(false);
        SummarizeTool tool = new SummarizeTool(chatService);

        ToolResult result = tool.execute("帮我总结一下", context(), Map.of());

        assertThat(result.getToolName()).isEqualTo(ToolName.SUMMARIZE);
        assertThat(result.getSummary()).contains("已收到你的需求：帮我总结一下");
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
