package org.example.ggbot.planner;

import org.example.ggbot.tool.ToolName;

public enum StepType {
    GENERATE_DOC(ToolName.GENERATE_DOC),
    GENERATE_PPT(ToolName.GENERATE_PPT),
    SUMMARIZE(ToolName.SUMMARIZE),
    CLARIFY(null);

    private final ToolName defaultToolName;

    StepType(ToolName defaultToolName) {
        this.defaultToolName = defaultToolName;
    }

    public ToolName defaultToolName() {
        return defaultToolName;
    }

    public static StepType fromToolName(ToolName toolName) {
        if (toolName == null) {
            return CLARIFY;
        }
        return switch (toolName) {
            case GENERATE_DOC -> GENERATE_DOC;
            case GENERATE_PPT -> GENERATE_PPT;
            case MODIFY_PPT -> GENERATE_PPT;
            case SUMMARIZE -> SUMMARIZE;
        };
    }
}
