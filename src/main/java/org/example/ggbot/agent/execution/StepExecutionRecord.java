package org.example.ggbot.agent.execution;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.planner.StepStatus;
import org.example.ggbot.tool.ToolName;

@Data
@RequiredArgsConstructor
public class StepExecutionRecord {

    private final int iteration;
    private final String stepId;
    private final ToolName toolName;
    private final String instruction;
    private final StepStatus status;
    private final String thought;
    private final String observation;
    private final Object result;
    private final String errorMessage;
}
