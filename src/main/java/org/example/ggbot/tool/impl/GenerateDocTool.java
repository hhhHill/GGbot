package org.example.ggbot.tool.impl;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.tool.Tool;
import org.example.ggbot.tool.ToolName;
import org.example.ggbot.tool.ToolRequest;
import org.example.ggbot.tool.ToolResult;
import org.example.ggbot.tool.model.DocumentArtifact;
import org.springframework.stereotype.Component;

@Component
@Data
@RequiredArgsConstructor
public class GenerateDocTool implements Tool {

    @Override
    public ToolName toolName() {
        return ToolName.GENERATE_DOC;
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        String markdown = """
                # 项目方案文档

                ## 目标
                %s

                ## 核心方案
                - 明确业务目标和用户价值
                - 拆分关键模块与交付物
                - 规划阶段性里程碑与验收标准

                ## 实施建议
                1. 先完成 MVP 范围定义
                2. 明确接口、数据和协作流程
                3. 用可量化指标验证交付效果
                """.formatted(request.getPrompt());
        DocumentArtifact artifact = new DocumentArtifact("方案文档", markdown);
        return new ToolResult(toolName(), true, "已生成 Markdown 方案文档", artifact);
    }
}
