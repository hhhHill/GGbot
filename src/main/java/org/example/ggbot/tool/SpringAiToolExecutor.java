package org.example.ggbot.tool;

import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.agent.AgentContext;
import org.example.ggbot.tool.impl.GenerateDocTool;
import org.example.ggbot.tool.impl.GeneratePptTool;
import org.example.ggbot.tool.impl.ModifyPptTool;
import org.example.ggbot.tool.impl.SummarizeTool;
import org.springframework.stereotype.Component;

/**
 * 工项目内部的工具执行适配层。
 *
 * <p>它负责在当前规则式 Agent 主循环里按 {@link ToolName} 分发工具，
 * 同时让具体工具类保持 Spring AI tool object 形态，方便后续接入 `ChatClient.tools(...)`。
 */
@Component
@Data
@RequiredArgsConstructor
public class SpringAiToolExecutor {

    private final GenerateDocTool generateDocTool;
    private final GeneratePptTool generatePptTool;
    private final ModifyPptTool modifyPptTool;
    private final SummarizeTool summarizeTool;

    public ToolResult execute(ToolName toolName, String prompt, AgentContext context, Map<String, Object> parameters) {
        return switch (toolName) {
            case GENERATE_DOC -> generateDocTool.execute(prompt, context, parameters);
            case GENERATE_PPT -> generatePptTool.execute(prompt, context, parameters);
            case SUMMARIZE -> summarizeTool.execute(prompt, context, parameters);
            case MODIFY_PPT -> modifyPptTool.execute(prompt, context, parameters);
        };
    }
}
