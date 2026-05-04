package org.example.ggbot.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.agent.execution.ExecutionResult;
import org.example.ggbot.agent.execution.StepExecutionRecord;
import org.example.ggbot.agent.reflection.ReflectionAnalysis;
import org.example.ggbot.planner.Plan;
import org.example.ggbot.tool.ToolResult;

/**
 * Agent运行时状态容器，贯穿整个Agent执行生命周期
 * 所有工作流节点都会读写这个对象，是Agent状态流转的核心载体
 */
@Data
@RequiredArgsConstructor
public class AgentState {

    /** 任务ID，唯一标识本次Agent执行任务 */
    private final String taskId;
    /** 原始Agent请求对象 */
    private final AgentRequest request;
    /** 用户原始输入内容 */
    private final String userInput;
    /** 当前执行计划，由Planner生成，包含多个执行步骤 */
    private Plan currentPlan;
    /** 执行历史记录，保存所有步骤的执行详情 */
    private final List<StepExecutionRecord> executionHistory = new ArrayList<>();
    /** 中间结果映射，按步骤ID存储各步骤的执行结果 */
    private final Map<String, Object> intermediateResults = new HashMap<>();
    /** 记忆列表，包含会话历史和用户全局记忆 */
    private final List<String> memory = new ArrayList<>();
    /** 上下文数据，用于在节点间传递自定义数据 */
    private final Map<String, Object> context = new HashMap<>();
    /** 当前迭代轮次，每轮计划执行+1 */
    private int iteration;
    /** 是否执行完成，为true时工作流终止 */
    private boolean done;
    /** 最终回复内容，返回给用户的最终结果 */
    private String finalReply = "";

    /**
     * 初始化Agent状态
     * @param taskId 任务ID
     * @param request Agent请求对象
     * @param memory 初始记忆列表
     * @return 初始化后的Agent状态
     */
    public static AgentState initialize(String taskId, AgentRequest request, List<String> memory) {
        AgentState state = new AgentState(taskId, request, request.getUserInput());
        state.getMemory().addAll(memory);
        state.getContext().putAll(request.getMetadata());
        return state;
    }

    /**
     * 更新Agent状态，执行完每个步骤后调用
     * @param result 执行结果
     * @param reflection 反思分析结果
     * @return 更新后的Agent状态
     * 会自动追加执行记录、更新中间结果、拼接回复内容、更新完成状态
     */
    public AgentState update(ExecutionResult result, ReflectionAnalysis reflection) {
        this.executionHistory.addAll(result.getRecords());
        for (StepExecutionRecord record : result.getRecords()) {
            if (record.getResult() != null) {
                intermediateResults.put(record.getStepId(), record.getResult());
            }
            if (record.getObservation() != null && !record.getObservation().isBlank()) {
                appendReply(record.getObservation());
            } else if (record.getResult() instanceof ToolResult toolResult) {
                appendReply(toolResult.getSummary());
            }
        }
        this.done = reflection.isDone();
        context.put("lastReflection", reflection);
        context.put("lastExecutionSummary", result.getSummary());
        return this;
    }

    private void appendReply(String replyPart) {
        if (replyPart == null || replyPart.isBlank()) {
            return;
        }
        if (finalReply == null || finalReply.isBlank()) {
            finalReply = replyPart;
            return;
        }
        finalReply = finalReply + "\n\n" + replyPart;
    }
}
