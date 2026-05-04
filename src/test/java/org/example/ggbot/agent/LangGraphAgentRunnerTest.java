package org.example.ggbot.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.bsc.langgraph4j.NodeOutput;
import org.example.ggbot.agent.execution.ExecutionResult;
import org.example.ggbot.agent.execution.StepExecutionRecord;
import org.example.ggbot.agent.graph.AgentGraphFactory;
import org.example.ggbot.agent.graph.AgentGraphRouter;
import org.example.ggbot.agent.graph.AgentNodes;
import org.example.ggbot.agent.runner.LangGraphAgentRunner;
import org.example.ggbot.agent.reflection.ReflectionAnalysis;
import org.example.ggbot.agent.reflection.ReflectionType;
import org.example.ggbot.planner.Plan;
import org.example.ggbot.planner.PlanStep;
import org.example.ggbot.planner.StepStatus;
import org.example.ggbot.tool.ToolName;
import org.junit.jupiter.api.Test;

class LangGraphAgentRunnerTest {

    @Test
    void shouldCompleteGraphExecutionAcrossMultiplePlanCycles() {
        AgentNodes nodes = new AgentNodes(
                state -> {
                    if (state.getCurrentPlan() != null && state.getCurrentPlan().hasPendingSteps()) {
                        return state.getCurrentPlan();
                    }
                    Plan plan = new Plan();
                    plan.addStep(new PlanStep("step-1", ToolName.GENERATE_DOC, "先生成文档", state.getUserInput()));
                    plan.addStep(new PlanStep("step-2", ToolName.SUMMARIZE, "再总结结果", "总结文档结果"));
                    return plan;
                },
                (state, plan) -> {
                    PlanStep step = plan.getPendingSteps().get(0);
                    String observation = "完成 " + step.getStepId();
                    step.markSuccess(observation);
                    StepExecutionRecord record = new StepExecutionRecord(
                            state.getIteration(),
                            step.getStepId(),
                            step.getToolName(),
                            step.getInstruction(),
                            StepStatus.SUCCESS,
                            "执行步骤",
                            observation,
                            observation,
                            null
                    );
                    return new ExecutionResult(List.of(step), List.of(record), true, false, observation);
                },
                (state, result) -> {
                    boolean done = state.getCurrentPlan() != null && state.getCurrentPlan().allStepsCompleted();
                    return new ReflectionAnalysis(
                            done,
                            false,
                            false,
                            done,
                            ReflectionType.SUCCESS,
                            done ? "全部完成" : "继续执行下一步",
                            result.getSummary(),
                            done ? "finish" : "continue"
                    );
                },
                (state, reflection) -> state.getCurrentPlan()
        );
        LangGraphAgentRunner runner = new LangGraphAgentRunner(
                new AgentGraphFactory(nodes, new AgentGraphRouter())
        );

        AgentState result = runner.run(initialState("帮我先生成文档再总结"), 10);

        assertThat(result.isDone()).isTrue();
        assertThat(result.getIteration()).isEqualTo(2);
        assertThat(result.getExecutionHistory()).hasSize(2);
        assertThat(result.getCurrentPlan().getSteps()).extracting(PlanStep::getStatus)
                .containsExactly(StepStatus.SUCCESS, StepStatus.SUCCESS);
        assertThat(result.getFinalReply()).isEqualTo("完成 step-1\n\n完成 step-2");
    }

    @Test
    void shouldStreamNodeOutputsAndReleaseResources() {
        AgentNodes nodes = new AgentNodes(
                state -> {
                    if (state.getCurrentPlan() != null && state.getCurrentPlan().hasPendingSteps()) {
                        return state.getCurrentPlan();
                    }
                    Plan plan = new Plan();
                    plan.addStep(new PlanStep("step-1", ToolName.GENERATE_DOC, "先生成文档", state.getUserInput()));
                    return plan;
                },
                (state, plan) -> {
                    PlanStep step = plan.getPendingSteps().get(0);
                    String observation = "完成 " + step.getStepId();
                    step.markSuccess(observation);
                    StepExecutionRecord record = new StepExecutionRecord(
                            state.getIteration(),
                            step.getStepId(),
                            step.getToolName(),
                            step.getInstruction(),
                            StepStatus.SUCCESS,
                            "执行步骤",
                            observation,
                            observation,
                            null
                    );
                    return new ExecutionResult(List.of(step), List.of(record), true, false, observation);
                },
                (state, result) -> new ReflectionAnalysis(
                        true,
                        false,
                        false,
                        true,
                        ReflectionType.SUCCESS,
                        "全部完成",
                        result.getSummary(),
                        "finish"
                ),
                (state, reflection) -> state.getCurrentPlan()
        );
        LangGraphAgentRunner runner = new LangGraphAgentRunner(
                new AgentGraphFactory(nodes, new AgentGraphRouter())
        );

        List<NodeOutput<org.example.ggbot.agent.graph.GGBotAgentGraphState>> outputs =
                runner.stream(initialState("帮我先生成文档"), 10).stream().toList();

        assertThat(outputs).isNotEmpty();
        assertThat(outputs.get(outputs.size() - 1).node()).isEqualTo("__END__");
        assertThat(runner.run(initialState("帮我先生成文档"), 10).getFinalReply()).isEqualTo("完成 step-1");
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
