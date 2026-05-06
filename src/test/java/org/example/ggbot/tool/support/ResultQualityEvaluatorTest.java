package org.example.ggbot.tool.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.example.ggbot.tool.ppt.PptSpec;
import org.example.ggbot.tool.ppt.SlideSpec;
import org.junit.jupiter.api.Test;

class ResultQualityEvaluatorTest {

    private final ResultQualityEvaluator evaluator = new ResultQualityEvaluator();

    @Test
    void shouldFlagBriefDocumentAsTooShort() {
        String text = "广州".repeat(400);

        assertThat(evaluator.evaluateText(text, PromptDetailLevel.BRIEF, 4))
                .contains("too_short");
    }

    @Test
    void shouldFlagDetailedDocumentAgainstOriginalInputLength() {
        String text = "广州发展".repeat(120);
        int originalInputLength = 1200;

        assertThat(evaluator.evaluateText(text, PromptDetailLevel.DETAILED, originalInputLength))
                .contains("too_short");
    }

    @Test
    void shouldFlagSparsePptForBriefPrompt() {
        PptSpec spec = new PptSpec("广州发展", List.of(
                new SlideSpec(1, "阶段一", "背景概览", List.of("要点1", "要点2", "要点3"), null),
                new SlideSpec(2, "阶段二", "产业升级", List.of("要点1", "要点2", "要点3", "要点4"), "讲稿"),
                new SlideSpec(3, "阶段三", "交通建设", List.of("要点1", "要点2", "要点3", "要点4"), "讲稿"),
                new SlideSpec(4, "阶段四", "开放格局", List.of("要点1", "要点2", "要点3", "要点4"), "讲稿"),
                new SlideSpec(5, "阶段五", "案例分析", List.of("要点1", "要点2", "要点3", "要点4"), "讲稿"),
                new SlideSpec(6, "阶段六", "现实挑战", List.of("要点1", "要点2", "要点3", "要点4"), "讲稿")
        ));

        assertThat(evaluator.evaluatePptSpec(spec, "广州发展历程", PromptDetailLevel.BRIEF, 6))
                .contains("too_short", "too_sparse");
    }
}
