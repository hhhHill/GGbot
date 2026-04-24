package org.example.ggbot.tool.impl;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.tool.Tool;
import org.example.ggbot.tool.ToolName;
import org.example.ggbot.tool.ToolRequest;
import org.example.ggbot.tool.ToolResult;
import org.springframework.stereotype.Component;

@Component
@Data
@RequiredArgsConstructor
public class SummarizeTool implements Tool {

    @Override
    public ToolName toolName() {
        return ToolName.SUMMARIZE;
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        String summary = "已收到你的需求：" + request.getPrompt() + "。当前未触发文档或 PPT 生成，先给出简要建议并等待下一步指令。";
        return new ToolResult(toolName(), true, summary, summary);
    }
}
