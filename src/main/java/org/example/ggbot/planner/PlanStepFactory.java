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
        if (signal.isNeedClarification()) {
            steps.add(new PlanStep(
                    stepId(),
                    StepType.CLARIFY,
                    null,
                    signal.getClarificationQuestion(),
                    signal.getClarificationQuestion(),
                    List.of(),
                    List.of(),
                    "用户补充的任务目标、主题和交付要求"
            ));
            return steps;
        }
        if (signal.isNeedDoc()) {
            steps.add(new PlanStep(
                    stepId(),
                    StepType.GENERATE_DOC,
                    ToolName.GENERATE_DOC,
                    "生成方案文档",
                    input,
                    List.of(),
                    List.of(),
                    "一份结构化文档草稿"
            ));
        }
        if (signal.isNeedPpt()) {
            List<String> dependsOn = steps.stream()
                    .filter(step -> step.getType() == StepType.GENERATE_DOC)
                    .map(PlanStep::getStepId)
                    .toList();
            steps.add(new PlanStep(
                    stepId(),
                    StepType.GENERATE_PPT,
                    ToolName.GENERATE_PPT,
                    "生成汇报 PPT 大纲",
                    input,
                    dependsOn,
                    dependsOn,
                    "一份汇报 PPT 大纲"
            ));
        }
        if (!signal.isNeedDoc() && !signal.isNeedPpt()) {
            steps.add(new PlanStep(
                    stepId(),
                    StepType.SUMMARIZE,
                    ToolName.SUMMARIZE,
                    "生成直接回复",
                    input,
                    List.of(),
                    List.of(),
                    "一段总结答复"
            ));
        }
        return steps;
    }

    private String stepId() {
        return "step-" + UUID.randomUUID().toString().replace("-", "");
    }
}
