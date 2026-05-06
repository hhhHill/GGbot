package org.example.ggbot.tool.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.example.ggbot.tool.model.PptArtifact;
import org.example.ggbot.tool.ppt.PptSpec;
import org.example.ggbot.tool.ppt.SlideSpec;
import org.springframework.stereotype.Component;

@Component
public class ResultQualityEvaluator {

    public static final List<String> INTERNAL_MARKERS = List.of(
            "ToolResult(",
            "DocumentArtifact(",
            "PptArtifact(",
            "markdown=",
            "artifact=",
            "toolName=",
            "success=",
            "org.example",
            "src/main/java"
    );

    private static final List<String> DEMO_TITLES = List.of(
            "背景与目标",
            "方案设计",
            "实施计划",
            "结论与下一步"
    );

    private static final List<String> PROJECT_TEMPLATE_TERMS = List.of(
            "mvp",
            "接口",
            "业务目标",
            "用户价值",
            "数据协作流程",
            "可量化指标",
            "阶段性里程碑",
            "资源与风险"
    );

    private static final List<String> PLACEHOLDER_TERMS = List.of(
            "todo",
            "待补充",
            "此处省略",
            "示例内容",
            "根据实际情况调整",
            "占位"
    );

    public List<String> evaluateText(String text) {
        return evaluateText(text, PromptDetailLevel.NORMAL, 0);
    }

    public List<String> evaluateText(String text, PromptDetailLevel detailLevel, int originalInputLength) {
        List<String> issues = new ArrayList<>();
        if (text == null || text.isBlank()) {
            issues.add("content is blank");
            return issues;
        }
        addGeneralTextIssues(text, issues);
        if (text.length() < minDocumentLength(detailLevel, originalInputLength)) {
            issues.add("too_short");
        }
        return issues;
    }

    public List<String> evaluatePptSpec(PptSpec spec, String instruction) {
        return evaluatePptSpec(spec, instruction, PromptDetailLevel.NORMAL, instruction == null ? 0 : instruction.length());
    }

    public List<String> evaluatePptSpec(PptSpec spec, String instruction, PromptDetailLevel detailLevel, int originalInputLength) {
        List<String> issues = new ArrayList<>();
        if (spec == null) {
            issues.add("spec is null");
            return issues;
        }
        if (spec.getTitle() == null || spec.getTitle().isBlank()) {
            issues.add("title is blank");
        }
        StringBuilder combined = new StringBuilder(spec.getTitle() == null ? "" : spec.getTitle());
        int demoHits = 0;
        int projectTermHits = 0;
        if (spec.getSlides() != null) {
            for (SlideSpec slide : spec.getSlides()) {
                if (slide.getTitle() != null) {
                    combined.append('\n').append(slide.getTitle());
                    if (DEMO_TITLES.contains(slide.getTitle())) {
                        demoHits++;
                    }
                }
                if (slide.getBullets() != null) {
                    for (String bullet : slide.getBullets()) {
                        combined.append('\n').append(bullet);
                    }
                }
                if (slide.getSubtitle() == null || slide.getSubtitle().isBlank()) {
                    issues.add("too_sparse");
                }
                if (slide.getBullets() == null || slide.getBullets().size() < 4) {
                    issues.add("too_sparse");
                }
                if (slide.getSpeakerNotes() == null || slide.getSpeakerNotes().isBlank() || slide.getSpeakerNotes().length() < 120) {
                    issues.add("too_sparse");
                }
            }
        }
        String normalized = combined.toString().toLowerCase(Locale.ROOT);
        for (String term : PROJECT_TEMPLATE_TERMS) {
            if (normalized.contains(term.toLowerCase(Locale.ROOT))) {
                projectTermHits++;
            }
        }
        addGeneralTextIssues(combined.toString(), issues);
        if (spec.getSlides() == null || spec.getSlides().size() < minSlideCount(detailLevel)) {
            issues.add("too_short");
        }
        if (demoHits >= 3 && !isProjectTopic(instruction)) {
            issues.add("matched fixed demo template");
        }
        if (projectTermHits >= 3 && !isProjectTopic(instruction)) {
            issues.add("contains too many project-template terms");
        }
        if (!containsTopicKeyword(spec, instruction)) {
            issues.add("missing topic keywords from instruction");
        }
        return issues;
    }

    public List<String> evaluatePptArtifact(PptArtifact artifact, String instruction) {
        return evaluatePptArtifact(artifact, instruction, PromptDetailLevel.NORMAL, instruction == null ? 0 : instruction.length());
    }

    public List<String> evaluatePptArtifact(PptArtifact artifact, String instruction, PromptDetailLevel detailLevel, int originalInputLength) {
        if (artifact == null) {
            return List.of("artifact is null");
        }
        List<SlideSpec> slides = artifact.getSlides().stream()
                .map(slide -> new SlideSpec(
                        slide.getPageNumber(),
                        slide.getTitle(),
                        slide.getSubtitle(),
                        slide.getBullets(),
                        slide.getSpeakerNotes()))
                .toList();
        return evaluatePptSpec(new PptSpec(artifact.getTitle(), slides), instruction, detailLevel, originalInputLength);
    }

    public String sanitizeInternalMarkers(String text) {
        String value = text == null ? "" : text;
        for (String marker : INTERNAL_MARKERS) {
            value = value.replace(marker, "");
        }
        return value.trim();
    }

    private boolean isProjectTopic(String instruction) {
        String normalized = instruction == null ? "" : instruction.toLowerCase(Locale.ROOT);
        return normalized.contains("项目")
                || normalized.contains("方案")
                || normalized.contains("mvp")
                || normalized.contains("接口")
                || normalized.contains("项目汇报");
    }

    private boolean containsTopicKeyword(PptSpec spec, String instruction) {
        if (instruction == null || instruction.isBlank()) {
            return true;
        }
        String corpus = buildCorpus(spec).toLowerCase(Locale.ROOT);
        String normalizedInstruction = instruction.toLowerCase(Locale.ROOT);
        if (corpus.contains(normalizedInstruction) || normalizedInstruction.contains(corpus)) {
            return true;
        }
        if (spec.getTitle() != null && !spec.getTitle().isBlank()) {
            String title = spec.getTitle().toLowerCase(Locale.ROOT);
            if (normalizedInstruction.contains(title) || title.contains(normalizedInstruction)) {
                return true;
            }
        }
        List<String> keywords = extractKeywords(instruction);
        if (keywords.isEmpty()) {
            return true;
        }
        return keywords.stream().anyMatch(keyword -> corpus.contains(keyword.toLowerCase(Locale.ROOT)));
    }

    private List<String> extractKeywords(String instruction) {
        if (instruction == null || instruction.isBlank()) {
            return List.of();
        }
        String normalized = instruction
                .replace("请生成", " ")
                .replace("请介绍", " ")
                .replace("请基于", " ")
                .replace("生成", " ")
                .replace("汇报", " ")
                .replace("PPT", " ")
                .replace("ppt", " ")
                .replace("文档", " ")
                .replace("根据以下内容", " ")
                .replace("根据以下材料", " ")
                .replace("根据", " ")
                .replace("按照以下", " ")
                .replace("按照", " ")
                .replace("关于", " ")
                .replace("的", " ")
                .replace("和", " ")
                .replace("与", " ")
                .replace("及", " ")
                .replaceAll("[\\p{Punct}\\s]+", " ")
                .trim();
        return java.util.Arrays.stream(normalized.split(" "))
                .filter(token -> token.length() >= 2)
                .distinct()
                .limit(8)
                .toList();
    }

    private String buildCorpus(PptSpec spec) {
        StringBuilder builder = new StringBuilder();
        builder.append(spec.getTitle() == null ? "" : spec.getTitle());
        if (spec.getSlides() == null) {
            return builder.toString();
        }
        for (SlideSpec slide : spec.getSlides()) {
            builder.append('\n').append(slide.getTitle() == null ? "" : slide.getTitle());
            if (slide.getBullets() != null) {
                slide.getBullets().forEach(bullet -> builder.append('\n').append(bullet));
            }
        }
        return builder.toString();
    }

    private void addGeneralTextIssues(String text, List<String> issues) {
        INTERNAL_MARKERS.stream()
                .filter(text::contains)
                .forEach(marker -> issues.add("contains internal marker: " + marker));
        String normalized = text.toLowerCase(Locale.ROOT);
        PLACEHOLDER_TERMS.stream()
                .filter(normalized::contains)
                .forEach(term -> issues.add("contains placeholder: " + term));
    }

    private int minDocumentLength(PromptDetailLevel detailLevel, int originalInputLength) {
        return switch (detailLevel) {
            case BRIEF -> 1200;
            case NORMAL -> 1000;
            case DETAILED -> Math.max(800, originalInputLength);
        };
    }

    private int minSlideCount(PromptDetailLevel detailLevel) {
        return switch (detailLevel) {
            case BRIEF -> 7;
            case NORMAL -> 6;
            case DETAILED -> 5;
        };
    }
}
