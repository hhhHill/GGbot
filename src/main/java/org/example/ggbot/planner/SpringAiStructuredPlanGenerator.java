package org.example.ggbot.planner;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ggbot.ai.SpringAiChatService;
import org.example.ggbot.prompt.ClasspathPromptRepository;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpringAiStructuredPlanGenerator implements StructuredPlanGenerator {

    private static final String SYSTEM_PROMPT_NAME = "planner-system-prompt.txt";
    private static final String USER_PROMPT_NAME = "planner-user-prompt.txt";

    private final SpringAiChatService chatService;
    private final ClasspathPromptRepository promptRepository;

    @Override
    public Optional<String> generate(PlannerContext context) {
        if (!chatService.isAvailable()) {
            log.info("Structured planner disabled because chat model is unavailable.");
            return Optional.empty();
        }

        log.info("Structured planner invoking LLM for input: {}", context.userInput());
        String content = chatService.chat(
                promptRepository.load(SYSTEM_PROMPT_NAME),
                promptRepository.load(USER_PROMPT_NAME, java.util.Map.of(
                        "userInput", safe(context.userInput()),
                        "conversationContext", safe(context.conversationContext())
                ))
        );
        if (content == null || content.isBlank()) {
            log.warn("Structured planner returned empty content.");
            return Optional.empty();
        }
        return Optional.of(content);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "(empty)" : value;
    }
}
