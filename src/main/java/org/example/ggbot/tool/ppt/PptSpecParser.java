package org.example.ggbot.tool.ppt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PptSpecParser {

    private final ObjectMapper objectMapper;

    public PptSpecParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PptSpec parse(String rawContent) {
        try {
            PptSpec spec = objectMapper.readValue(stripCodeFence(rawContent), PptSpec.class);
            return normalize(spec);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to parse PPT spec JSON", ex);
        }
    }

    private String stripCodeFence(String rawContent) {
        String trimmed = rawContent == null ? "" : rawContent.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        int firstLineBreak = trimmed.indexOf('\n');
        int lastFence = trimmed.lastIndexOf("```");
        if (firstLineBreak < 0 || lastFence <= firstLineBreak) {
            return trimmed;
        }
        return trimmed.substring(firstLineBreak + 1, lastFence).trim();
    }

    private PptSpec normalize(PptSpec spec) {
        if (spec == null) {
            return new PptSpec(null, new ArrayList<>());
        }
        List<SlideSpec> slides = spec.getSlides() == null ? new ArrayList<>() : spec.getSlides();
        for (int index = 0; index < slides.size(); index++) {
            SlideSpec slide = slides.get(index);
            if (slide.getPageNumber() <= 0) {
                slide.setPageNumber(index + 1);
            }
            if (slide.getTitle() == null || slide.getTitle().isBlank()) {
                slide.setTitle("第 " + (index + 1) + " 页");
            }
            if (slide.getBullets() == null) {
                slide.setBullets(new ArrayList<>());
            }
        }
        spec.setSlides(slides);
        return spec;
    }
}
