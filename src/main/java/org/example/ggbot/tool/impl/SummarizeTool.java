package org.example.ggbot.tool.impl;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.Data;
import org.example.ggbot.ai.ContextAwareChatService;
import org.example.ggbot.ai.StreamingContextKeys;
import org.example.ggbot.prompt.ClasspathPromptRepository;
import org.example.ggbot.tool.ToolName;
import org.example.ggbot.tool.ToolResult;
import org.example.ggbot.agent.AgentContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@Data
public class SummarizeTool {

    private static final String SYSTEM_PROMPT_NAME = "summarize-system-prompt.txt";

    private final ContextAwareChatService chatService;
    private final ClasspathPromptRepository promptRepository;

    public SummarizeTool(ContextAwareChatService chatService, ClasspathPromptRepository promptRepository) {
        this.chatService = chatService;
        this.promptRepository = promptRepository;
    }

    @Tool(name = "summarize", description = "对当前用户需求生成简要总结和建议")
    public String summarize(@ToolParam(description = "用户当前输入或待总结内容") String prompt) {
        return generateReply(prompt, null);
    }

    public ToolResult execute(String prompt, AgentContext context, Map<String, Object> parameters) {
        String summary = generateReply(prompt, context);
        return new ToolResult(ToolName.SUMMARIZE, true, summary, null);
    }

    private String generateReply(String prompt, AgentContext context) {
        if (!chatService.isAvailable()) {
            return templateReply(prompt);
        }

        Consumer<String> chunkConsumer = streamChunkConsumer(context).orElse(null);
        String systemPrompt = promptRepository.load(SYSTEM_PROMPT_NAME);
        if (chunkConsumer == null) {
            return chatService.chat(systemPrompt, prompt, context);
        }

        try {
            return chatService.stream(systemPrompt, prompt, context)
                    .filter(chunk -> chunk != null && !chunk.isBlank())
                    .doOnNext(chunkConsumer)
                    .collectList()
                    .map(chunks -> String.join("", chunks))
                    .blockOptional()
                    .filter(reply -> !reply.isBlank())
                    .orElseGet(() -> chatService.chat(systemPrompt, prompt, context));
        } catch (RuntimeException ex) {
            return chatService.chat(systemPrompt, prompt, context);
        }
    }

    private String templateReply(String prompt) {
        return "已收到你的需求：" + prompt + "。当前未触发文档或 PPT 生成，先给出简要建议并等待下一步指令。";
    }

    @SuppressWarnings("unchecked")
    private Optional<Consumer<String>> streamChunkConsumer(AgentContext context) {
        if (context == null || context.getMetadata() == null) {
            return Optional.empty();
        }
        Object value = context.getMetadata().get(StreamingContextKeys.STREAM_CHUNK_CONSUMER);
        if (value instanceof Consumer<?> consumer) {
            return Optional.of((Consumer<String>) consumer);
        }
        return Optional.empty();
    }
}
