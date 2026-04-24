package org.example.ggbot.tool.impl;

import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.tool.Tool;
import org.example.ggbot.tool.ToolName;
import org.example.ggbot.tool.ToolRequest;
import org.example.ggbot.tool.ToolResult;
import org.example.ggbot.tool.model.PptArtifact;
import org.example.ggbot.tool.model.Slide;
import org.springframework.stereotype.Component;

@Component
@Data
@RequiredArgsConstructor
public class GeneratePptTool implements Tool {

    @Override
    public ToolName toolName() {
        return ToolName.GENERATE_PPT;
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        PptArtifact artifact = new PptArtifact(
                "汇报演示稿",
                List.of(
                        new Slide(1, "封面", List.of("主题：" + request.getPrompt(), "场景：项目汇报")),
                        new Slide(2, "背景与目标", List.of("说明业务背景", "定义本次目标")),
                        new Slide(3, "方案设计", List.of("核心模块拆分", "流程与能力说明")),
                        new Slide(4, "实施计划", List.of("阶段计划", "资源与风险")),
                        new Slide(5, "结论与下一步", List.of("关键结论", "建议动作"))
                )
        );
        return new ToolResult(toolName(), true, "已生成 5 页 PPT 大纲", artifact);
    }
}
