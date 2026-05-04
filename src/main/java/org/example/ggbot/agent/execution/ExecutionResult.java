package org.example.ggbot.agent.execution;

import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.planner.PlanStep;

/**
 * 执行结果对象，封装一次执行的所有结果信息
 */
@Data
@RequiredArgsConstructor
public class ExecutionResult {

    /** 已执行的步骤列表 */
    private final List<PlanStep> executedSteps;
    /** 执行记录详情列表 */
    private final List<StepExecutionRecord> records;
    /** 执行是否成功 */
    private final boolean success;
    /** 是否有失败的步骤 */
    private final boolean hasFailures;
    /** 执行结果摘要 */
    private final String summary;

    public boolean hasFailures() {
        return hasFailures;
    }
}
