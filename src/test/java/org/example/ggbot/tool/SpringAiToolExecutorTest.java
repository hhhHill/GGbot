package org.example.ggbot.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.example.ggbot.agent.AgentChannel;
import org.example.ggbot.agent.AgentContext;
import org.example.ggbot.ai.SpringAiChatService;
import org.example.ggbot.tool.impl.GenerateDocTool;
import org.example.ggbot.tool.impl.GeneratePptTool;
import org.example.ggbot.tool.impl.ModifyPptTool;
import org.example.ggbot.tool.impl.SummarizeTool;
import org.example.ggbot.tool.model.DocumentArtifact;
import org.junit.jupiter.api.Test;

class SpringAiToolExecutorTest {

    @Test
    void shouldExecuteGenerateDocToolWithoutCustomRegistry() {
        SpringAiChatService chatService = mock(SpringAiChatService.class);
        when(chatService.isAvailable()).thenReturn(false);
        SpringAiToolExecutor executor = new SpringAiToolExecutor(
                new GenerateDocTool(),
                new GeneratePptTool(),
                new ModifyPptTool(),
                new SummarizeTool(chatService)
        );

        ToolResult result = executor.execute(
                ToolName.GENERATE_DOC,
                "帮我整理一个项目方案",
                context(),
                Map.of("iteration", 1)
        );

        assertThat(result.getToolName()).isEqualTo(ToolName.GENERATE_DOC);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getArtifact()).isInstanceOf(DocumentArtifact.class);
    }

    private AgentContext context() {
        return new AgentContext(
                "task-1",
                "conversation-1",
                "user-1",
                AgentChannel.WEB,
                List.of("历史对话"),
                Map.of("source", "test")
        );
    }
}
