package org.example.ggbot.tool.impl;

import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.tool.ToolName;
import org.example.ggbot.tool.ToolResult;
import org.example.ggbot.agent.AgentContext;
import org.example.ggbot.tool.model.DocumentArtifact;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@Data
@RequiredArgsConstructor
public class GenerateDocTool {

    @Tool(name = "generateDoc", description = "根据用户需求生成 Markdown 方案文档")
    public DocumentArtifact generateDoc(@ToolParam(description = "用户当前的方案文档需求") String prompt) {
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
                """.formatted(prompt);
        return new DocumentArtifact("方案文档", markdown);
    }

    public ToolResult execute(String prompt, AgentContext context, Map<String, Object> parameters) {
        DocumentArtifact artifact = generateDoc(prompt);
        return new ToolResult(ToolName.GENERATE_DOC, true, "已生成 Markdown 方案文档", artifact);
    }
}
