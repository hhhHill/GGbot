package org.example.ggbot.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.core.env.Environment;

class LlmConfigurationDiagnosticsServiceTest {

    @Test
    void shouldExposeResolvedConfigurationWithMaskedSecrets() {
        Environment environment = mock(Environment.class);
        when(environment.getProperty("spring.ai.openai.base-url", "<null>")).thenReturn("https://ark.cn-beijing.volces.com/api/v3");
        when(environment.getProperty("spring.ai.openai.chat.options.model", "<null>")).thenReturn("ep-20260423222827-6lcn6");
        when(environment.getProperty("spring.ai.openai.api-key", "<null>")).thenReturn("ark-1234567890");
        when(environment.getProperty("SPRING_AI_OPENAI_BASE_URL", "<null>")).thenReturn("https://ark.cn-beijing.volces.com/api/v3");
        when(environment.getProperty("SPRING_AI_OPENAI_MODEL", "<null>")).thenReturn("ep-20260423222827-6lcn6");
        when(environment.getProperty("SPRING_AI_OPENAI_API_KEY", "<null>")).thenReturn("ark-1234567890");

        LlmConfigurationDiagnosticsService service = new LlmConfigurationDiagnosticsService(
                environment,
                java.util.Optional.empty(),
                java.util.Optional.empty(),
                java.util.Optional.empty()
        );
        Map<String, String> values = service.resolvedConfiguration();

        assertThat(values.get("spring.ai.openai.api-key")).isEqualTo("ark-****7890");
        assertThat(values.get("SPRING_AI_OPENAI_API_KEY")).isEqualTo("ark-****7890");
    }

    @Test
    void shouldReportNullWhenPropertyIsMissing() {
        Environment environment = mock(Environment.class);
        when(environment.getProperty("spring.ai.openai.base-url", "<null>")).thenReturn("<null>");
        when(environment.getProperty("spring.ai.openai.chat.options.model", "<null>")).thenReturn("<null>");
        when(environment.getProperty("spring.ai.openai.api-key", "<null>")).thenReturn("<null>");
        when(environment.getProperty("SPRING_AI_OPENAI_BASE_URL", "<null>")).thenReturn("<null>");
        when(environment.getProperty("SPRING_AI_OPENAI_MODEL", "<null>")).thenReturn("<null>");
        when(environment.getProperty("SPRING_AI_OPENAI_API_KEY", "<null>")).thenReturn("<null>");

        LlmConfigurationDiagnosticsService service = new LlmConfigurationDiagnosticsService(
                environment,
                java.util.Optional.empty(),
                java.util.Optional.empty(),
                java.util.Optional.empty()
        );
        Map<String, String> values = service.resolvedConfiguration();

        assertThat(values.get("spring.ai.openai.api-key")).isEqualTo("<null>");
    }

    @Test
    void shouldTreatUnresolvedPlaceholderAsNullValue() {
        Environment environment = mock(Environment.class);
        when(environment.getProperty("spring.ai.openai.base-url", "<null>")).thenReturn("https://example.test");
        when(environment.getProperty("spring.ai.openai.chat.options.model", "<null>"))
                .thenThrow(new IllegalArgumentException("Could not resolve placeholder SPRING_AI_OPENAI_MODEL"));
        when(environment.getProperty("spring.ai.openai.api-key", "<null>")).thenReturn("<null>");
        when(environment.getProperty("SPRING_AI_OPENAI_BASE_URL", "<null>")).thenReturn("<null>");
        when(environment.getProperty("SPRING_AI_OPENAI_MODEL", "<null>")).thenReturn("<null>");
        when(environment.getProperty("SPRING_AI_OPENAI_API_KEY", "<null>")).thenReturn("<null>");

        LlmConfigurationDiagnosticsService service = new LlmConfigurationDiagnosticsService(
                environment,
                java.util.Optional.empty(),
                java.util.Optional.empty(),
                java.util.Optional.empty()
        );

        Map<String, String> values = service.resolvedConfiguration();

        assertThat(values.get("spring.ai.openai.chat.options.model")).isEqualTo("<null>");
    }

    @Test
    void shouldExposeResolvedBeanDiagnostics() {
        Environment environment = mock(Environment.class);
        ChatModel chatModel = mock(ChatModel.class);
        ChatClient chatClient = mock(ChatClient.class);

        LlmConfigurationDiagnosticsService service = new LlmConfigurationDiagnosticsService(
                environment,
                java.util.Optional.of(chatModel),
                java.util.Optional.of(chatClient),
                java.util.Optional.empty()
        );

        Map<String, String> values = service.resolvedBeanDiagnostics();

        assertThat(values.get("ChatModel")).contains(chatModel.getClass().getName());
        assertThat(values.get("ChatClient")).contains(chatClient.getClass().getName());
        assertThat(values.get("ChatClient.Builder")).isEqualTo("<missing>");
    }
}
