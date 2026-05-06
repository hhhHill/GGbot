package org.example.ggbot.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.example.ggbot.prompt.ClasspathPromptRepository;
import org.junit.jupiter.api.Test;

class LlmStartupProbeServiceTest {

    @Test
    void shouldMarkProbeSuccessfulWhenChatServiceResponds() {
        SpringAiChatService chatService = mock(SpringAiChatService.class);
        ClasspathPromptRepository promptRepository = mock(ClasspathPromptRepository.class);
        when(chatService.isAvailable()).thenReturn(true);
        when(promptRepository.load("llm-startup-probe-system-prompt.txt")).thenReturn("Reply with OK only.");
        when(promptRepository.load("llm-startup-probe-user-prompt.txt")).thenReturn("OK");
        when(chatService.chat("Reply with OK only.", "OK")).thenReturn("OK");

        LlmStartupProbeService probeService = new LlmStartupProbeService(chatService, promptRepository);
        probeService.runStartupProbe();

        assertThat(probeService.isLlmConfigured()).isTrue();
        assertThat(probeService.isLlmReachable()).isTrue();
        assertThat(probeService.getLlmMessage()).contains("成功");
        verify(promptRepository).load("llm-startup-probe-system-prompt.txt");
        verify(promptRepository).load("llm-startup-probe-user-prompt.txt");
    }

    @Test
    void shouldMarkProbeFailedWhenChatServiceThrows() {
        SpringAiChatService chatService = mock(SpringAiChatService.class);
        ClasspathPromptRepository promptRepository = mock(ClasspathPromptRepository.class);
        when(chatService.isAvailable()).thenReturn(true);
        when(promptRepository.load("llm-startup-probe-system-prompt.txt")).thenReturn("Reply with OK only.");
        when(promptRepository.load("llm-startup-probe-user-prompt.txt")).thenReturn("OK");
        doThrow(new RuntimeException("401 Unauthorized"))
                .when(chatService).chat("Reply with OK only.", "OK");

        LlmStartupProbeService probeService = new LlmStartupProbeService(chatService, promptRepository);
        probeService.runStartupProbe();

        assertThat(probeService.isLlmConfigured()).isTrue();
        assertThat(probeService.isLlmReachable()).isFalse();
        assertThat(probeService.getLlmMessage()).contains("401 Unauthorized");
    }

    @Test
    void shouldMarkProbeSkippedWhenChatServiceIsUnavailable() {
        SpringAiChatService chatService = mock(SpringAiChatService.class);
        ClasspathPromptRepository promptRepository = mock(ClasspathPromptRepository.class);
        when(chatService.isAvailable()).thenReturn(false);

        LlmStartupProbeService probeService = new LlmStartupProbeService(chatService, promptRepository);
        probeService.runStartupProbe();

        assertThat(probeService.isLlmConfigured()).isFalse();
        assertThat(probeService.isLlmReachable()).isFalse();
        assertThat(probeService.getLlmMessage()).contains("未配置");
    }
}
