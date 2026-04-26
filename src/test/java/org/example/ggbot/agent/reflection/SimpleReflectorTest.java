package org.example.ggbot.agent.reflection;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.example.ggbot.agent.AgentChannel;
import org.example.ggbot.agent.AgentRequest;
import org.example.ggbot.agent.AgentState;
import org.example.ggbot.agent.execution.ExecutionResult;
import org.example.ggbot.agent.execution.StepExecutionRecord;
import org.example.ggbot.planner.Plan;
import org.example.ggbot.planner.PlanStep;
import org.example.ggbot.planner.StepStatus;
import org.example.ggbot.tool.ToolName;
import org.junit.jupiter.api.Test;

class SimpleReflectorTest {

    @Test
    void shouldFinishWhenCurrentPlanHasNoPendingStepsAfterSuccessfulExecution() {
        SimpleReflector reflector = new SimpleReflector();
        AgentState state = AgentState.initialize(
                "task-1",
                new AgentRequest(
                        "conversation-1",
                        "user-1",
                        "飞书是什么",
                        AgentChannel.WEB,
                        null,
                        "conversation-1",
                        Map.of()
                ),
                List.of()
        );
        Plan plan = new Plan();
        PlanStep step = new PlanStep("step-1", ToolName.SUMMARIZE, "总结", "飞书是什么");
        step.markSuccess("飞书是企业协作平台。");
        plan.addStep(step);
        state.setCurrentPlan(plan);

        ExecutionResult result = new ExecutionResult(
                List.of(step),
                List.of(new StepExecutionRecord(
                        1,
                        "step-1",
                        ToolName.SUMMARIZE,
                        "飞书是什么",
                        StepStatus.SUCCESS,
                        "执行总结",
                        "飞书是企业协作平台。",
                        "飞书是企业协作平台。",
                        null
                )),
                true,
                false,
                "执行成功"
        );

        ReflectionAnalysis analysis = reflector.analyze(state, result);

        assertThat(analysis.isDone()).isTrue();
        assertThat(analysis.isNeedReplan()).isFalse();
        assertThat(analysis.getRecommendedAction()).isEqualTo("finish");
    }
}
