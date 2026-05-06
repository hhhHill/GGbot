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
            new DisabledStructuredPlanGenerator(),
            new StructuredPlanParser(),
            new PlanValidator(),
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
        assertThat(plan.getDeliverableType()).isEqualTo(DeliverableType.SUMMARY);
        assertThat(plan.getGoal()).isEqualTo("帮我总结一下这周进展");
        assertThat(plan.isNeedClarification()).isFalse();
        assertThat(plan.isMultiStep()).isFalse();
        assertThat(plan.isNeedDoc()).isFalse();
        assertThat(plan.isNeedPpt()).isFalse();
        assertThat(plan.getSteps()).extracting(PlanStep::getToolName)
                .containsExactly(ToolName.SUMMARIZE);
    }

    @Test
    void shouldCreateDocPlanWhenDocKeywordMatched() {
        Plan plan = plannerService.plan(state("帮我写一个项目方案文档"));

        assertThat(plan.getIntentType()).isEqualTo(IntentType.CREATE_DOC);
        assertThat(plan.getDeliverableType()).isEqualTo(DeliverableType.DOC);
        assertThat(plan.isMultiStep()).isFalse();
        assertThat(plan.isNeedDoc()).isTrue();
        assertThat(plan.isNeedPpt()).isFalse();
        assertThat(plan.getSteps()).extracting(PlanStep::getToolName)
                .containsExactly(ToolName.GENERATE_DOC);
    }

    @Test
    void shouldCreatePptPlanWhenPptKeywordMatched() {
        Plan plan = plannerService.plan(state("帮我做一个汇报PPT"));

        assertThat(plan.getIntentType()).isEqualTo(IntentType.CREATE_DOC_AND_PPT);
        assertThat(plan.getDeliverableType()).isEqualTo(DeliverableType.MIXED);
        assertThat(plan.isNeedClarification()).isFalse();
        assertThat(plan.getSteps()).extracting(PlanStep::getType)
                .containsExactly(StepType.GENERATE_DOC, StepType.GENERATE_PPT);
    }

    @Test
    void shouldCreateCombinedPlanWhenDocAndPptKeywordMatched() {
        Plan plan = plannerService.plan(state("帮我做一个方案文档和汇报PPT"));

        assertThat(plan.getIntentType()).isEqualTo(IntentType.CREATE_DOC_AND_PPT);
        assertThat(plan.getDeliverableType()).isEqualTo(DeliverableType.MIXED);
        assertThat(plan.isMultiStep()).isTrue();
        assertThat(plan.isNeedDoc()).isTrue();
        assertThat(plan.isNeedPpt()).isTrue();
        assertThat(plan.getSteps()).extracting(PlanStep::getToolName)
                .containsExactly(ToolName.GENERATE_DOC, ToolName.GENERATE_PPT);
        assertThat(plan.getSteps().get(1).getDependsOn()).containsExactly(plan.getSteps().get(0).getStepId());
        assertThat(plan.getSteps().get(1).getInputRefs()).containsExactly(plan.getSteps().get(0).getStepId());
    }

    @Test
    void shouldUseStructuredPlanWhenGeneratedPlanIsValid() {
        PlannerService service = new PlannerService(
                new StubStructuredPlanGenerator("""
                        {
                          "goal": "生成 AI 趋势汇报材料",
                          "deliverableType": "PPT",
                          "needClarification": false,
                          "multiStep": true,
                          "steps": [
                            {
                              "id": "1",
                              "type": "GENERATE_DOC",
                              "desc": "先整理 AI 趋势要点",
                              "dependsOn": [],
                              "inputRefs": [],
                              "expectedOutput": "一份 AI 趋势内容文档"
                            },
                            {
                              "id": "2",
                              "type": "GENERATE_PPT",
                              "desc": "根据文档生成汇报 PPT",
                              "dependsOn": ["1"],
                              "inputRefs": ["1"],
                              "expectedOutput": "AI 趋势汇报 PPT 大纲"
                            }
                          ]
                        }
                        """),
                new StructuredPlanParser(),
                new PlanValidator(),
                List.of(new DocPlanningRule(), new PptPlanningRule()),
                new PlanStepFactory()
        );

        Plan plan = service.plan(state("帮我做一个 AI 趋势汇报 PPT"));

        assertThat(plan.getDeliverableType()).isEqualTo(DeliverableType.PPT);
        assertThat(plan.getGoal()).isEqualTo("生成 AI 趋势汇报材料");
        assertThat(plan.isNeedClarification()).isFalse();
        assertThat(plan.isMultiStep()).isTrue();
        assertThat(plan.getSteps()).extracting(PlanStep::getType)
                .containsExactly(StepType.GENERATE_DOC, StepType.GENERATE_PPT);
        assertThat(plan.getSteps().get(1).getDependsOn()).containsExactly("1");
        assertThat(plan.getSteps().get(1).getInputRefs()).containsExactly("1");
    }

    @Test
    void shouldCreateSummarizeThenGenerateDocPlanFromStructuredOutput() {
        PlannerService service = new PlannerService(
                new StubStructuredPlanGenerator("""
                        {
                          "goal": "先总结内容再形成方案文档",
                          "deliverableType": "DOC",
                          "needClarification": false,
                          "multiStep": true,
                          "steps": [
                            {
                              "id": "1",
                              "type": "SUMMARIZE",
                              "desc": "提炼原始内容重点",
                              "dependsOn": [],
                              "inputRefs": [],
                              "expectedOutput": "一段重点总结"
                            },
                            {
                              "id": "2",
                              "type": "GENERATE_DOC",
                              "desc": "把总结整理成方案文档",
                              "dependsOn": ["1"],
                              "inputRefs": ["1"],
                              "expectedOutput": "结构化方案文档"
                            }
                          ]
                        }
                        """),
                new StructuredPlanParser(),
                new PlanValidator(),
                List.of(new DocPlanningRule(), new PptPlanningRule()),
                new PlanStepFactory()
        );

        Plan plan = service.plan(state("先总结下面这段内容，再整理成一份方案文档"));

        assertThat(plan.getDeliverableType()).isEqualTo(DeliverableType.DOC);
        assertThat(plan.getSteps()).extracting(PlanStep::getType)
                .containsExactly(StepType.SUMMARIZE, StepType.GENERATE_DOC);
        assertThat(plan.getSteps().get(1).getDependsOn()).containsExactly("1");
    }

    @Test
    void shouldCreateClarifyPlanWhenInformationIsInsufficient() {
        PlannerService service = new PlannerService(
                new StubStructuredPlanGenerator("""
                        {
                          "goal": "补充汇报任务的关键信息",
                          "deliverableType": "UNKNOWN",
                          "needClarification": true,
                          "multiStep": false,
                          "steps": [
                            {
                              "id": "1",
                              "type": "CLARIFY",
                              "desc": "你希望做什么主题、给谁汇报、预计几页？",
                              "dependsOn": [],
                              "inputRefs": [],
                              "expectedOutput": "用户补充的汇报范围与目标"
                            }
                          ]
                        }
                        """),
                new StructuredPlanParser(),
                new PlanValidator(),
                List.of(new DocPlanningRule(), new PptPlanningRule()),
                new PlanStepFactory()
        );

        Plan plan = service.plan(state("帮我做个汇报"));

        assertThat(plan.isNeedClarification()).isTrue();
        assertThat(plan.getSteps()).singleElement()
                .extracting(PlanStep::getType)
                .isEqualTo(StepType.CLARIFY);
    }

    @Test
    void shouldFallbackWhenStructuredPlanIsInvalid() {
        PlannerService service = new PlannerService(
                new StubStructuredPlanGenerator("""
                        {
                          "goal": "生成 PPT",
                          "deliverableType": "PPT",
                          "needClarification": false,
                          "multiStep": true,
                          "steps": [
                            {
                              "id": "2",
                              "type": "GENERATE_PPT",
                              "desc": "直接生成 PPT",
                              "dependsOn": ["9"],
                              "inputRefs": ["9"],
                              "expectedOutput": "PPT"
                            }
                          ]
                        }
                        """),
                new StructuredPlanParser(),
                new PlanValidator(),
                List.of(new DocPlanningRule(), new PptPlanningRule()),
                new PlanStepFactory()
        );

        Plan plan = service.plan(state("帮我做一个 AI 趋势汇报 PPT"));

        assertThat(plan.getDeliverableType()).isEqualTo(DeliverableType.MIXED);
        assertThat(plan.isNeedClarification()).isFalse();
        assertThat(plan.getSteps()).extracting(PlanStep::getType)
                .containsExactly(StepType.GENERATE_DOC, StepType.GENERATE_PPT);
    }

    @Test
    void shouldFallbackWhenStructuredPlanIsNotJson() {
        PlannerService service = new PlannerService(
                new StubStructuredPlanGenerator("not-json"),
                new StructuredPlanParser(),
                new PlanValidator(),
                List.of(new DocPlanningRule(), new PptPlanningRule()),
                new PlanStepFactory()
        );

        Plan plan = service.plan(state("总结一下这段内容"));

        assertThat(plan.getDeliverableType()).isEqualTo(DeliverableType.SUMMARY);
        assertThat(plan.getSteps()).extracting(PlanStep::getType)
                .containsExactly(StepType.SUMMARIZE);
    }

    @Test
    void shouldFallbackWhenStructuredPlanContainsInvalidStepType() {
        PlannerService service = new PlannerService(
                new StubStructuredPlanGenerator("""
                        {
                          "goal": "生成汇报",
                          "deliverableType": "PPT",
                          "needClarification": false,
                          "multiStep": false,
                          "steps": [
                            {
                              "id": "1",
                              "type": "UNKNOWN_STEP",
                              "desc": "无效步骤",
                              "dependsOn": [],
                              "inputRefs": [],
                              "expectedOutput": "无"
                            }
                          ]
                        }
                        """),
                new StructuredPlanParser(),
                new PlanValidator(),
                List.of(new DocPlanningRule(), new PptPlanningRule()),
                new PlanStepFactory()
        );

        Plan plan = service.plan(state("帮我做个汇报"));

        assertThat(plan.isNeedClarification()).isTrue();
        assertThat(plan.getSteps()).extracting(PlanStep::getType)
                .containsExactly(StepType.CLARIFY);
    }

    @Test
    void shouldFallbackWhenStructuredPlanContainsCycle() {
        PlannerService service = new PlannerService(
                new StubStructuredPlanGenerator("""
                        {
                          "goal": "先总结再生成文档",
                          "deliverableType": "DOC",
                          "needClarification": false,
                          "multiStep": true,
                          "steps": [
                            {
                              "id": "1",
                              "type": "SUMMARIZE",
                              "desc": "先总结",
                              "dependsOn": ["2"],
                              "inputRefs": ["2"],
                              "expectedOutput": "总结"
                            },
                            {
                              "id": "2",
                              "type": "GENERATE_DOC",
                              "desc": "再生成文档",
                              "dependsOn": ["1"],
                              "inputRefs": ["1"],
                              "expectedOutput": "文档"
                            }
                          ]
                        }
                        """),
                new StructuredPlanParser(),
                new PlanValidator(),
                List.of(new DocPlanningRule(), new PptPlanningRule()),
                new PlanStepFactory()
        );

        Plan plan = service.plan(state("先总结下面这段内容，再整理成一份方案文档"));

        assertThat(plan.getDeliverableType()).isEqualTo(DeliverableType.DOC);
        assertThat(plan.getSteps()).extracting(PlanStep::getType)
                .containsExactly(StepType.GENERATE_DOC);
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

    private static final class StubStructuredPlanGenerator implements StructuredPlanGenerator {
        private final String planJson;

        private StubStructuredPlanGenerator(String planJson) {
            this.planJson = planJson;
        }

        @Override
        public java.util.Optional<String> generate(PlannerContext context) {
            return java.util.Optional.ofNullable(planJson);
        }
    }
}
