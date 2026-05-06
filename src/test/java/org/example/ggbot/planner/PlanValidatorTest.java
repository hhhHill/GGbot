package org.example.ggbot.planner;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.example.ggbot.tool.ToolName;
import org.junit.jupiter.api.Test;

class PlanValidatorTest {

    private final PlanValidator validator = new PlanValidator();

    @Test
    void shouldRejectPlanWithEmptySteps() {
        Plan plan = new Plan();
        plan.setGoal("生成 PPT");
        plan.setDeliverableType(DeliverableType.PPT);

        ValidationResult result = validator.validate(plan);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).anyMatch(error -> error.contains("steps"));
    }

    @Test
    void shouldRejectPlanWithCircularDependencies() {
        Plan plan = new Plan();
        plan.setGoal("先总结再生成文档");
        plan.setDeliverableType(DeliverableType.DOC);
        plan.appendSteps(List.of(
                new PlanStep("1", StepType.SUMMARIZE, ToolName.SUMMARIZE, "先总结", "先总结", List.of("2"), List.of("2"), "总结"),
                new PlanStep("2", StepType.GENERATE_DOC, ToolName.GENERATE_DOC, "再生成文档", "再生成文档", List.of("1"), List.of("1"), "文档")
        ));

        ValidationResult result = validator.validate(plan);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).anyMatch(error -> error.contains("circular"));
    }

    @Test
    void shouldRejectClarificationPlanWhenFirstStepIsNotClarify() {
        Plan plan = new Plan();
        plan.setGoal("补充汇报信息");
        plan.setDeliverableType(DeliverableType.UNKNOWN);
        plan.setNeedClarification(true);
        plan.appendSteps(List.of(
                new PlanStep("1", StepType.SUMMARIZE, ToolName.SUMMARIZE, "错误步骤", "错误步骤", List.of(), List.of(), "总结")
        ));

        ValidationResult result = validator.validate(plan);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).anyMatch(error -> error.contains("CLARIFY"));
    }
}
