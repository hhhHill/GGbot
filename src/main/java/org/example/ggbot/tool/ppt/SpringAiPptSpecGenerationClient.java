package org.example.ggbot.tool.ppt;

import org.example.ggbot.ai.ReliableChatService;
import org.example.ggbot.prompt.ClasspathPromptRepository;
import org.springframework.stereotype.Component;

@Component
public class SpringAiPptSpecGenerationClient implements PptSpecGenerationClient {

    private static final String SYSTEM_PROMPT_NAME = "ppt-spec-system-prompt.txt";

    private final ReliableChatService chatService;
    private final ClasspathPromptRepository promptRepository;

    public SpringAiPptSpecGenerationClient(ReliableChatService chatService, ClasspathPromptRepository promptRepository) {
        this.chatService = chatService;
        this.promptRepository = promptRepository;
    }

    @Override
    public String generate(String instruction) {
        return chatService.chat(promptRepository.load(SYSTEM_PROMPT_NAME), instruction);
    }
}
