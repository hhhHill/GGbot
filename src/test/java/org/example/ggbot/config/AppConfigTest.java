package org.example.ggbot.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.example.ggbot.ai.ContextAwareChatService;
import org.example.ggbot.prompt.ClasspathPromptRepository;
import org.example.ggbot.tool.support.PromptDetailAnalyzer;
import org.example.ggbot.tool.impl.GenerateDocTool;
import org.example.ggbot.tool.impl.ModifyPptTool;
import org.example.ggbot.tool.ppt.PptxRenderer;
import org.example.ggbot.tool.ppt.SemanticPptFallbackGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallbackProvider;

class AppConfigTest {

    @Test
    void shouldExposeOnlyPureToolCallbacks() {
        AppConfig appConfig = new AppConfig();

        ToolCallbackProvider provider = appConfig.toolCallbackProvider(
                new ModifyPptTool(new SemanticPptFallbackGenerator(), new PptxRenderer(), new ClasspathPromptRepository()));

        Set<String> toolNames = Arrays.stream(provider.getToolCallbacks())
                .map(callback -> callback.getToolDefinition().name())
                .collect(Collectors.toSet());

        assertThat(toolNames).containsExactlyInAnyOrder("modifyPpt");
        assertThat(toolNames).doesNotContain("generateDoc");
        assertThat(toolNames).doesNotContain("summarize");
        assertThat(toolNames).doesNotContain("generatePpt");
    }
}
