package org.example.ggbot.agenttask;

import org.example.ggbot.agent.AgentRequest;

/**
 * Agent异步任务服务接口
 * 定义任务的创建、查询、状态变更等操作
 */
public interface AgentTaskService {

    /**
     * 创建新任务
     * @param request Agent请求
     * @param source 任务来源
     * @param externalEventId 外部事件ID（用于去重）
     * @return 创建后的任务记录
     */
    AgentTaskRecord createTask(AgentRequest request, String source, String externalEventId);

    /**
     * 根据外部事件ID创建或获取任务（幂等操作）
     * @param request Agent请求
     * @param source 任务来源
     * @param externalEventId 外部事件ID
     * @return 任务创建结果
     */
    AgentTaskCreationResult createOrGetByExternalEventId(AgentRequest request, String source, String externalEventId);

    /**
     * 根据任务ID查询任务
     * @param taskId 任务ID
     * @return 任务记录
     */
    AgentTaskRecord findByTaskId(String taskId);

    /**
     * 标记任务为运行中
     * @param taskId 任务ID
     * @return 更新后的任务记录
     */
    AgentTaskRecord markRunning(String taskId);

    /**
     * 标记任务为成功
     * @param taskId 任务ID
     * @param result 执行结果
     * @return 更新后的任务记录
     */
    AgentTaskRecord markSuccess(String taskId, String result);

    /**
     * 标记任务为失败
     * @param taskId 任务ID
     * @param errorMessage 错误信息
     * @return 更新后的任务记录
     */
    AgentTaskRecord markFailed(String taskId, String errorMessage);

    /**
     * 标记任务为已取消
     * @param taskId 任务ID
     * @param errorMessage 取消原因
     * @return 更新后的任务记录
     */
    AgentTaskRecord markCancelled(String taskId, String errorMessage);

    /**
     * 标记任务为重试中
     * @param taskId 任务ID
     * @param retryCount 重试次数
     * @param errorMessage 重试原因
     * @return 更新后的任务记录
     */
    AgentTaskRecord markRetrying(String taskId, int retryCount, String errorMessage);

    /**
     * 重试任务
     * @param taskId 任务ID
     * @return 更新后的任务记录
     */
    AgentTaskRecord retry(String taskId);
}
