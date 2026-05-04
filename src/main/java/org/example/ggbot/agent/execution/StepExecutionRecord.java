package org.example.ggbot.agent.execution;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.planner.StepStatus;
import org.example.ggbot.tool.ToolName;

/**
 * 步骤执行记录，保存单个步骤执行的所有详情
 * 用于审计、调试和展示执行过程
 */
@Data
@RequiredArgsConstructor
public class StepExecutionRecord {

    /** 所属迭代轮次 */
    private final int iteration;
    /** 步骤ID */
    private final String stepId;
    /** 使用的工具名称 */
    private final ToolName toolName;
    /** 步骤执行指令 */
    private final String instruction;
    /** 步骤执行状态 */
    private final StepStatus status;
    /** 执行思考过程（LLM的思考内容） */
    private final String thought;
    /** 执行观察结果（工具返回的结果） */
    private final String observation;
    /** 原始执行结果对象 */
    private final Object result;
    /** 错误信息（执行失败时不为空） */
    private final String errorMessage;
}
