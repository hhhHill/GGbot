package org.example.ggbot.tool.ppt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.example.ggbot.ai.ReliableChatService;
import org.example.ggbot.prompt.ClasspathPromptRepository;
import org.junit.jupiter.api.Test;

class SpringAiPptSpecGenerationClientTest {

    @Test
    void shouldLoadSystemPromptFromRepository() {
        ReliableChatService chatService = mock(ReliableChatService.class);
        ClasspathPromptRepository promptRepository = mock(ClasspathPromptRepository.class);
        when(promptRepository.load("ppt-spec-system-prompt.txt")).thenReturn("ppt-system");
        when(chatService.chat("ppt-system", "生成一个 AI 趋势 PPT")).thenReturn("{\"title\":\"AI 趋势\"}");

        SpringAiPptSpecGenerationClient client = new SpringAiPptSpecGenerationClient(chatService, promptRepository);

        String result = client.generate("生成一个 AI 趋势 PPT");

        assertThat(result).isEqualTo("{\"title\":\"AI 趋势\"}");
        verify(promptRepository).load("ppt-spec-system-prompt.txt");
    }
}
