package org.example.ggbot.agenttask;

/**
 * 任务创建结果
 * @param task 任务记录
 * @param created 是否是新创建的任务，false表示已存在相同的外部事件ID
 */
public record AgentTaskCreationResult(
        AgentTaskRecord task,
        boolean created
) {
}
