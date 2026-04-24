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
public class ModifyPptTool implements Tool {

    @Override
    public ToolName toolName() {
        return ToolName.MODIFY_PPT;
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        PptArtifact artifact = new PptArtifact(
                "修改后的演示稿",
                List.of(
                        new Slide(1, "封面（已调整）", List.of("主题更聚焦", "视觉表达待增强")),
                        new Slide(2, "背景补充", List.of("增加业务背景", "补充目标指标")),
                        new Slide(3, "方案细化", List.of("补充实施细节", "增加流程说明")),
                        new Slide(4, "风险与计划", List.of("强调关键风险", "说明应对策略")),
                        new Slide(5, "行动建议", List.of("下一步动作", "责任分工"))
                )
        );
        return new ToolResult(toolName(), true, "已生成修改后的 PPT 草稿", artifact);
    }
}
