package org.example.ggbot.planner;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.tool.ToolName;

/**
 * `Plan` 中的单个执行步骤。
 *
 * <p>每一步都会对应一个具体工具，以及给这个工具的简要说明和输入指令。
 */
@Data
@RequiredArgsConstructor
public class PlanStep {

    private final String stepId;
    private final ToolName toolName;
    private final String description;
    private final String instruction;
    private StepStatus status = StepStatus.PENDING;
    private Object result;
    private String errorMessage;

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
