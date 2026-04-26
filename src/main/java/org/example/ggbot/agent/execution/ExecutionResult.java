package org.example.ggbot.agent.execution;

import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.planner.PlanStep;

@Data
@RequiredArgsConstructor
public class ExecutionResult {

    private final List<PlanStep> executedSteps;
    private final List<StepExecutionRecord> records;
    private final boolean success;
    private final boolean hasFailures;
    private final String summary;

    public boolean hasFailures() {
        return hasFailures;
    }
}
