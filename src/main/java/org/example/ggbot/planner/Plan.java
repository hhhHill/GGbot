package org.example.ggbot.planner;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * 规划层的最终输出对象。
 *
 * <p>`Plan` 用来描述一次请求最终应该执行什么类型的任务、是否需要文档或 PPT，
 * 以及具体要按什么步骤执行。
 */
@Data
@RequiredArgsConstructor
public class Plan {

    private IntentType intentType = IntentType.CHAT;
    private boolean needDoc;
    private boolean needPpt;
    private final List<PlanStep> steps = new ArrayList<>();

    public void addStep(PlanStep step) {
        this.steps.add(step);
    }

    public void appendSteps(List<PlanStep> steps) {
        this.steps.addAll(steps);
    }

    public List<PlanStep> getPendingSteps() {
        return steps.stream()
                .filter(step -> step.getStatus() == StepStatus.PENDING)
                .toList();
    }

    public boolean hasPendingSteps() {
        return !getPendingSteps().isEmpty();
    }

    public boolean allStepsCompleted() {
        return !steps.isEmpty() && steps.stream().allMatch(step -> step.getStatus() == StepStatus.SUCCESS);
    }
}
