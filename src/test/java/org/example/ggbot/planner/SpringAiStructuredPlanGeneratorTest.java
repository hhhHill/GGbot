package org.example.ggbot.planner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.example.ggbot.ai.SpringAiChatService;
import org.example.ggbot.prompt.ClasspathPromptRepository;
import org.junit.jupiter.api.Test;

class SpringAiStructuredPlanGeneratorTest {

    @Test
    void shouldLoadPlannerPromptsFromRepository() {
        SpringAiChatService chatService = mock(SpringAiChatService.class);
        ClasspathPromptRepository promptRepository = mock(ClasspathPromptRepository.class);
        when(chatService.isAvailable()).thenReturn(true);
        when(promptRepository.load("planner-system-prompt.txt")).thenReturn("planner-system");
        when(promptRepository.load("planner-user-prompt.txt", java.util.Map.of(
                "userInput", "帮我做个汇报",
                "conversationContext", "最近对话"
        ))).thenReturn("planner-user");
        when(chatService.chat("planner-system", "planner-user")).thenReturn("{\"goal\":\"ok\"}");

        SpringAiStructuredPlanGenerator generator = new SpringAiStructuredPlanGenerator(chatService, promptRepository);

        Optional<String> result = generator.generate(new PlannerContext("帮我做个汇报", "最近对话"));

        assertThat(result).contains("{\"goal\":\"ok\"}");
        verify(promptRepository).load("planner-system-prompt.txt");
        verify(promptRepository).load("planner-user-prompt.txt", java.util.Map.of(
                "userInput", "帮我做个汇报",
                "conversationContext", "最近对话"
        ));
    }
}
