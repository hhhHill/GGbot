package org.example.ggbot.tool.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.example.ggbot.ai.ContextAwareChatService;
import org.example.ggbot.tool.support.PromptDetailAnalyzer;
import org.example.ggbot.tool.support.PromptDetailLevel;
import org.example.ggbot.prompt.ClasspathPromptRepository;
import org.example.ggbot.tool.model.DocumentArtifact;
import org.junit.jupiter.api.Test;

class GenerateDocToolTest {

    @Test
    void shouldUseBriefStrategyForShortPrompt() {
        ContextAwareChatService chatService = mock(ContextAwareChatService.class);
        ClasspathPromptRepository promptRepository = mock(ClasspathPromptRepository.class);
        PromptDetailAnalyzer analyzer = mock(PromptDetailAnalyzer.class);
        when(chatService.isAvailable()).thenReturn(true);
        when(analyzer.analyze("介绍广州")).thenReturn(PromptDetailLevel.BRIEF);
        when(promptRepository.load("generate-doc-system-prompt.txt")).thenReturn("doc-system");
        when(promptRepository.load("generate-doc-user-prompt.txt", Map.of(
                "title", "介绍广州",
                "prompt", "介绍广州",
                "promptDetailLevel", "BRIEF"
        ))).thenReturn("doc-user");
        String markdown = """
                # 广州

                广州是中国南方重要中心城市。%s
                """.formatted("广州拥有两千多年建城史，作为海上丝绸之路的重要节点，长期承担商贸枢纽角色。".repeat(40));
        when(chatService.chat("doc-system", "doc-user", null)).thenReturn(markdown);

        GenerateDocTool tool = new GenerateDocTool(chatService, promptRepository, analyzer);

        DocumentArtifact artifact = tool.generateDoc("介绍广州");

        assertThat(artifact.getMarkdown().length()).isGreaterThanOrEqualTo(1200);
        assertThat(artifact.getMarkdown()).contains("广州", "海上丝绸之路");
        assertThat(artifact.getMarkdown()).doesNotContain("TODO", "待补充", "大纲");
        verify(promptRepository).load("generate-doc-user-prompt.txt", Map.of(
                "title", "介绍广州",
                "prompt", "介绍广州",
                "promptDetailLevel", "BRIEF"
        ));
    }

    @Test
    void shouldKeepUserFactsForDetailedPrompt() {
        ContextAwareChatService chatService = mock(ContextAwareChatService.class);
        ClasspathPromptRepository promptRepository = mock(ClasspathPromptRepository.class);
        PromptDetailAnalyzer analyzer = mock(PromptDetailAnalyzer.class);
        String prompt = """
                请根据以下材料整理广州城市发展文档：广州依托千年商都基础形成外贸优势，改革开放后依靠制造业和港口体系快速增长，近年来重点发展南沙、自贸区、琶洲数字经济和国际消费中心城市建设。请保留这些重点，并分为历史基础、产业升级、交通枢纽、典型案例、挑战与建议五部分。
                """;
        when(chatService.isAvailable()).thenReturn(true);
        when(analyzer.analyze(prompt)).thenReturn(PromptDetailLevel.DETAILED);
        when(promptRepository.load("generate-doc-system-prompt.txt")).thenReturn("doc-system");
        when(promptRepository.load(eq("generate-doc-user-prompt.txt"), anyMap())).thenReturn("doc-user-detailed");
        String markdown = """
                # 广州城市发展

                广州依托千年商都基础形成外贸优势，改革开放后依靠制造业和港口体系快速增长。

                ## 典型案例
                南沙、自贸区、琶洲数字经济构成广州新阶段的重要抓手，同时延伸出国际消费中心城市建设。

                ## 挑战与建议
                在保留原有制造与商贸优势的同时，需要进一步提升创新转化效率与全球资源配置能力。
                %s
                """.formatted("广州城市发展需要在既有基础上推进制度创新与产业协同。".repeat(30));
        when(chatService.chat("doc-system", "doc-user-detailed", null)).thenReturn(markdown);

        GenerateDocTool tool = new GenerateDocTool(chatService, promptRepository, analyzer);

        DocumentArtifact artifact = tool.generateDoc(prompt);

        assertThat(artifact.getMarkdown()).contains("千年商都", "南沙", "自贸区", "琶洲数字经济");
        assertThat(artifact.getMarkdown()).doesNotContain("深圳", "北京中关村");
    }

    @Test
    void shouldFallbackToTemplateWhenChatServiceUnavailable() {
        ContextAwareChatService chatService = mock(ContextAwareChatService.class);
        ClasspathPromptRepository promptRepository = mock(ClasspathPromptRepository.class);
        PromptDetailAnalyzer analyzer = new PromptDetailAnalyzer();
        when(chatService.isAvailable()).thenReturn(false);
        when(promptRepository.load(anyString(), org.mockito.ArgumentMatchers.<Map<String, String>>any()))
                .thenReturn("# 主题文档\n\n广州是重要城市。");

        GenerateDocTool tool = new GenerateDocTool(chatService, promptRepository, analyzer);

        DocumentArtifact artifact = tool.generateDoc("介绍广州");

        assertThat(artifact.getMarkdown()).contains("广州");
    }
}
