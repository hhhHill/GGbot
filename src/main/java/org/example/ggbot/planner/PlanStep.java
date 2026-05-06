package org.example.ggbot.planner;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.example.ggbot.tool.ToolName;

/**
 * `Plan` 中的单个执行步骤。
 *
 * <p>每一步都会对应一个具体工具，以及给这个工具的简要说明和输入指令。
 */
@Data
public class PlanStep {

    private final String stepId;
    private final StepType type;
    private final ToolName toolName;
    private final String description;
    private final String instruction;
    private final List<String> dependsOn;
    private final List<String> inputRefs;
    private final String expectedOutput;
    private StepStatus status = StepStatus.PENDING;
    private Object result;
    private String errorMessage;

    public PlanStep(String stepId, ToolName toolName, String description, String instruction) {
        this(stepId, StepType.fromToolName(toolName), toolName, description, instruction, List.of(), List.of(), null);
    }

    public PlanStep(
            String stepId,
            StepType type,
            ToolName toolName,
            String description,
            String instruction,
            List<String> dependsOn,
            List<String> inputRefs,
            String expectedOutput
    ) {
        this.stepId = stepId;
        this.type = type == null ? StepType.fromToolName(toolName) : type;
        this.toolName = toolName != null ? toolName : this.type.defaultToolName();
        this.description = description;
        this.instruction = instruction;
        this.dependsOn = new ArrayList<>(dependsOn == null ? List.of() : dependsOn);
        this.inputRefs = new ArrayList<>(inputRefs == null ? List.of() : inputRefs);
        this.expectedOutput = expectedOutput;
    }

    public void markSuccess(Object result) {
        this.status = StepStatus.SUCCESS;
        this.result = result;
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = StepStatus.FAILED;
        this.errorMessage = errorMessage;
    }
}
