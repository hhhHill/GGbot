package org.example.ggbot.tool.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PromptDetailAnalyzerTest {

    private final PromptDetailAnalyzer analyzer = new PromptDetailAnalyzer();

    @Test
    void shouldClassifyBriefPrompt() {
        assertThat(analyzer.analyze("介绍广州")).isEqualTo(PromptDetailLevel.BRIEF);
    }

    @Test
    void shouldClassifyNormalPrompt() {
        assertThat(analyzer.analyze("生成一篇介绍广州的文档，包含历史、交通、经济、美食"))
                .isEqualTo(PromptDetailLevel.NORMAL);
    }

    @Test
    void shouldClassifyDetailedPrompt() {
        String prompt = """
                请基于以下材料整理一篇关于广州城市发展的分析文档，按照以下内容输出：1. 历史阶段与关键转折；2. 产业结构演变；3. 枢纽交通体系；4. 商贸与外向型经济；5. 科技创新与现代服务业；6. 典型案例，包括南沙、自贸区、琶洲数字经济；7. 当前挑战与建议。
                文档需要涵盖背景、数据口径说明、案例分析、阶段总结，并根据以下内容保留我提供的重点，不要偏离广州城市发展这个主题。
                """;

        assertThat(analyzer.analyze(prompt)).isEqualTo(PromptDetailLevel.DETAILED);
    }
}
