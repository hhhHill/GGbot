package org.example.ggbot.tool.impl;

import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.tool.ToolName;
import org.example.ggbot.tool.ToolResult;
import org.example.ggbot.agent.AgentContext;
import org.example.ggbot.tool.model.PptArtifact;
import org.example.ggbot.tool.model.Slide;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@Data
@RequiredArgsConstructor
public class GeneratePptTool {

    @Tool(name = "generatePpt", description = "根据用户需求生成 5 页结构化 PPT 大纲")
    public PptArtifact generatePpt(@ToolParam(description = "用户当前的 PPT 生成需求") String prompt) {
        return new PptArtifact(
                "汇报演示稿",
                List.of(
                        new Slide(1, "封面", List.of("主题：" + prompt, "场景：项目汇报")),
                        new Slide(2, "背景与目标", List.of("说明业务背景", "定义本次目标")),
                        new Slide(3, "方案设计", List.of("核心模块拆分", "流程与能力说明")),
                        new Slide(4, "实施计划", List.of("阶段计划", "资源与风险")),
                        new Slide(5, "结论与下一步", List.of("关键结论", "建议动作"))
                )
        );
    }

    public ToolResult execute(String prompt, AgentContext context, Map<String, Object> parameters) {
        PptArtifact artifact = generatePpt(prompt);
        return new ToolResult(toolName(), true, "已生成 5 页 PPT 大纲", artifact);
    }

    private ToolName toolName() {
        return ToolName.GENERATE_PPT;
    }
}
