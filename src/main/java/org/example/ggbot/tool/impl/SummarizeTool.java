package org.example.ggbot.tool.impl;

import java.util.Map;
import lombok.Data;
import org.example.ggbot.ai.ReliableChatService;
import org.example.ggbot.tool.ToolName;
import org.example.ggbot.tool.ToolResult;
import org.example.ggbot.agent.AgentContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@Data
public class SummarizeTool {

    private static final String CHAT_SYSTEM_PROMPT = """
            你是 GGbot 的 Web MVP 对话助手。
            你的回答应当直接、简洁、可执行。
            如果用户只是普通聊天或提问，请直接回答，不要假装生成文档或 PPT。
            """;

    private final ReliableChatService chatService;

    public SummarizeTool(ReliableChatService chatService) {
        this.chatService = chatService;
    }

    @Tool(name = "summarize", description = "对当前用户需求生成简要总结和建议")
    public String summarize(@ToolParam(description = "用户当前输入或待总结内容") String prompt) {
        return generateReply(prompt);
    }

    public ToolResult execute(String prompt, AgentContext context, Map<String, Object> parameters) {
        String summary = generateReply(prompt);
        return new ToolResult(ToolName.SUMMARIZE, true, summary, null);
    }

    private String generateReply(String prompt) {
        if (!chatService.isAvailable()) {
            return templateReply(prompt);
        }

        return chatService.chat(CHAT_SYSTEM_PROMPT, prompt);
    }

    private String templateReply(String prompt) {
        return "已收到你的需求：" + prompt + "。当前未触发文档或 PPT 生成，先给出简要建议并等待下一步指令。";
    }
}
