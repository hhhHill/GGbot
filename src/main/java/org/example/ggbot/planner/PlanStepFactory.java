package org.example.ggbot.planner;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.tool.ToolName;
import org.springframework.stereotype.Component;

/**
 * 执行步骤工厂。
 *
 * <p>它负责把最终的 `PlanningSignal` 转换成 `PlanStep` 列表，
 * 也就是把“识别用户要什么”进一步变成“后续该调用哪些工具”。
 */
@Component
@Data
@RequiredArgsConstructor
public class PlanStepFactory {

    /**
     * 根据规划信号生成执行步骤。
     */
    public List<PlanStep> createSteps(PlanningSignal signal, String input) {
        List<PlanStep> steps = new ArrayList<>();
        if (signal.isNeedDoc()) {
            steps.add(new PlanStep(stepId(), ToolName.GENERATE_DOC, "生成方案文档", input));
        }
        if (signal.isNeedPpt()) {
            steps.add(new PlanStep(stepId(), ToolName.GENERATE_PPT, "生成汇报 PPT 大纲", input));
        }
        if (!signal.isNeedDoc() && !signal.isNeedPpt()) {
            steps.add(new PlanStep(stepId(), ToolName.SUMMARIZE, "生成直接回复", input));
        }
        return steps;
    }

    private String stepId() {
        return "step-" + UUID.randomUUID().toString().replace("-", "");
    }
}
