package org.example.ggbot.agent.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.example.ggbot.agent.AgentChannel;
import org.example.ggbot.agent.AgentRequest;
import org.example.ggbot.agent.AgentState;
import org.example.ggbot.ai.ReliableChatService;
import org.example.ggbot.planner.Plan;
import org.example.ggbot.planner.PlanStep;
import org.example.ggbot.planner.StepStatus;
import org.example.ggbot.tool.SpringAiToolExecutor;
import org.example.ggbot.tool.ToolName;
import org.example.ggbot.tool.impl.GenerateDocTool;
import org.example.ggbot.tool.impl.GeneratePptTool;
import org.example.ggbot.tool.impl.ModifyPptTool;
import org.example.ggbot.tool.impl.SummarizeTool;
import org.junit.jupiter.api.Test;

class DefaultExecutorTest {

    @Test
    void shouldExecutePendingStepThroughSpringAiToolExecutor() {
        ReliableChatService chatService = mock(ReliableChatService.class);
        when(chatService.isAvailable()).thenReturn(false);
        DefaultExecutor executor = new DefaultExecutor(new SpringAiToolExecutor(
                new GenerateDocTool(),
                new GeneratePptTool(),
                new ModifyPptTool(),
                new SummarizeTool(chatService)
        ));

        AgentState state = AgentState.initialize(
                "task-1",
                new AgentRequest(
                        "conversation-1",
                        "user-1",
                        "帮我生成方案文档",
                        AgentChannel.WEB,
                        null,
                        "conversation-1",
                        Map.of()
                ),
                List.of()
        );
        Plan plan = new Plan();
        PlanStep step = new PlanStep("step-1", ToolName.GENERATE_DOC, "生成文档", "帮我生成方案文档");
        plan.addStep(step);
        state.setCurrentPlan(plan);

        ExecutionResult result = executor.execute(state, plan);

        assertThat(result.isSuccess()).isTrue();
        assertThat(step.getStatus()).isEqualTo(StepStatus.SUCCESS);
        assertThat(result.getRecords()).hasSize(1);
    }
}
