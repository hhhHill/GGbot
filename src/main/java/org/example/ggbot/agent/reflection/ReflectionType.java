package org.example.ggbot.agent.reflection;

/**
 * 反思结果类型枚举
 * 表示执行结果的不同情况
 */
public enum ReflectionType {
    /** 执行成功 */
    SUCCESS,
    /** 工具执行失败 */
    TOOL_EXECUTION_FAILURE,
    /** 结果为空或质量过弱 */
    EMPTY_OR_WEAK_RESULT,
    /** 计划步骤已耗尽但任务未完成 */
    PLAN_EXHAUSTED_BUT_NOT_DONE
}
