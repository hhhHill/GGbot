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
    private String goal = "";
    private DeliverableType deliverableType = DeliverableType.UNKNOWN;
    private boolean needClarification;
    private boolean multiStep;
    private boolean needDoc;
    private boolean needPpt;
    private final List<PlanStep> steps = new ArrayList<>();

    public void addStep(PlanStep step) {
        this.steps.add(step);
        this.multiStep = this.steps.size() > 1;
    }

    public void appendSteps(List<PlanStep> steps) {
        this.steps.addAll(steps);
        this.multiStep = this.steps.size() > 1;
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

    public void syncLegacyFlags() {
        this.needDoc = deliverableType == DeliverableType.DOC || deliverableType == DeliverableType.MIXED
                || steps.stream().anyMatch(step -> step.getType() == StepType.GENERATE_DOC);
        this.needPpt = deliverableType == DeliverableType.PPT || deliverableType == DeliverableType.MIXED
                || steps.stream().anyMatch(step -> step.getType() == StepType.GENERATE_PPT);
        this.multiStep = this.steps.size() > 1 || this.multiStep;
    }
}
