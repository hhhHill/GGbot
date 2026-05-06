package org.example.ggbot.tool.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.example.ggbot.tool.ToolName;
import org.example.ggbot.tool.ToolResult;
import org.example.ggbot.tool.model.DocumentArtifact;
import org.example.ggbot.tool.model.PptArtifact;
import org.example.ggbot.tool.model.Slide;
import org.junit.jupiter.api.Test;

class ArtifactContentExtractorTest {

    private final ArtifactContentExtractor extractor = new ArtifactContentExtractor(new ResultQualityEvaluator());

    @Test
    void shouldReturnMarkdownForDocumentArtifact() {
        ToolResult result = new ToolResult(
                ToolName.GENERATE_DOC,
                true,
                "文档已生成",
                new DocumentArtifact("红军长征", "# 红军长征")
        );

        assertThat(extractor.extract(result)).isEqualTo("# 红军长征");
    }

    @Test
    void shouldRenderSlidesForPptArtifact() {
        ToolResult result = new ToolResult(
                ToolName.GENERATE_PPT,
                true,
                "PPT 已生成",
                new PptArtifact(
                        "红军长征",
                        List.of(new Slide(1, "历史背景", "阶段背景", List.of("第五次反围剿失利", "战略转移开始", "形势严峻", "中央红军被迫转移"), "本页讲稿用于说明历史背景。")),
                        "generated/pptx/demo.pptx"
                )
        );

        String extracted = extractor.extract(result);

        assertThat(extracted).contains("历史背景");
        assertThat(extracted).doesNotContain("PptArtifact(");
    }
}
