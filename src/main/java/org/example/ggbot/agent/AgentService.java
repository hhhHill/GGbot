package org.example.ggbot.agent;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.memory.ConversationMemoryService;
import org.example.ggbot.service.dto.ConversationContext;
import org.example.ggbot.task.TaskRecord;
import org.example.ggbot.task.TaskService;
import org.example.ggbot.task.TaskStatus;
import org.springframework.stereotype.Service;

/**
 * Agent服务入口，对外提供Agent能力的统一门面
 * 负责协调任务创建、记忆管理、Agent执行、结果返回全流程
 */
@Service
@Data
@RequiredArgsConstructor
public class AgentService {

    /** Agent执行器，负责运行Agent工作流 */
    private final AgentRunner agentRunner;
    /** 会话记忆服务，管理对话历史 */
    private final ConversationMemoryService conversationMemoryService;
    /** 任务服务，管理Agent执行任务的生命周期 */
    private final TaskService taskService;

    /**
     * 处理Agent请求的主入口
     * @param request Agent请求，包含用户输入、会话信息等
     * @return Agent执行结果，包含任务ID、回复内容、执行历史等
     * 执行流程：
     * 1. 创建任务记录，标记为运行中
     * 2. 追加用户消息到会话记忆
     * 3. 初始化Agent状态，传入会话历史
     * 4. 运行Agent工作流
     * 5. 追加Agent回复到会话记忆
     * 6. 更新任务状态为完成，返回结果
     */
    public AgentResult handle(AgentRequest request) {
        TaskRecord taskRecord = taskService.createTask(request.getConversationId(), request.getUserInput());
        taskService.updateStatus(taskRecord.getTaskId(), TaskStatus.RUNNING, "Agent is processing request");

        try {
            conversationMemoryService.appendUserMessage(request.getConversationId(), request.getUserInput());
            AgentState initialState = AgentState.initialize(
                    taskRecord.getTaskId(),
                    request,
                    conversationMemoryService.getConversationHistory(request.getConversationId())
            );

            AgentState finalState = agentRunner.run(initialState);
            String replyText = finalState.getFinalReply();
            conversationMemoryService.appendAgentMessage(request.getConversationId(), replyText);
            taskService.updateStatus(taskRecord.getTaskId(), TaskStatus.COMPLETED, replyText);
            return new AgentResult(
                    taskRecord.getTaskId(),
                    finalState.getCurrentPlan() == null ? null : finalState.getCurrentPlan().getIntentType(),
                    replyText,
                    finalState.getExecutionHistory().stream().map(record -> record.getObservation()).toList()
            );
        } catch (RuntimeException ex) {
            taskService.updateStatus(taskRecord.getTaskId(), TaskStatus.FAILED, ex.getMessage());
            throw ex;
        }
    }

    /**
     * 使用持久化上下文处理对话请求（用于已落地的持久会话场景）
     * @param context 持久化会话上下文，包含组织ID、用户ID、会话历史、全局记忆等
     * @param userInput 用户当前输入
     * @return Agent回复内容
     * 执行流程：
     * 1. 构造Agent请求，将持久化上下文传入
     * 2. 创建任务记录，标记为运行中
     * 3. 合并会话历史和全局记忆作为Agent上下文
     * 4. 初始化Agent状态并运行工作流
     * 5. 更新任务状态为完成，返回回复内容
     */
    public String replyWithPersistentContext(ConversationContext context, String userInput) {
        AgentRequest request = new AgentRequest(
                String.valueOf(context.conversationId()),
                null,
                userInput,
                AgentChannel.WEB,
                null,
                String.valueOf(context.conversationId()),
                java.util.Map.of(
                        "orgId", context.orgId(),
                        "subjectId", context.subjectId(),
                        "conversationId", context.conversationId()
                ),
                context.history(),
                context.globalMemory()
        );
        TaskRecord taskRecord = taskService.createTask(request.getConversationId(), request.getUserInput());
        taskService.updateStatus(taskRecord.getTaskId(), TaskStatus.RUNNING, "Agent is processing persistent context request");
        try {
            List<String> stateMemory = new ArrayList<>(request.getHistory());
            request.getMemory().forEach(memoryEntry -> stateMemory.add("MEMORY: " + memoryEntry));
            AgentState initialState = AgentState.initialize(taskRecord.getTaskId(), request, stateMemory);
            AgentState finalState = agentRunner.run(initialState);
            String replyText = finalState.getFinalReply();
            taskService.updateStatus(taskRecord.getTaskId(), TaskStatus.COMPLETED, replyText);
            return replyText;
        } catch (RuntimeException ex) {
            taskService.updateStatus(taskRecord.getTaskId(), TaskStatus.FAILED, ex.getMessage());
            throw ex;
        }
    }
}
