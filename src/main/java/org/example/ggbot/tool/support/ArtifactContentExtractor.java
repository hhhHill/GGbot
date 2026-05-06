package org.example.ggbot.tool.support;

import java.util.stream.Collectors;
import org.example.ggbot.tool.ToolResult;
import org.example.ggbot.tool.model.DocumentArtifact;
import org.example.ggbot.tool.model.PptArtifact;
import org.example.ggbot.tool.model.Slide;
import org.springframework.stereotype.Component;

@Component
public class ArtifactContentExtractor {

    private final ResultQualityEvaluator resultQualityEvaluator;

    public ArtifactContentExtractor(ResultQualityEvaluator resultQualityEvaluator) {
        this.resultQualityEvaluator = resultQualityEvaluator;
    }

    public ArtifactContentExtractor() {
        this(new ResultQualityEvaluator());
    }

    public String extract(ToolResult result) {
        if (result == null) {
            return "";
        }
        Object artifact = result.getArtifact();
        String content;
        if (artifact instanceof DocumentArtifact documentArtifact) {
            content = documentArtifact.getMarkdown();
        } else if (artifact instanceof PptArtifact pptArtifact) {
            content = renderPptArtifact(pptArtifact);
        } else if (artifact == null) {
            content = result.getSummary();
        } else {
            content = result.getSummary();
        }
        return resultQualityEvaluator.sanitizeInternalMarkers(content);
    }

    private String renderPptArtifact(PptArtifact artifact) {
        return artifact.getSlides().stream()
                .map(this::renderSlide)
                .collect(Collectors.joining("\n\n"));
    }

    private String renderSlide(Slide slide) {
        StringBuilder builder = new StringBuilder();
        builder.append(slide.getPageNumber()).append(". ").append(slide.getTitle());
        if (slide.getSubtitle() != null && !slide.getSubtitle().isBlank()) {
            builder.append(" / ").append(slide.getSubtitle());
        }
        builder.append("\n- ").append(String.join("\n- ", slide.getBullets()));
        if (slide.getSpeakerNotes() != null && !slide.getSpeakerNotes().isBlank()) {
            builder.append("\n讲稿：").append(slide.getSpeakerNotes());
        }
        return builder.toString();
    }
}
