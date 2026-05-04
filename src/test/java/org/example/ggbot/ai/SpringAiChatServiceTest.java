package org.example.ggbot.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import reactor.core.publisher.Flux;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

class SpringAiChatServiceTest {

    @Test
    void shouldReturnFallbackWhenChatClientIsUnavailable() {
        SpringAiChatService service = new SpringAiChatService(Optional.empty());

        String response = service.chat("test prompt");

        assertThat(response).contains("Spring AI chat model is not configured");
        assertThat(service.isAvailable()).isFalse();
    }

    @Test
    void shouldDelegateToChatClientWhenConfigured() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("generated reply");

        SpringAiChatService service = new SpringAiChatService(Optional.of(chatClient));

        String response = service.chat("system prompt", "user prompt");

        assertThat(response).isEqualTo("generated reply");
        assertThat(service.isAvailable()).isTrue();
    }

    @Test
    void shouldStreamContentFromChatClientWhenConfigured() {
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.StreamResponseSpec streamResponseSpec = mock(ChatClient.StreamResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.stream()).thenReturn(streamResponseSpec);
        when(streamResponseSpec.content()).thenReturn(Flux.just("hel", "lo"));

        SpringAiChatService service = new SpringAiChatService(Optional.of(chatClient));

        String response = service.stream("system prompt", "user prompt").collectList().map(parts -> String.join("", parts)).block();

        assertThat(response).isEqualTo("hello");
        verify(chatClient, times(1)).prompt();
    }
}
