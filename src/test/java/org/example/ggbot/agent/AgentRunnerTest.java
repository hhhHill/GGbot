package org.example.ggbot.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.example.ggbot.agent.execution.ExecutionResult;
import org.example.ggbot.agent.execution.Executor;
import org.example.ggbot.agent.execution.StepExecutionRecord;
import org.example.ggbot.agent.reflection.ReflectionAnalysis;
import org.example.ggbot.agent.reflection.Reflector;
import org.example.ggbot.agent.reflection.ReflectionType;
import org.example.ggbot.agent.replan.RePlanner;
import org.example.ggbot.planner.Plan;
import org.example.ggbot.planner.PlanStep;
import org.example.ggbot.planner.Planner;
import org.example.ggbot.planner.StepStatus;
import org.example.ggbot.tool.ToolName;
import org.junit.jupiter.api.Test;

class AgentRunnerTest {

    @Test
    void shouldFinishWhenExecutionSucceedsWithoutReplan() {
        Planner planner = state -> {
            Plan plan = new Plan();
            plan.addStep(new PlanStep("step-1", ToolName.SUMMARIZE, "直接总结", state.getUserInput()));
            return plan;
        };

        Executor executor = (state, plan) -> {
            PlanStep step = plan.getPendingSteps().get(0);
            step.markSuccess("总结完成");
            StepExecutionRecord record = new StepExecutionRecord(
                    state.getIteration(),
                    step.getStepId(),
                    step.getToolName(),
                    step.getInstruction(),
                    StepStatus.SUCCESS,
                    "执行总结",
                    "得到总结结果",
                    "总结完成",
                    null
            );
            return new ExecutionResult(List.of(step), List.of(record), true, false, "本轮执行成功");
        };

        Reflector reflector = (state, result) -> new ReflectionAnalysis(
                true,
                false,
                false,
                true,
                ReflectionType.SUCCESS,
                "步骤已完成",
                "无需重规划",
                "finish"
        );

        RePlanner rePlanner = (state, reflection) -> state.getCurrentPlan();

        AgentRunner runner = new AgentRunner(planner, executor, reflector, rePlanner);
        AgentState finalState = runner.run(initialState("帮我总结这周工作"));

        assertThat(finalState.isDone()).isTrue();
        assertThat(finalState.getExecutionHistory()).hasSize(1);
        assertThat(finalState.getCurrentPlan().getSteps().get(0).getStatus()).isEqualTo(StepStatus.SUCCESS);
    }

    @Test
    void shouldReplanWhenFirstExecutionFails() {
        Planner planner = state -> {
            if (state.getCurrentPlan() != null) {
                return state.getCurrentPlan();
            }
            Plan plan = new Plan();
            plan.addStep(new PlanStep("step-1", ToolName.GENERATE_DOC, "生成文档", state.getUserInput()));
            return plan;
        };

        Executor executor = (state, plan) -> {
            PlanStep step = plan.getPendingSteps().get(0);
            if (state.getIteration() == 1) {
                step.markFailed("文档生成失败");
                StepExecutionRecord failedRecord = new StepExecutionRecord(
                        state.getIteration(),
                        step.getStepId(),
                        step.getToolName(),
                        step.getInstruction(),
                        StepStatus.FAILED,
                        "先尝试生成文档",
                        "工具执行失败",
                        null,
                        "文档生成失败"
                );
                return new ExecutionResult(List.of(step), List.of(failedRecord), false, true, "执行失败");
            }

            PlanStep replannedStep = plan.getPendingSteps().get(0);
            replannedStep.markSuccess("兜底总结完成");
            StepExecutionRecord successRecord = new StepExecutionRecord(
                    state.getIteration(),
                    replannedStep.getStepId(),
                    replannedStep.getToolName(),
                    replannedStep.getInstruction(),
                    StepStatus.SUCCESS,
                    "降级执行总结",
                    "总结成功",
                    "兜底总结完成",
                    null
            );
            return new ExecutionResult(List.of(replannedStep), List.of(successRecord), true, false, "重规划后成功");
        };

        Reflector reflector = (state, result) -> {
            if (result.hasFailures()) {
                return new ReflectionAnalysis(
                        false,
                        false,
                        true,
                        false,
                        ReflectionType.TOOL_EXECUTION_FAILURE,
                        "执行失败",
                        "需要调整计划",
                        "replan"
                );
            }
            return new ReflectionAnalysis(
                    true,
                    false,
                    false,
                    true,
                    ReflectionType.SUCCESS,
                    "重规划后成功",
                    "可以结束",
                    "finish"
            );
        };

        RePlanner rePlanner = (state, reflection) -> {
            Plan replanned = state.getCurrentPlan();
            replanned.addStep(new PlanStep("step-2", ToolName.SUMMARIZE, "兜底总结", "请总结失败原因并输出保底答复"));
            return replanned;
        };

        AgentRunner runner = new AgentRunner(planner, executor, reflector, rePlanner);
        AgentState finalState = runner.run(initialState("帮我生成一个方案文档"));

        assertThat(finalState.isDone()).isTrue();
        assertThat(finalState.getExecutionHistory()).hasSize(2);
        assertThat(finalState.getCurrentPlan().getSteps()).hasSize(2);
        assertThat(finalState.getCurrentPlan().getSteps().get(0).getStatus()).isEqualTo(StepStatus.FAILED);
        assertThat(finalState.getCurrentPlan().getSteps().get(1).getStatus()).isEqualTo(StepStatus.SUCCESS);
    }

    private AgentState initialState(String userInput) {
        return AgentState.initialize(
                "task-1",
                new AgentRequest(
                        "conversation-1",
                        "user-1",
                        userInput,
                        AgentChannel.WEB,
                        null,
                        "conversation-1",
                        Map.of()
                ),
                List.of()
        );
    }
}
