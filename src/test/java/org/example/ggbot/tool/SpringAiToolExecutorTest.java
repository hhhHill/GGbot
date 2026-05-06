package org.example.ggbot.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.example.ggbot.agent.AgentChannel;
import org.example.ggbot.agent.AgentContext;
import org.example.ggbot.ai.ContextAwareChatService;
import org.example.ggbot.ai.MemoryManager;
import org.example.ggbot.ai.ReliableChatService;
import org.example.ggbot.prompt.ClasspathPromptRepository;
import org.example.ggbot.tool.impl.GenerateDocTool;
import org.example.ggbot.tool.impl.GeneratePptTool;
import org.example.ggbot.tool.impl.ModifyPptTool;
import org.example.ggbot.tool.impl.SummarizeTool;
import org.example.ggbot.tool.model.DocumentArtifact;
import org.example.ggbot.tool.model.PptArtifact;
import org.example.ggbot.tool.ppt.PptSpecParser;
import org.example.ggbot.tool.ppt.PptSpecValidator;
import org.example.ggbot.tool.ppt.PptxRenderer;
import org.example.ggbot.tool.ppt.SemanticPptFallbackGenerator;
import org.example.ggbot.tool.support.ArtifactContentExtractor;
import org.example.ggbot.tool.support.PromptDetailAnalyzer;
import org.example.ggbot.tool.support.ResultQualityEvaluator;
import org.junit.jupiter.api.Test;

class SpringAiToolExecutorTest {

    @Test
    void shouldExecuteGenerateDocToolWithoutCustomRegistry() {
        ReliableChatService reliableChatService = mock(ReliableChatService.class);
        ClasspathPromptRepository repository = mock(ClasspathPromptRepository.class);
        ContextAwareChatService chatService = new ContextAwareChatService(
                reliableChatService,
                new MemoryManager(new ClasspathPromptRepository())
        );
        when(reliableChatService.isAvailable()).thenReturn(false);
        SpringAiToolExecutor executor = new SpringAiToolExecutor(
                new GenerateDocTool(chatService, repository, new PromptDetailAnalyzer()),
                createGeneratePptTool(reliableChatService),
                new ModifyPptTool(new SemanticPptFallbackGenerator(), new PptxRenderer(), repository),
                new SummarizeTool(chatService, repository)
        );

        ToolResult result = executor.execute(
                ToolName.GENERATE_DOC,
                "帮我整理一个项目方案",
                context(),
                Map.of("iteration", 1)
        );

        assertThat(result.getToolName()).isEqualTo(ToolName.GENERATE_DOC);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getArtifact()).isInstanceOf(DocumentArtifact.class);
    }

    @Test
    void shouldGenerateTopicBasedDocumentInsteadOfProjectTemplate() {
        ContextAwareChatService chatService = mock(ContextAwareChatService.class);
        when(chatService.isAvailable()).thenReturn(false);
        GenerateDocTool tool = new GenerateDocTool(chatService, new ClasspathPromptRepository(), new PromptDetailAnalyzer());

        ToolResult result = tool.execute("请生成一份关于红军长征的文档", context(), Map.of());

        DocumentArtifact artifact = (DocumentArtifact) result.getArtifact();
        assertThat(artifact.getTitle()).contains("红军长征");
        assertThat(artifact.getMarkdown()).doesNotContain("MVP", "接口", "用户价值");
    }

    @Test
    void shouldModifyPptWithoutReturningDemoTemplate() {
        ModifyPptTool tool = new ModifyPptTool(
                new SemanticPptFallbackGenerator(),
                new PptxRenderer(),
                new ClasspathPromptRepository()
        );

        ToolResult result = tool.execute("在原有红军长征 PPT 上补充重要会议和重要战役", context(), Map.of());

        PptArtifact artifact = (PptArtifact) result.getArtifact();
        assertThat(artifact.getSlides()).extracting("title").doesNotContain("方案设计", "实施计划");
    }

    private AgentContext context() {
        return new AgentContext(
                "task-1",
                "conversation-1",
                "user-1",
                AgentChannel.WEB,
                List.of("历史对话"),
                Map.of("source", "test")
        );
    }

    private GeneratePptTool createGeneratePptTool(ReliableChatService reliableChatService) {
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
