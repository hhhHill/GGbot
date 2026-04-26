package org.example.ggbot.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.example.ggbot.agent.execution.ExecutionResult;
import org.example.ggbot.agent.execution.StepExecutionRecord;
import org.example.ggbot.agent.reflection.ReflectionAnalysis;
import org.example.ggbot.agent.reflection.ReflectionType;
import org.example.ggbot.agent.reflection.SimpleReflector;
import org.example.ggbot.agent.replan.DefaultRePlanner;
import org.example.ggbot.agent.replan.PlanExhaustedReplanStrategy;
import org.example.ggbot.agent.replan.ToolFailureReplanStrategy;
import org.example.ggbot.agent.replan.WeakResultReplanStrategy;
import org.example.ggbot.planner.Plan;
import org.example.ggbot.planner.PlanStep;
import org.example.ggbot.planner.StepStatus;
import org.example.ggbot.tool.ToolName;
import org.example.ggbot.tool.ToolResult;
import org.junit.jupiter.api.Test;

class ReflectionAndReplanTest {

    private final SimpleReflector reflector = new SimpleReflector();
    private final DefaultRePlanner rePlanner = new DefaultRePlanner(List.of(
            new ToolFailureReplanStrategy(),
            new WeakResultReplanStrategy(),
            new PlanExhaustedReplanStrategy()
    ));

    @Test
    void shouldClassifyToolExecutionFailureAndAppendRetryOrFallbackStep() {
        AgentState state = stateWithSinglePendingStep("step-1", ToolName.GENERATE_DOC, "生成文档");
        StepExecutionRecord failedRecord = new StepExecutionRecord(
                1,
                "step-1",
                ToolName.GENERATE_DOC,
                "生成文档",
                StepStatus.FAILED,
                "尝试执行文档工具",
                "工具执行失败",
                null,
                "timeout"
        );
        ExecutionResult result = new ExecutionResult(List.of(state.getCurrentPlan().getSteps().get(0)), List.of(failedRecord), false, true, "执行失败");

        ReflectionAnalysis analysis = reflector.analyze(state, result);
        Plan replanned = rePlanner.replan(state, analysis);

        assertThat(analysis.getReflectionType()).isEqualTo(ReflectionType.TOOL_EXECUTION_FAILURE);
        assertThat(replanned.getPendingSteps()).extracting(PlanStep::getToolName)
                .contains(ToolName.GENERATE_DOC);
    }

    @Test
    void shouldClassifyWeakResultAndAppendModifyPptStep() {
        AgentState state = stateWithSinglePendingStep("step-1", ToolName.GENERATE_PPT, "生成 PPT");
        PlanStep step = state.getCurrentPlan().getSteps().get(0);
        step.markSuccess(new ToolResult(ToolName.GENERATE_PPT, true, "", null));
        StepExecutionRecord weakRecord = new StepExecutionRecord(
                1,
                "step-1",
                ToolName.GENERATE_PPT,
                "生成 PPT",
                StepStatus.SUCCESS,
                "执行 PPT 生成",
                "",
                new ToolResult(ToolName.GENERATE_PPT, true, "", null),
                null
        );
        ExecutionResult result = new ExecutionResult(List.of(step), List.of(weakRecord), true, false, "");

        ReflectionAnalysis analysis = reflector.analyze(state, result);
        Plan replanned = rePlanner.replan(state, analysis);

        assertThat(analysis.getReflectionType()).isEqualTo(ReflectionType.EMPTY_OR_WEAK_RESULT);
        assertThat(replanned.getPendingSteps()).extracting(PlanStep::getToolName)
                .contains(ToolName.MODIFY_PPT);
    }

    @Test
    void shouldClassifyPlanExhaustedAndAppendSummarizeStep() {
        AgentState state = AgentState.initialize(
                "task-1",
                request("继续完成这个任务"),
                List.of()
        );
        Plan plan = new Plan();
        plan.setIntentType(org.example.ggbot.planner.IntentType.CHAT);
        state.setCurrentPlan(plan);
        state.setDone(false);

        ExecutionResult result = new ExecutionResult(List.of(), List.of(), true, false, "没有待执行步骤");
        ReflectionAnalysis analysis = reflector.analyze(state, result);
        Plan replanned = rePlanner.replan(state, analysis);

        assertThat(analysis.getReflectionType()).isEqualTo(ReflectionType.PLAN_EXHAUSTED_BUT_NOT_DONE);
        assertThat(replanned.getPendingSteps()).extracting(PlanStep::getToolName)
                .contains(ToolName.SUMMARIZE);
    }

    private AgentState stateWithSinglePendingStep(String stepId, ToolName toolName, String instruction) {
        AgentState state = AgentState.initialize("task-1", request(instruction), List.of());
        Plan plan = new Plan();
        plan.addStep(new PlanStep(stepId, toolName, instruction, instruction));
        state.setCurrentPlan(plan);
        return state;
    }

    private AgentRequest request(String userInput) {
        return new AgentRequest(
                "conversation-1",
                "user-1",
                userInput,
                AgentChannel.WEB,
                null,
                "conversation-1",
                Map.of()
        );
    }
}
