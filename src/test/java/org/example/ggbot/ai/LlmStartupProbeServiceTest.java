package org.example.ggbot.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class LlmStartupProbeServiceTest {

    @Test
    void shouldMarkProbeSuccessfulWhenChatServiceResponds() {
        SpringAiChatService chatService = mock(SpringAiChatService.class);
        when(chatService.isAvailable()).thenReturn(true);
        when(chatService.chat(anyString(), anyString())).thenReturn("OK");

        LlmStartupProbeService probeService = new LlmStartupProbeService(chatService);
        probeService.runStartupProbe();

        assertThat(probeService.isLlmConfigured()).isTrue();
        assertThat(probeService.isLlmReachable()).isTrue();
        assertThat(probeService.getLlmMessage()).contains("成功");
    }

    @Test
    void shouldMarkProbeFailedWhenChatServiceThrows() {
        SpringAiChatService chatService = mock(SpringAiChatService.class);
        when(chatService.isAvailable()).thenReturn(true);
        doThrow(new RuntimeException("401 Unauthorized"))
                .when(chatService).chat(anyString(), anyString());

        LlmStartupProbeService probeService = new LlmStartupProbeService(chatService);
        probeService.runStartupProbe();

        assertThat(probeService.isLlmConfigured()).isTrue();
        assertThat(probeService.isLlmReachable()).isFalse();
        assertThat(probeService.getLlmMessage()).contains("401 Unauthorized");
    }

    @Test
    void shouldMarkProbeSkippedWhenChatServiceIsUnavailable() {
        SpringAiChatService chatService = mock(SpringAiChatService.class);
        when(chatService.isAvailable()).thenReturn(false);

        LlmStartupProbeService probeService = new LlmStartupProbeService(chatService);
        probeService.runStartupProbe();

        assertThat(probeService.isLlmConfigured()).isFalse();
        assertThat(probeService.isLlmReachable()).isFalse();
        assertThat(probeService.getLlmMessage()).contains("未配置");
    }
}
