package org.example.ggbot.agent.reflection;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * 反思分析结果对象
 * 封装对执行结果的评估结论，用于后续的路由和重规划决策
 */
@Data
@RequiredArgsConstructor
public class ReflectionAnalysis {

    /** 执行是否成功 */
    private final boolean success;
    /** 是否需要重试当前步骤 */
    private final boolean needRetry;
    /** 是否需要重新规划 */
    private final boolean needReplan;
    /** 整个任务是否完成 */
    private final boolean done;
    /** 反思结果类型 */
    private final ReflectionType reflectionType;
    /** 反思思考内容 */
    private final String thought;
    /** 观察到的执行结果 */
    private final String observation;
    /** 建议的下一步动作 */
    private final String recommendedAction;
}
