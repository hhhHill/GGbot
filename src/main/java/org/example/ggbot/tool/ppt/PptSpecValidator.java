package org.example.ggbot.tool.ppt;

import java.util.ArrayList;
import java.util.List;
import org.example.ggbot.tool.support.PromptDetailLevel;
import org.example.ggbot.tool.support.ResultQualityEvaluator;
import org.springframework.stereotype.Component;

@Component
public class PptSpecValidator {

    private final ResultQualityEvaluator resultQualityEvaluator;

    public PptSpecValidator(ResultQualityEvaluator resultQualityEvaluator) {
        this.resultQualityEvaluator = resultQualityEvaluator;
    }

    public PptSpecValidator() {
        this(new ResultQualityEvaluator());
    }

    public ValidationResult validate(PptSpec spec, String instruction) {
        return validate(spec, instruction, PromptDetailLevel.NORMAL, instruction == null ? 0 : instruction.length());
    }

    public ValidationResult validate(PptSpec spec, String instruction, PromptDetailLevel detailLevel, int originalInputLength) {
        List<String> errors = new ArrayList<>();
        if (spec == null) {
            errors.add("spec is null");
            return new ValidationResult(false, errors);
        }
        if (spec.getTitle() == null || spec.getTitle().isBlank()) {
            errors.add("title is blank");
        }
        if (spec.getSlides() == null || spec.getSlides().isEmpty()) {
            errors.add("slides are empty");
        }
        errors.addAll(resultQualityEvaluator.evaluatePptSpec(spec, instruction, detailLevel, originalInputLength));
        return new ValidationResult(errors.isEmpty(), errors);
    }

    public record ValidationResult(boolean valid, List<String> errors) {
    }
}
