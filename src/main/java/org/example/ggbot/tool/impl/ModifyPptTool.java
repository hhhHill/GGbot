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
public class ModifyPptTool {

    @Tool(name = "modifyPpt", description = "根据补充要求调整现有 PPT 大纲")
    public PptArtifact modifyPpt(@ToolParam(description = "用户给出的 PPT 修改要求") String prompt) {
        return new PptArtifact(
                "修改后的演示稿",
                List.of(
                        new Slide(1, "封面（已调整）", List.of("主题更聚焦", "修改依据：" + prompt)),
                        new Slide(2, "背景补充", List.of("增加业务背景", "补充目标指标")),
                        new Slide(3, "方案细化", List.of("补充实施细节", "增加流程说明")),
                        new Slide(4, "风险与计划", List.of("强调关键风险", "说明应对策略")),
                        new Slide(5, "行动建议", List.of("下一步动作", "责任分工"))
                )
        );
    }

    public ToolResult execute(String prompt, AgentContext context, Map<String, Object> parameters) {
        PptArtifact artifact = modifyPpt(prompt);
        return new ToolResult(toolName(), true, "已生成修改后的 PPT 草稿", artifact);
    }

    private ToolName toolName() {
        return ToolName.MODIFY_PPT;
    }
}
