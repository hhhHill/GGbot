package org.example.ggbot.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.example.ggbot.tool.impl.GenerateDocTool;
import org.example.ggbot.tool.impl.GeneratePptTool;
import org.example.ggbot.tool.impl.ModifyPptTool;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallbackProvider;

class AppConfigTest {

    @Test
    void shouldExposeOnlyPureToolCallbacks() {
        AppConfig appConfig = new AppConfig();

        ToolCallbackProvider provider = appConfig.toolCallbackProvider(
                new GenerateDocTool(),
                new GeneratePptTool(),
                new ModifyPptTool()
        );

        Set<String> toolNames = Arrays.stream(provider.getToolCallbacks())
                .map(callback -> callback.getToolDefinition().name())
                .collect(Collectors.toSet());

        assertThat(toolNames).containsExactlyInAnyOrder("generateDoc", "generatePpt", "modifyPpt");
        assertThat(toolNames).doesNotContain("summarize");
    }
}
