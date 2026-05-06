package org.example.ggbot.tool.impl;

import java.util.Map;
import lombok.Data;
import org.example.ggbot.agent.AgentContext;
import org.example.ggbot.ai.ContextAwareChatService;
import org.example.ggbot.prompt.ClasspathPromptRepository;
import org.example.ggbot.tool.ToolName;
import org.example.ggbot.tool.ToolResult;
import org.example.ggbot.tool.model.DocumentArtifact;
import org.example.ggbot.tool.support.PromptDetailAnalyzer;
import org.example.ggbot.tool.support.PromptDetailLevel;
import org.example.ggbot.tool.support.ResultQualityEvaluator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@Data
public class GenerateDocTool {

    private static final String MARKDOWN_TEMPLATE_NAME = "generate-doc-markdown-template.txt";
    private static final String SYSTEM_PROMPT_NAME = "generate-doc-system-prompt.txt";
    private static final String USER_PROMPT_NAME = "generate-doc-user-prompt.txt";
    private static final String RETRY_PROMPT_NAME = "generate-doc-retry-prompt.txt";

    private final ContextAwareChatService chatService;
    private final ClasspathPromptRepository promptRepository;
    private final PromptDetailAnalyzer promptDetailAnalyzer;
    private final ResultQualityEvaluator resultQualityEvaluator;

    @Autowired
    public GenerateDocTool(
            ContextAwareChatService chatService,
            ClasspathPromptRepository promptRepository,
            PromptDetailAnalyzer promptDetailAnalyzer) {
        this(chatService, promptRepository, promptDetailAnalyzer, new ResultQualityEvaluator());
    }

    GenerateDocTool(
            ContextAwareChatService chatService,
            ClasspathPromptRepository promptRepository,
            PromptDetailAnalyzer promptDetailAnalyzer,
            ResultQualityEvaluator resultQualityEvaluator) {
        this.chatService = chatService;
        this.promptRepository = promptRepository;
        this.promptDetailAnalyzer = promptDetailAnalyzer;
        this.resultQualityEvaluator = resultQualityEvaluator;
    }

    @Tool(name = "generateDoc", description = "根据用户需求生成 Markdown 方案文档")
    public DocumentArtifact generateDoc(@ToolParam(description = "用户当前的方案文档需求") String prompt) {
        return generateDocument(prompt, null);
    }

    public ToolResult execute(String prompt, AgentContext context, Map<String, Object> parameters) {
        DocumentArtifact artifact = generateDocument(prompt, context);
        return new ToolResult(ToolName.GENERATE_DOC, true, "已生成主题相关 Markdown 文档", artifact);
    }

    private DocumentArtifact generateDocument(String prompt, AgentContext context) {
        String title = inferTitle(prompt);
        PromptDetailLevel detailLevel = java.util.Optional.ofNullable(promptDetailAnalyzer.analyze(prompt))
                .orElse(PromptDetailLevel.NORMAL);
        if (!chatService.isAvailable()) {
            return new DocumentArtifact(title, promptRepository.load(MARKDOWN_TEMPLATE_NAME, Map.of(
                    "title", title,
                    "prompt", prompt == null ? "" : prompt
            )));
        }
        String systemPrompt = promptRepository.load(SYSTEM_PROMPT_NAME);
        String userPrompt = promptRepository.load(USER_PROMPT_NAME, Map.of(
                "title", title,
                "prompt", prompt == null ? "" : prompt,
                "promptDetailLevel", detailLevel.name()
        ));
        String markdown = generateWithRetry(systemPrompt, userPrompt, detailLevel, prompt == null ? 0 : prompt.length(), context);
        return new DocumentArtifact(title, markdown);
    }

    private String generateWithRetry(
            String systemPrompt,
            String userPrompt,
            PromptDetailLevel detailLevel,
            int originalInputLength,
            AgentContext context) {
        String currentPrompt = userPrompt;
        String reply = "";
        for (int attempt = 0; attempt < 2; attempt++) {
            reply = chatService.chat(systemPrompt, currentPrompt, context);
            var issues = resultQualityEvaluator.evaluateText(reply, detailLevel, originalInputLength);
            if (issues.isEmpty()) {
                return reply;
            }
            currentPrompt = promptRepository.load(RETRY_PROMPT_NAME, Map.of(
                    "originalPrompt", userPrompt,
                    "issues", "- " + String.join("\n- ", issues)
            ));
        }
        return reply;
    }

    private String inferTitle(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return "主题文档";
        }
        String normalized = prompt
                .replace("请生成一份关于", "")
                .replace("请生成", "")
                .replace("先生成一份关于", "")
                .replace("文档", "")
                .replace("Markdown", "")
                .trim();
        if (normalized.endsWith("的")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        if (normalized.isBlank()) {
            return "主题文档";
        }
        return normalized;
    }
}
