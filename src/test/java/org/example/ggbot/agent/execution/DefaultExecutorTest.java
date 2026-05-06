package org.example.ggbot.agent.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.example.ggbot.agent.AgentChannel;
import org.example.ggbot.agent.AgentRequest;
import org.example.ggbot.agent.AgentState;
import org.example.ggbot.ai.ContextAwareChatService;
import org.example.ggbot.ai.MemoryManager;
import org.example.ggbot.ai.ReliableChatService;
import org.example.ggbot.planner.Plan;
import org.example.ggbot.planner.PlanStep;
import org.example.ggbot.planner.StepStatus;
import org.example.ggbot.planner.StepType;
import org.example.ggbot.prompt.ClasspathPromptRepository;
import org.example.ggbot.tool.SpringAiToolExecutor;
import org.example.ggbot.tool.ToolResult;
import org.example.ggbot.tool.ToolName;
import org.example.ggbot.tool.impl.GenerateDocTool;
import org.example.ggbot.tool.impl.GeneratePptTool;
import org.example.ggbot.tool.impl.ModifyPptTool;
import org.example.ggbot.tool.impl.SummarizeTool;
import org.example.ggbot.tool.model.DocumentArtifact;
import org.example.ggbot.tool.ppt.PptSpecParser;
import org.example.ggbot.tool.ppt.PptSpecValidator;
import org.example.ggbot.tool.ppt.PptxRenderer;
import org.example.ggbot.tool.ppt.SemanticPptFallbackGenerator;
import org.example.ggbot.tool.support.ArtifactContentExtractor;
import org.example.ggbot.tool.support.PromptDetailAnalyzer;
import org.example.ggbot.tool.support.ResultQualityEvaluator;
import org.junit.jupiter.api.Test;

class DefaultExecutorTest {

    @Test
    void shouldExecutePendingStepThroughSpringAiToolExecutor() {
        ReliableChatService reliableChatService = mock(ReliableChatService.class);
        ClasspathPromptRepository repository = mock(ClasspathPromptRepository.class);
        ContextAwareChatService chatService = new ContextAwareChatService(
                reliableChatService,
                new MemoryManager(new ClasspathPromptRepository())
        );
        when(reliableChatService.isAvailable()).thenReturn(false);
        DefaultExecutor executor = new DefaultExecutor(new SpringAiToolExecutor(
                new GenerateDocTool(chatService, repository, new PromptDetailAnalyzer()),
                createGeneratePptTool(),
                new ModifyPptTool(new SemanticPptFallbackGenerator(), new PptxRenderer(), repository),
                new SummarizeTool(chatService, repository)
        ), new ArtifactContentExtractor(new ResultQualityEvaluator()));

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

    @Test
    void shouldNotRenderDuplicateObservationWhenArtifactMatchesPlainTextReply() {
        SpringAiToolExecutor toolExecutor = mock(SpringAiToolExecutor.class);
        when(toolExecutor.execute(any(), any(), any(), any())).thenReturn(
                new ToolResult(ToolName.SUMMARIZE, true, "飞书是企业协作平台。", "飞书是企业协作平台。")
        );
        DefaultExecutor executor = new DefaultExecutor(toolExecutor, new ArtifactContentExtractor(new ResultQualityEvaluator()));

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
        plan.addStep(step);
        state.setCurrentPlan(plan);

        ExecutionResult result = executor.execute(state, plan);

        assertThat(result.getRecords()).singleElement()
                .extracting(StepExecutionRecord::getObservation)
                .isEqualTo("飞书是企业协作平台。");
    }

    @Test
    void shouldExecuteStepOnlyWhenDependenciesAreSatisfied() {
        SpringAiToolExecutor toolExecutor = mock(SpringAiToolExecutor.class);
        when(toolExecutor.execute(any(), any(), any(), any())).thenReturn(
                new ToolResult(ToolName.GENERATE_DOC, true, "文档已生成", "文档已生成")
        );
        DefaultExecutor executor = new DefaultExecutor(toolExecutor, new ArtifactContentExtractor(new ResultQualityEvaluator()));

        AgentState state = AgentState.initialize(
                "task-1",
                new AgentRequest(
                        "conversation-1",
                        "user-1",
                        "先总结，再生成文档",
                        AgentChannel.WEB,
                        null,
                        "conversation-1",
                        Map.of()
                ),
                List.of()
        );
        Plan plan = new Plan();
        PlanStep summarize = new PlanStep("step-1", StepType.SUMMARIZE, ToolName.SUMMARIZE, "先总结", "先总结", List.of(), List.of(), "总结结果");
        PlanStep generateDoc = new PlanStep("step-2", StepType.GENERATE_DOC, ToolName.GENERATE_DOC, "再生成文档", "再生成文档", List.of("step-1"), List.of("step-1"), "方案文档");
        summarize.markSuccess(new ToolResult(ToolName.SUMMARIZE, true, "总结完成", "总结完成"));
        plan.addStep(summarize);
        plan.addStep(generateDoc);
        state.setCurrentPlan(plan);
        state.getIntermediateResults().put("step-1", summarize.getResult());

        ExecutionResult result = executor.execute(state, plan);

        assertThat(result.isSuccess()).isTrue();
        assertThat(generateDoc.getStatus()).isEqualTo(StepStatus.SUCCESS);
        verify(toolExecutor).execute(any(), any(), any(), any());
    }

    @Test
    void shouldFailWhenNoPendingStepHasSatisfiedDependencies() {
        SpringAiToolExecutor toolExecutor = mock(SpringAiToolExecutor.class);
        DefaultExecutor executor = new DefaultExecutor(toolExecutor, new ArtifactContentExtractor(new ResultQualityEvaluator()));

        AgentState state = AgentState.initialize(
                "task-1",
                new AgentRequest(
                        "conversation-1",
                        "user-1",
                        "生成文档",
                        AgentChannel.WEB,
                        null,
                        "conversation-1",
                        Map.of()
                ),
                List.of()
        );
        Plan plan = new Plan();
        plan.addStep(new PlanStep("step-2", StepType.GENERATE_DOC, ToolName.GENERATE_DOC, "生成文档", "生成文档", List.of("step-1"), List.of("step-1"), "方案文档"));
        state.setCurrentPlan(plan);

        ExecutionResult result = executor.execute(state, plan);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.hasFailures()).isTrue();
        assertThat(result.getSummary()).contains("依赖");
        verify(toolExecutor, never()).execute(any(), any(), any(), any());
    }

    @Test
    void shouldReturnClarificationWithoutCallingToolExecutor() {
        SpringAiToolExecutor toolExecutor = mock(SpringAiToolExecutor.class);
        DefaultExecutor executor = new DefaultExecutor(toolExecutor, new ArtifactContentExtractor(new ResultQualityEvaluator()));

        AgentState state = AgentState.initialize(
                "task-1",
                new AgentRequest(
                        "conversation-1",
                        "user-1",
                        "帮我做个汇报",
                        AgentChannel.WEB,
                        null,
                        "conversation-1",
                        Map.of()
                ),
                List.of()
        );
        Plan plan = new Plan();
        PlanStep clarify = new PlanStep("step-1", StepType.CLARIFY, null, "请补充汇报主题、对象和页数要求", "请补充汇报主题、对象和页数要求", List.of(), List.of(), "用户补充后的汇报范围");
        plan.addStep(clarify);
        state.setCurrentPlan(plan);

        ExecutionResult result = executor.execute(state, plan);

        assertThat(result.isSuccess()).isTrue();
        assertThat(clarify.getStatus()).isEqualTo(StepStatus.SUCCESS);
        assertThat(result.getSummary()).contains("请补充汇报主题");
        verify(toolExecutor, never()).execute(any(), any(), any(), any());
    }

    @Test
    void shouldMaterializeDependencyInstructionWithReadableArtifactContent() {
        SpringAiToolExecutor toolExecutor = mock(SpringAiToolExecutor.class);
        when(toolExecutor.execute(any(), any(), any(), any())).thenReturn(
                new ToolResult(ToolName.GENERATE_PPT, true, "ppt ok", "ppt ok")
        );
        DefaultExecutor executor = new DefaultExecutor(toolExecutor, new ArtifactContentExtractor(new ResultQualityEvaluator()));

        AgentState state = AgentState.initialize(
                "task-1",
                new AgentRequest(
                        "conversation-1",
                        "user-1",
                        "先生成文档，再生成 PPT",
                        AgentChannel.WEB,
                        null,
                        "conversation-1",
                        Map.of()
                ),
                List.of()
        );
        Plan plan = new Plan();
        PlanStep docStep = new PlanStep(
                "step-1",
                StepType.GENERATE_DOC,
                ToolName.GENERATE_DOC,
                "生成文档",
                "生成文档",
                List.of(),
                List.of(),
                "文档"
        );
        docStep.markSuccess(new ToolResult(
                ToolName.GENERATE_DOC,
                true,
                "文档已生成",
                new DocumentArtifact("红军长征", "# 红军长征\n## 历史背景")
        ));
        PlanStep pptStep = new PlanStep(
                "step-2",
                StepType.GENERATE_PPT,
                ToolName.GENERATE_PPT,
                "生成 PPT",
                "生成 PPT",
                List.of("step-1"),
                List.of("step-1"),
                "PPT"
        );
        plan.addStep(docStep);
        plan.addStep(pptStep);
        state.setCurrentPlan(plan);
        state.getIntermediateResults().put("step-1", docStep.getResult());

        ExecutionResult result = executor.execute(state, plan);

        assertThat(result.isSuccess()).isTrue();
        verify(toolExecutor).execute(eq(ToolName.GENERATE_PPT), contains("# 红军长征"), any(), any());
    }

    private GeneratePptTool createGeneratePptTool() {
        return new GeneratePptTool(
                instruction -> """
                        {
                          "title": "测试主题",
                          "slides": [
                            {"title": "主题概览", "subtitle": "概览", "bullets": ["要点一", "要点二", "要点三", "要点四"], "speakerNotes": "%s"},
                            {"title": "核心内容", "subtitle": "内容", "bullets": ["要点一", "要点二", "要点三", "要点四"], "speakerNotes": "%s"},
                            {"title": "关键要点", "subtitle": "重点", "bullets": ["要点一", "要点二", "要点三", "要点四"], "speakerNotes": "%s"},
                            {"title": "总结建议", "subtitle": "总结", "bullets": ["要点一", "要点二", "要点三", "要点四"], "speakerNotes": "%s"},
                            {"title": "补充页", "subtitle": "补充", "bullets": ["要点一", "要点二", "要点三", "要点四"], "speakerNotes": "%s"},
                            {"title": "结束页", "subtitle": "结束", "bullets": ["要点一", "要点二", "要点三", "要点四"], "speakerNotes": "%s"}
                          ]
                        }
                        """.formatted(notes(), notes(), notes(), notes(), notes(), notes()),
                new PptSpecParser(new ObjectMapper()),
                new PptSpecValidator(new ResultQualityEvaluator()),
                new SemanticPptFallbackGenerator(),
                new PptxRenderer(),
                new ArtifactContentExtractor(new ResultQualityEvaluator()),
                new ClasspathPromptRepository(),
                new PromptDetailAnalyzer()
        );
    }

    private static String notes() {
        return "本页讲稿用于补充背景、逻辑、案例与结论说明，确保演示时不仅列点，还能讲清楚主题之间的关系和现实意义。"
                + "讲述时应当说明这一页为什么重要、数据或事实如何支撑判断，以及下一页如何承接这一页的分析。"
                + "还需要补充关键情境、风险提醒和最终结论，使口头表达具备完整性和说服力。";
    }
}
