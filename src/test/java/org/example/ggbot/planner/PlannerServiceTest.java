package org.example.ggbot.planner;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.example.ggbot.agent.AgentChannel;
import org.example.ggbot.agent.AgentRequest;
import org.example.ggbot.agent.AgentState;
import org.example.ggbot.tool.ToolName;
import org.junit.jupiter.api.Test;

class PlannerServiceTest {

    private final PlannerService plannerService = new PlannerService(
            List.of(
                    new DocPlanningRule(),
                    new PptPlanningRule()
            ),
            new PlanStepFactory()
    );

    @Test
    void shouldCreateChatPlanWhenNoDocOrPptKeywordMatched() {
        Plan plan = plannerService.plan(state("帮我总结一下这周进展"));

        assertThat(plan.getIntentType()).isEqualTo(IntentType.CHAT);
        assertThat(plan.isNeedDoc()).isFalse();
        assertThat(plan.isNeedPpt()).isFalse();
        assertThat(plan.getSteps()).extracting(PlanStep::getToolName)
                .containsExactly(ToolName.SUMMARIZE);
    }

    @Test
    void shouldCreateDocPlanWhenDocKeywordMatched() {
        Plan plan = plannerService.plan(state("帮我写一个项目方案文档"));

        assertThat(plan.getIntentType()).isEqualTo(IntentType.CREATE_DOC);
        assertThat(plan.isNeedDoc()).isTrue();
        assertThat(plan.isNeedPpt()).isFalse();
        assertThat(plan.getSteps()).extracting(PlanStep::getToolName)
                .containsExactly(ToolName.GENERATE_DOC);
    }

    @Test
    void shouldCreatePptPlanWhenPptKeywordMatched() {
        Plan plan = plannerService.plan(state("帮我做一个汇报PPT"));

        assertThat(plan.getIntentType()).isEqualTo(IntentType.CREATE_PPT);
        assertThat(plan.isNeedDoc()).isFalse();
        assertThat(plan.isNeedPpt()).isTrue();
        assertThat(plan.getSteps()).extracting(PlanStep::getToolName)
                .containsExactly(ToolName.GENERATE_PPT);
    }

    @Test
    void shouldCreateCombinedPlanWhenDocAndPptKeywordMatched() {
        Plan plan = plannerService.plan(state("帮我做一个方案文档和汇报PPT"));

        assertThat(plan.getIntentType()).isEqualTo(IntentType.CREATE_DOC_AND_PPT);
        assertThat(plan.isNeedDoc()).isTrue();
        assertThat(plan.isNeedPpt()).isTrue();
        assertThat(plan.getSteps()).extracting(PlanStep::getToolName)
                .containsExactly(ToolName.GENERATE_DOC, ToolName.GENERATE_PPT);
    }

    private AgentState state(String userInput) {
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
