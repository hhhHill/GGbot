package org.example.ggbot.planner;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.tool.ToolName;

@Data
@RequiredArgsConstructor
public class PlanStep {

    private final ToolName toolName;
    private final String description;
    private final String instruction;
}
