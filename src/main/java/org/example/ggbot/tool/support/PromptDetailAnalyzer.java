package org.example.ggbot.tool.support;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class PromptDetailAnalyzer {

    private static final List<String> DENSITY_HINTS = List.of(
            "包括", "涵盖", "分为", "按照以下", "根据以下内容", "根据以下材料", "包含",
            "章节", "要求", "部分", "目录", "结构", "案例", "背景", "建议"
    );

    public PromptDetailLevel analyze(String prompt) {
        String value = prompt == null ? "" : prompt.trim();
        int length = value.length();
        PromptDetailLevel baseLevel = classifyByLength(length);
        int score = structuralScore(value);

        if (baseLevel == PromptDetailLevel.BRIEF && score >= 1 && length >= 12) {
            return PromptDetailLevel.NORMAL;
        }
        if (baseLevel == PromptDetailLevel.NORMAL && score >= 4 && length >= 120) {
            return PromptDetailLevel.DETAILED;
        }
        return baseLevel;
    }

    private PromptDetailLevel classifyByLength(int length) {
        if (length < 40) {
            return PromptDetailLevel.BRIEF;
        }
        if (length < 180) {
            return PromptDetailLevel.NORMAL;
        }
        return PromptDetailLevel.DETAILED;
    }

    private int structuralScore(String prompt) {
        if (prompt.isBlank()) {
            return 0;
        }
        String normalized = prompt.toLowerCase(Locale.ROOT);
        int score = 0;
        long punctuationCount = normalized.chars()
                .filter(ch -> ch == '，' || ch == '、' || ch == ',' || ch == ';' || ch == '；')
                .count();
        if (punctuationCount >= 3) {
            score++;
        }
        if (normalized.contains("\n")) {
            score++;
        }
        if (normalized.matches("(?s).*(^|\\n)\\s*[0-9]+[.、].*")) {
            score++;
        }
        if (normalized.contains("- ") || normalized.contains("* ") || normalized.contains("•")) {
            score++;
        }
        long hintHits = DENSITY_HINTS.stream().filter(normalized::contains).count();
        if (hintHits >= 2) {
            score++;
        }
        return score;
    }
}
