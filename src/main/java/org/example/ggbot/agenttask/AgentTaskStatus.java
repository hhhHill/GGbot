package org.example.ggbot.agenttask;

/**
 * Agent异步任务状态枚举
 */
public enum AgentTaskStatus {
    /** 等待执行 */
    PENDING,
    /** 正在执行 */
    RUNNING,
    /** 执行成功 */
    SUCCESS,
    /** 执行失败 */
    FAILED,
    /** 重试中 */
    RETRYING,
    /** 已取消 */
    CANCELLED
}
