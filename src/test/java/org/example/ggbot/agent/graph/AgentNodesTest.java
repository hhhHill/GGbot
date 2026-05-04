package org.example.ggbot.agent.graph;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.example.ggbot.agent.AgentChannel;
import org.example.ggbot.agent.AgentRequest;
import org.example.ggbot.agent.AgentState;
import org.example.ggbot.agent.execution.ExecutionResult;
import org.example.ggbot.agent.execution.StepExecutionRecord;
import org.example.ggbot.agent.reflection.ReflectionAnalysis;
import org.example.ggbot.agent.reflection.ReflectionType;
import org.example.ggbot.planner.Plan;
import org.example.ggbot.planner.PlanStep;
import org.example.ggbot.planner.Planner;
import org.example.ggbot.planner.StepStatus;
import org.example.ggbot.tool.ToolName;
import org.junit.jupiter.api.Test;

class AgentNodesTest {

    @Test
    void shouldWriteBackPlanExecutionAndReflectionIntoDelegateState() throws Exception {
        Planner planner = state -> {
            Plan plan = new Plan();
            plan.addStep(new PlanStep("step-1", ToolName.SUMMARIZE, "总结", state.getUserInput()));
            return plan;
        };
        AgentNodes nodes = new AgentNodes(
                planner,
                (state, plan) -> {
                    PlanStep step = plan.getPendingSteps().get(0);
                    step.markSuccess("完成总结");
                    StepExecutionRecord record = new StepExecutionRecord(
                            state.getIteration(),
                            step.getStepId(),
                            step.getToolName(),
                            step.getInstruction(),
                            StepStatus.SUCCESS,
                            "执行总结",
                            "总结完成",
                            "完成总结",
                            null
                    );
                    return new ExecutionResult(List.of(step), List.of(record), true, false, "执行成功");
                },
                (state, result) -> new ReflectionAnalysis(
                        true,
                        false,
                        false,
                        true,
                        ReflectionType.SUCCESS,
                        "任务已完成",
                        result.getSummary(),
                        "finish"
                ),
                (state, reflection) -> state.getCurrentPlan()
        );

        AgentState delegate = initialState("帮我总结今天的会议记录");
        Map<String, Object> input = GGBotAgentGraphState.inputOf(delegate);
        try {
            GGBotAgentGraphState plannedState = apply(delegateState(input), nodes.plan(delegateState(input)));
            assertThat(plannedState.delegate().getIteration()).isEqualTo(1);
            assertThat(plannedState.delegate().getCurrentPlan()).isNotNull();

            GGBotAgentGraphState executedState = apply(plannedState, nodes.execute(plannedState));
            assertThat(executedState.lastExecutionResult()).isNotNull();
            assertThat(executedState.delegate().getExecutionHistory()).isEmpty();
            assertThat(executedState.delegate().getCurrentPlan().getSteps()).singleElement()
                    .extracting(PlanStep::getStatus)
                    .isEqualTo(StepStatus.SUCCESS);

            GGBotAgentGraphState reflectedState = apply(executedState, nodes.reflect(executedState));
            assertThat(reflectedState.lastReflection()).isNotNull();
            assertThat(reflectedState.delegate().isDone()).isTrue();
            assertThat(reflectedState.delegate().getExecutionHistory()).hasSize(1);
            assertThat(reflectedState.delegate().getFinalReply()).isEqualTo("总结完成");
        } finally {
            GGBotAgentGraphState.release(input);
        }
    }

    private GGBotAgentGraphState delegateState(Map<String, Object> data) {
        return new GGBotAgentGraphState(data);
    }

    private GGBotAgentGraphState apply(GGBotAgentGraphState state, Map<String, Object> updates) {
        return new GGBotAgentGraphState(
                org.bsc.langgraph4j.state.AgentState.updateState(state, updates, GGBotAgentGraphState.channels())
        );
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
