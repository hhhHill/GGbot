package org.example.ggbot.tool.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.example.ggbot.agent.AgentChannel;
import org.example.ggbot.agent.AgentContext;
import org.example.ggbot.prompt.ClasspathPromptRepository;
import org.example.ggbot.tool.ToolName;
import org.example.ggbot.tool.ToolResult;
import org.example.ggbot.tool.model.DocumentArtifact;
import org.example.ggbot.tool.model.PptArtifact;
import org.example.ggbot.tool.ppt.PptSpecGenerationClient;
import org.example.ggbot.tool.ppt.PptSpecParser;
import org.example.ggbot.tool.ppt.PptSpecValidator;
import org.example.ggbot.tool.ppt.PptxRenderer;
import org.example.ggbot.tool.ppt.SemanticPptFallbackGenerator;
import org.example.ggbot.tool.support.PromptDetailAnalyzer;
import org.example.ggbot.tool.support.PromptDetailLevel;
import org.example.ggbot.tool.support.ArtifactContentExtractor;
import org.example.ggbot.tool.support.ResultQualityEvaluator;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GeneratePptToolTest {

    @Test
    void shouldGeneratePptArtifactFromValidJson() {
        PptSpecGenerationClient client = Mockito.mock(PptSpecGenerationClient.class);
        when(client.generate(anyString())).thenReturn("""
                {
                  "title": "红军长征",
                  "slides": [
                    {"title": "长征概览", "subtitle": "主题与范围", "bullets": ["重大历史事件", "战略转移过程", "红军核心目标", "汇报范围说明"], "speakerNotes": "%s"},
                    {"title": "历史背景", "subtitle": "危机形成", "bullets": ["反围剿失利", "根据地压力", "军事路线问题", "战略转移启动"], "speakerNotes": "%s"},
                    {"title": "重要会议", "subtitle": "转折节点", "bullets": ["遵义会议召开", "确立正确领导", "扭转被动局面", "统一战略方向"], "speakerNotes": "%s"},
                    {"title": "主要战役", "subtitle": "关键过程", "bullets": ["突破封锁线", "四渡赤水", "巧渡金沙江", "会师北上"], "speakerNotes": "%s"},
                    {"title": "历史意义", "subtitle": "长期影响", "bullets": ["保存革命力量", "形成长征精神", "影响革命进程", "奠定胜利基础"], "speakerNotes": "%s"},
                    {"title": "总结启示", "subtitle": "现实价值", "bullets": ["战略转折重要", "组织能力重建", "精神价值沉淀", "现实启示鲜明"], "speakerNotes": "%s"}
                  ]
                }
                """.formatted(longNotes(), longNotes(), longNotes(), longNotes(), longNotes(), longNotes()));
        PromptDetailAnalyzer analyzer = mock(PromptDetailAnalyzer.class);
        when(analyzer.analyze("请基于红军长征生成汇报 PPT")).thenReturn(PromptDetailLevel.NORMAL);

        GeneratePptTool tool = new GeneratePptTool(
                client,
                new PptSpecParser(new ObjectMapper()),
                new PptSpecValidator(new ResultQualityEvaluator()),
                new SemanticPptFallbackGenerator(),
                new PptxRenderer(Path.of("target/test-generated/ppt-tool")),
                new ArtifactContentExtractor(new ResultQualityEvaluator()),
                new ClasspathPromptRepository(),
                analyzer
        );

        ToolResult result = tool.execute("请基于红军长征生成汇报 PPT", context(), Map.of());

        assertThat(result.getToolName()).isEqualTo(ToolName.GENERATE_PPT);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getArtifact()).isInstanceOf(PptArtifact.class);
        assertThat(((PptArtifact) result.getArtifact()).getFilePath()).endsWith(".pptx");
    }

    @Test
    void shouldFallbackWhenJsonIsInvalidTwice() {
        PptSpecGenerationClient client = Mockito.mock(PptSpecGenerationClient.class);
        when(client.generate(anyString())).thenReturn("not json");

        GeneratePptTool tool = new GeneratePptTool(
                client,
                new PptSpecParser(new ObjectMapper()),
                new PptSpecValidator(new ResultQualityEvaluator()),
                new SemanticPptFallbackGenerator(),
                new PptxRenderer(Path.of("target/test-generated/ppt-fallback")),
                new ArtifactContentExtractor(new ResultQualityEvaluator()),
                new ClasspathPromptRepository(),
                new PromptDetailAnalyzer()
        );

        ToolResult result = tool.execute("请介绍红军长征的历史背景和重要战役", context(), Map.of());

        PptArtifact artifact = (PptArtifact) result.getArtifact();
        assertThat(result.isSuccess()).isTrue();
        assertThat(artifact.getTitle()).contains("红军长征");
        assertThat(artifact.getSlides()).extracting("title").doesNotContain("方案设计", "实施计划");
    }

    @Test
    void shouldBuildInstructionSectionsFromPromptTemplates() {
        PptSpecGenerationClient client = Mockito.mock(PptSpecGenerationClient.class);
        ClasspathPromptRepository promptRepository = Mockito.mock(ClasspathPromptRepository.class);
        PromptDetailAnalyzer analyzer = mock(PromptDetailAnalyzer.class);
        when(analyzer.analyze("请生成 AI 趋势汇报 PPT\n\n前置文档内容：\ndoc:\n# AI 趋势")).thenReturn(PromptDetailLevel.NORMAL);
        when(promptRepository.load("ppt-step-results-section.txt", Map.of("stepResults", "doc:\n# AI 趋势"))).thenReturn(
                "前置文档内容：\ndoc:\n# AI 趋势");
        when(promptRepository.load("generate-ppt-user-prompt.txt", Map.of(
                "prompt", "请生成 AI 趋势汇报 PPT\n\n前置文档内容：\ndoc:\n# AI 趋势",
                "promptDetailLevel", "NORMAL"
        ))).thenReturn("ppt-user");
        when(client.generate(anyString())).thenReturn("""
                {
                  "title": "AI 趋势",
                  "slides": [
                    {"title": "趋势概览", "subtitle": "总览", "bullets": ["趋势一", "趋势二", "趋势三", "趋势四"], "speakerNotes": "%s"},
                    {"title": "关键变化", "subtitle": "变化", "bullets": ["变化一", "变化二", "变化三", "变化四"], "speakerNotes": "%s"},
                    {"title": "落地建议", "subtitle": "建议", "bullets": ["建议一", "建议二", "建议三", "建议四"], "speakerNotes": "%s"},
                    {"title": "总结", "subtitle": "收束", "bullets": ["总结一", "总结二", "总结三", "总结四"], "speakerNotes": "%s"},
                    {"title": "附录", "subtitle": "补充", "bullets": ["补充一", "补充二", "补充三", "补充四"], "speakerNotes": "%s"}
                  ]
                }
                """.formatted(longNotes(), longNotes(), longNotes(), longNotes(), longNotes()));

        GeneratePptTool tool = new GeneratePptTool(
                client,
                new PptSpecParser(new ObjectMapper()),
                new PptSpecValidator(new ResultQualityEvaluator()),
                new SemanticPptFallbackGenerator(),
                new PptxRenderer(Path.of("target/test-generated/ppt-template")),
                new ArtifactContentExtractor(new ResultQualityEvaluator()),
                promptRepository,
                analyzer
        );

        tool.execute("请生成 AI 趋势汇报 PPT", context(), Map.of(
                "stepResults",
                Map.of("doc", new ToolResult(ToolName.GENERATE_DOC, true, "ok", new DocumentArtifact("AI 趋势", "# AI 趋势")))
        ));

        verify(promptRepository).load("ppt-step-results-section.txt", Map.of("stepResults", "doc:\n# AI 趋势"));
        verify(promptRepository).load("generate-ppt-user-prompt.txt", Map.of(
                "prompt", "请生成 AI 趋势汇报 PPT\n\n前置文档内容：\ndoc:\n# AI 趋势",
                "promptDetailLevel", "NORMAL"
        ));
        verify(client).generate("ppt-user");
    }

    @Test
    void shouldGenerateRichDeckForBriefPrompt() {
        PptSpecGenerationClient client = Mockito.mock(PptSpecGenerationClient.class);
        ClasspathPromptRepository promptRepository = Mockito.mock(ClasspathPromptRepository.class);
        PromptDetailAnalyzer analyzer = mock(PromptDetailAnalyzer.class);
        when(analyzer.analyze("广州发展历程")).thenReturn(PromptDetailLevel.BRIEF);
        when(promptRepository.load("generate-ppt-user-prompt.txt", Map.of(
                "prompt", "广州发展历程",
                "promptDetailLevel", "BRIEF"
        ))).thenReturn("ppt-brief");
        when(client.generate("ppt-brief")).thenReturn(buildGuangzhouDeckJson(7));

        GeneratePptTool tool = new GeneratePptTool(
                client,
                new PptSpecParser(new ObjectMapper()),
                new PptSpecValidator(new ResultQualityEvaluator()),
                new SemanticPptFallbackGenerator(),
                new PptxRenderer(Path.of("target/test-generated/ppt-brief")),
                new ArtifactContentExtractor(new ResultQualityEvaluator()),
                promptRepository,
                analyzer
        );

        PptArtifact artifact = (PptArtifact) tool.execute("广州发展历程", context(), Map.of()).getArtifact();

        assertThat(artifact.getSlides()).hasSizeBetween(7, 10);
        assertThat(artifact.getSlides()).allSatisfy(slide -> {
            assertThat(slide.getBullets()).hasSizeBetween(4, 6);
            assertThat(slide.getSpeakerNotes()).hasSizeGreaterThanOrEqualTo(120);
        });
        assertThat(artifact.getSlides().getFirst().getTitle()).contains("广州");
    }

    @Test
    void shouldKeepDetailedPromptAsPrimarySource() {
        PptSpecGenerationClient client = Mockito.mock(PptSpecGenerationClient.class);
        ClasspathPromptRepository promptRepository = Mockito.mock(ClasspathPromptRepository.class);
        PromptDetailAnalyzer analyzer = mock(PromptDetailAnalyzer.class);
        String prompt = """
                请根据以下材料生成广州城市发展汇报：重点保留千年商都、制造业升级、南沙、自贸区、琶洲数字经济、国际消费中心城市建设，并分为历史基础、产业结构、枢纽交通、重点片区、挑战建议五部分。
                """;
        String normalizedPrompt = prompt.trim();
        when(analyzer.analyze(normalizedPrompt)).thenReturn(PromptDetailLevel.DETAILED);
        when(promptRepository.load("generate-ppt-user-prompt.txt", Map.of(
                "prompt", normalizedPrompt,
                "promptDetailLevel", "DETAILED"
        ))).thenReturn("ppt-detailed");
        when(client.generate("ppt-detailed")).thenReturn(buildDetailedGuangzhouDeckJson());

        GeneratePptTool tool = new GeneratePptTool(
                client,
                new PptSpecParser(new ObjectMapper()),
                new PptSpecValidator(new ResultQualityEvaluator()),
                new SemanticPptFallbackGenerator(),
                new PptxRenderer(Path.of("target/test-generated/ppt-detailed")),
                new ArtifactContentExtractor(new ResultQualityEvaluator()),
                promptRepository,
                analyzer
        );

        PptArtifact artifact = (PptArtifact) tool.execute(prompt, context(), Map.of()).getArtifact();

        assertThat(artifact.getSlides()).hasSizeBetween(5, 8);
        String combined = artifact.getSlides().toString();
        assertThat(combined).contains("千年商都", "南沙", "自贸区", "琶洲数字经济");
        assertThat(combined).doesNotContain("北京中关村", "上海迪士尼");
    }

    private AgentContext context() {
        return new AgentContext("task-1", "conversation-1", "user-1", AgentChannel.WEB, List.of(), Map.of());
    }

    private static String longNotes() {
        return """
                本页讲稿围绕主题展开，补充背景、逻辑关系、案例细节与结论说明，确保讲述时不仅复述要点，还能够解释原因、过程、影响与下一步建议。
                讲述时需要先交代这一页与整体汇报结构之间的关系，再说明核心事实、关键案例和结论判断，最后补充实际启示与延伸信息。
                还要把这一页与前后页面的衔接逻辑讲清楚，让听众理解为什么这些内容会共同支撑最终结论，并帮助听众形成完整、连续且有判断力的理解。
                """.replace("\n", "");
    }

    private static String buildGuangzhouDeckJson(int slideCount) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"title\":\"广州发展历程\",\"slides\":[");
        for (int index = 1; index <= slideCount; index++) {
            if (index > 1) {
                builder.append(',');
            }
            builder.append("""
                    {"pageNumber":%d,"title":"广州发展第%d页","subtitle":"广州阶段分析","bullets":["广州历史基础","广州产业升级","广州交通枢纽","广州案例支撑"],"speakerNotes":"%s"}
                    """.formatted(index, index, longNotes()));
        }
        builder.append("]}");
        return builder.toString();
    }

    private static String buildDetailedGuangzhouDeckJson() {
        return """
                {
                  "title": "广州城市发展汇报",
                  "slides": [
                    {"pageNumber":1,"title":"千年商都基础","subtitle":"历史基底","bullets":["千年商都形成外贸优势","岭南门户带动商贸流通","港口传统支撑开放格局","城市品牌持续积累"],"speakerNotes":"%s"},
                    {"pageNumber":2,"title":"制造业升级","subtitle":"产业演变","bullets":["制造业体系推动增长","工业升级带动结构优化","外向型产业形成规模","创新链条逐步完善"],"speakerNotes":"%s"},
                    {"pageNumber":3,"title":"南沙与自贸区","subtitle":"重点片区","bullets":["南沙承担开放平台职能","自贸区推动制度创新","港航资源加速集聚","区域协同空间扩大"],"speakerNotes":"%s"},
                    {"pageNumber":4,"title":"琶洲数字经济","subtitle":"新动能","bullets":["琶洲集聚数字平台企业","会展经济与数字经济叠加","总部资源形成示范效应","创新服务能力持续增强"],"speakerNotes":"%s"},
                    {"pageNumber":5,"title":"国际消费中心城市建设","subtitle":"城市能级","bullets":["消费场景持续升级","商旅文融合更加明显","国际品牌与本土品牌协同","城市吸引力稳步增强"],"speakerNotes":"%s"}
                  ]
                }
                """.formatted(longNotes(), longNotes(), longNotes(), longNotes(), longNotes());
    }
}
